#!/usr/bin/env bash
#
# run-demo.sh — CodePerf 端到端演示（静态扫描 + 动态 attach + 交叉验证报告）
# =============================================================================
# 固化自 docs/03-cli.md §8 与 docs/05-static-analysis.md §8/§9 的命令流。
#
# 流程：
#   0. 前置检查：cli / agent / demo 三个产物 jar 是否已构建
#   1. scan   —— 对 demo 编译产物做静态扫描，产出 static.json + static.html
#   2. 启动 demo（JDK8），轮询就绪
#   3. attach —— 把 agent 挂入 demo JVM，后台等待 .done；随后 curl 触发目标接口
#   4. report —— 合并静态 + 动态，按 --fail-on 门禁返回退出码
#
# 关键前提（来自 docs/03-cli.md §3）：
#   * attach 走 JDK8 的 tools.jar Attach API —— CLI 与 demo 目标 JVM 都必须是 JDK8。
#   * 产物 jar 需先用 IDE 或 `mvn package` 构建（本仓库无 mvnw）。
#   * Git Bash(MINGW64) 下 $! 是 MSYS 内部 PID，与 Attach API 需要的真实 Windows PID 不同；
#     脚本自启 demo 时用 jps -l 解析 Windows PID 供 attach，$! 仅留作 kill 清理。
#     外部 DEMO_PID 视为已是 Windows PID（手动 jps/netstat 拿到的）。
#   * attach 握手走 Windows 命名管道，在 MSYS 子进程里跑会被 pty 污染（Non-numeric value
#     found - int expected）；故 attach 这一步经 cmd.exe 在原生 Windows 进程执行。
#
# 用法：
#   bash scripts/run-demo.sh                       # 自动起 demo、跑全流程
#   DEMO_PID=12345 bash scripts/run-demo.sh        # attach 到已在运行的 demo（跳过启动）
#   JDK8=/d/Java8/jdk1.8.0_341 bash scripts/run-demo.sh
# =============================================================================

set -u

# ---------- 可配置项（环境变量可覆盖） ----------
JDK8="${JDK8:-/d/Java8/jdk1.8.0_341}"
TARGET_PKG="${TARGET_PKG:-com.codeperf.demo}"
ENTRY="${ENTRY:-POST /api/orders/report}"
PORT="${PORT:-8080}"
FAIL_ON="${FAIL_ON:-warn}"

# 输出文件
RAW="${RAW:-perf-data.raw}"
STATIC_JSON="${STATIC_JSON:-perf-static.json}"
STATIC_HTML="${STATIC_HTML:-perf-static.html}"
REPORT_HTML="${REPORT_HTML:-perf-report.html}"

# 产物 jar 路径
CLI_JAR="codeperf-cli/target/codeperf-cli.jar"
AGENT_JAR="codeperf-agent/target/codeperf-agent.jar"

# 切到仓库根目录（脚本位于 scripts/ 下）
cd "$(dirname "$0")/.." || exit 2

JAVA8="$JDK8/bin/java"

log() { echo "[run-demo] $*"; }
die() { echo "[run-demo][ERROR] $*" >&2; exit "${2:-2}"; }

# ---------- 0. 前置检查 ----------
[ -x "$JAVA8" ] || die "找不到 JDK8: $JAVA8（用 JDK8=/path bash scripts/run-demo.sh 覆盖）"
[ -f "$CLI_JAR" ]   || die "缺少 $CLI_JAR，请先构建（IDE 或 mvn package）"
[ -f "$AGENT_JAR" ] || die "缺少 $AGENT_JAR，请先构建（IDE 或 mvn package）"

# demo 可启动 jar（排除 sources / original）
DEMO_JAR="${DEMO_JAR:-}"
if [ -z "$DEMO_JAR" ]; then
  DEMO_JAR="$(ls codeperf-demo/target/*.jar 2>/dev/null \
    | grep -vE -- '-sources\.jar$|\.original$' | head -1)"
fi

# ---------- 1. 静态扫描 ----------
log "step 1/4 静态扫描 ($TARGET_PKG)"
"$JAVA8" -jar "$CLI_JAR" scan \
  --target-package "$TARGET_PKG" \
  --classes-dir codeperf-demo/target/classes \
  --output "$STATIC_JSON" \
  --report "$STATIC_HTML" || die "scan 失败"

# ---------- 2. 启动 demo（或复用已运行实例） ----------
STARTED_DEMO=0
DEMO_PID="${DEMO_PID:-}"        # 用于 kill 清理：脚本自启时是 MSYS PID，外部传入时是 Windows PID
ATTACH_PID_TARGET=""           # 用于 attach --pid：必须是真实 Windows PID（见下方说明）
if [ -z "$DEMO_PID" ]; then
  [ -n "$DEMO_JAR" ] || die "未找到 demo jar，请构建或用 DEMO_JAR=... 指定"
  log "step 2/4 启动 demo: $DEMO_JAR"
  "$JAVA8" -jar "$DEMO_JAR" > demo.log 2>&1 &
  DEMO_PID=$!                   # MSYS/MINGW 内部 PID —— 仅 kill 用得到，Attach API 不认
  STARTED_DEMO=1
  log "demo 启动中（MSYS pid=$DEMO_PID，日志见 demo.log）..."
  ready=0
  for _ in $(seq 1 60); do
    if curl -s -o /dev/null "http://localhost:$PORT/api/orders/report"; then
      ready=1; break
    fi
    sleep 1
  done
  [ "$ready" = 1 ] || { [ "$STARTED_DEMO" = 1 ] && kill "$DEMO_PID" 2>/dev/null; die "demo 未就绪超时"; }

  # 关键：Git Bash 下 $! 是 MSYS PID，与 JDK8 Attach API 需要的 Windows PID 不同。
  # 用 jps -l 解析真实 Windows PID（jps 报告的就是 Windows PID）。
  BASE_JAR="$(basename "$DEMO_JAR")"
  for _ in $(seq 1 10); do
    ATTACH_PID_TARGET="$("$JDK8/bin/jps" -l 2>/dev/null | grep -F "$BASE_JAR" | awk '{print $1}' | head -1)"
    [ -n "$ATTACH_PID_TARGET" ] && break
    sleep 1
  done
  [ -n "$ATTACH_PID_TARGET" ] || die "无法用 jps 解析 demo 的 Windows PID（$BASE_JAR 未出现在 jps -l）"
  log "demo 就绪：Windows pid=$ATTACH_PID_TARGET（attach 用此 pid），MSYS pid=$DEMO_PID（kill 用此 pid）"
else
  # 外部传入的 DEMO_PID 视为已是真实 Windows PID（手动 jps/netstat 拿到的）。
  ATTACH_PID_TARGET="$DEMO_PID"
  log "step 2/4 复用已运行 demo pid=$DEMO_PID（跳过启动，attach 直接用此 pid）"
fi

cleanup() {
  rm -f .codeperf-attach.bat 2>/dev/null
  if [ "$STARTED_DEMO" = 1 ] && [ -n "$DEMO_PID" ]; then
    log "清理：停止 demo pid=$DEMO_PID"
    kill "$DEMO_PID" 2>/dev/null
  fi
}
trap cleanup EXIT

# ---------- 3. attach + 触发请求 ----------
# 关键：JDK8 的 attach 握手走 Windows 命名管道，若 CLI 跑在 Git Bash(MSYS) 子进程里，
# pty 环境会污染握手回包 → "Non-numeric value found - int expected"。
# 故把 attach 这一步交给 cmd.exe 在原生 Windows 进程里执行（其余步骤不涉握手，留在 bash）。
log "step 3/4 attach 到 pid=$ATTACH_PID_TARGET，并触发 $ENTRY"

# java.exe / jar / 仓库根都转成 Windows 绝对路径（cmd 不认 /d/... 这类 MSYS 路径）。
JAVA8_WIN="$(cygpath -w "$JDK8/bin/java.exe" 2>/dev/null)"
[ -n "$JAVA8_WIN" ] || JAVA8_WIN="$JDK8/bin/java.exe"
CLI_JAR_WIN="$(cygpath -w "$PWD/$CLI_JAR" 2>/dev/null || echo "$CLI_JAR")"
REPO_WIN="$(cygpath -w "$PWD" 2>/dev/null || echo "$PWD")"
RAW_WIN="$(cygpath -w "$PWD/$RAW" 2>/dev/null || echo "$RAW")"

# 生成临时 bat（必须 CRLF 行尾，否则 cmd.exe 解析出错报"系统找不到指定的路径"）。
ATTACH_BAT=".codeperf-attach.bat"
printf '@echo off\r\n' > "$ATTACH_BAT"
printf 'cd /d "%s"\r\n' "$REPO_WIN" >> "$ATTACH_BAT"
printf '"%s" -jar "%s" attach --pid %s --target-package "%s" --entry "%s" --output "%s"\r\n' \
  "$JAVA8_WIN" "$CLI_JAR_WIN" "$ATTACH_PID_TARGET" "$TARGET_PKG" "$ENTRY" "$RAW_WIN" >> "$ATTACH_BAT"
ATTACH_BAT_WIN="$(cygpath -w "$PWD/$ATTACH_BAT" 2>/dev/null || echo "$ATTACH_BAT")"

# 后台跑 cmd.exe；MSYS_NO_PATHCONV 防止路径被 MSYS 二次转换。cmd /c 的退出码=java 退出码。
MSYS_NO_PATHCONV=1 cmd.exe /c "$ATTACH_BAT_WIN" &
ATTACH_PID=$!

sleep 5  # 给 agent 安装插桩（retransform）留出时间

# 用 ENTRY 的 METHOD/PATH 触发一次请求
ENTRY_METHOD="${ENTRY%% *}"
ENTRY_PATH="${ENTRY#* }"
log "触发: curl -X $ENTRY_METHOD http://localhost:$PORT$ENTRY_PATH"
curl -s -o /dev/null -X "$ENTRY_METHOD" "http://localhost:$PORT$ENTRY_PATH" \
  || log "（curl 触发返回非零，attach 仍会等待 .done 直至超时）"

wait "$ATTACH_PID"
ATTACH_RC=$?
[ "$ATTACH_RC" = 0 ] || die "attach 采集未完成 (rc=$ATTACH_RC)" "$ATTACH_RC"

# ---------- 4. 合并报告 + 门禁 ----------
log "step 4/4 合并静态+动态，生成报告（--fail-on $FAIL_ON）"
"$JAVA8" -jar "$CLI_JAR" report \
  --input "$RAW" \
  --static "$STATIC_JSON" \
  --output "$REPORT_HTML" \
  --fail-on "$FAIL_ON"
REPORT_RC=$?

log "完成。产物：$STATIC_HTML / $REPORT_HTML（report 退出码=$REPORT_RC，门禁达标则非零）"
exit "$REPORT_RC"
