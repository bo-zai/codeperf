#!/usr/bin/env bash
set -euo pipefail

CODEPERF_DIR="target/codeperf"
AGENT_FILE="${CODEPERF_DIR}/codeperf-agent.jar"
AGENT_CONFIG_FILE="${CODEPERF_DIR}/agent.yml"
BUILD_INFO_FILE="${CODEPERF_DIR}/build-info.properties"
INJECT_START="# CODEPERF_AGENT_START"
INJECT_END="# CODEPERF_AGENT_END"
IMAGE_AGENT_DIR="/opt/codeperf"

REMOTE_URL=""
COMMIT_SHA=""
BRANCH_NAME=""
AUTHOR_NAME=""
AUTHOR_EMAIL=""
COMMIT_TIME=""
COMMIT_MESSAGE=""
PROJECT_NAME=""

INSTALL_ENABLED=""
SERVER_URL=""
AGENT_URL=""
AGENT_SHA256=""
APP_NAME=""
ENV_NAME=""
TARGET_PACKAGES=""
ENTRY_METHOD=""
ENTRY_PATH=""
SLOW_SQL_MS=""
SAMPLE_MS=""
MODE=""

log() {
  echo "[codeperf-install] $*"
}

fail() {
  echo "[codeperf-install] 错误: $*" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "缺少必要命令: $1"
}

resolve_dockerfile() {
  if [ -n "${CODEPERF_DOCKERFILE:-}" ]; then
    [ -f "$CODEPERF_DOCKERFILE" ] || fail "指定的 Dockerfile 不存在: ${CODEPERF_DOCKERFILE}"
    printf '%s\n' "$CODEPERF_DOCKERFILE"
    return
  fi
  if [ -f "Dockerfile" ]; then
    printf '%s\n' "Dockerfile"
    return
  fi
  fail "未找到 Dockerfile，请在仓库根目录执行脚本，或设置 CODEPERF_DOCKERFILE"
}

git_value() {
  local fallback="$1"
  shift
  local value
  value="$(git "$@" 2>/dev/null || true)"
  value="$(printf '%s' "$value" | head -n 1 | tr -d '\r')"
  if [ -z "$value" ]; then
    printf '%s\n' "$fallback"
  else
    printf '%s\n' "$value"
  fi
}

required_git_value() {
  local name="$1"
  shift
  local value
  value="$(git_value "" "$@")"
  if [ -z "$value" ] || [ "$value" = "UNKNOWN" ]; then
    fail "无法获取 Git 信息: ${name}"
  fi
  printf '%s\n' "$value"
}

collect_git_identity() {
  git rev-parse --is-inside-work-tree >/dev/null 2>&1 || fail "当前目录不是 Git 仓库，无法生成构建身份信息"
  REMOTE_URL="${CODEPERF_REMOTE_URL:-$(required_git_value "remoteUrl" config --get remote.origin.url)}"
  COMMIT_SHA="${CODEPERF_COMMIT_SHA:-$(required_git_value "commit" rev-parse HEAD)}"
  BRANCH_NAME="${CODEPERF_BRANCH:-$(required_git_value "branch" rev-parse --abbrev-ref HEAD)}"
  AUTHOR_NAME="${CODEPERF_AUTHOR_NAME:-$(required_git_value "authorName" log -1 --format=%an)}"
  AUTHOR_EMAIL="${CODEPERF_AUTHOR_EMAIL:-$(required_git_value "authorEmail" log -1 --format=%ae)}"
  COMMIT_TIME="${CODEPERF_COMMIT_TIME:-$(required_git_value "commitTime" log -1 --format=%aI)}"
  COMMIT_MESSAGE="${CODEPERF_COMMIT_MESSAGE:-$(required_git_value "commitMessage" log -1 --format=%s)}"
  PROJECT_NAME="${CODEPERF_PROJECT:-$(derive_project_name "$REMOTE_URL")}"
}

derive_project_name() {
  local remote_url="$1"
  local normalized
  normalized="$(printf '%s' "$remote_url" | tr ':' '/' | tr '\\' '/')"
  normalized="${normalized%.git}"
  normalized="${normalized%/}"
  printf '%s' "${normalized##*/}"
}

post_json() {
  local url="$1"
  local body="$2"
  local response_file
  response_file="$(mktemp)"
  local http_code
  http_code="$(curl -sS -o "$response_file" -w '%{http_code}' -X POST \
    -H 'Content-Type: application/json' \
    ${CODEPERF_ACCESS_TOKEN:+-H "Authorization: Bearer ${CODEPERF_ACCESS_TOKEN}"} \
    --data "$body" \
    "$url" || true)"
  if [ "${http_code:-000}" -lt 200 ] || [ "${http_code:-000}" -ge 300 ]; then
    local response
    response="$(cat "$response_file")"
    rm -f "$response_file"
    fail "配置接口调用失败，status=${http_code}, body=${response}"
  fi
  cat "$response_file"
  rm -f "$response_file"
}

fetch_install_config() {
  CODEPERF_INSTALL_CONFIG_URL="${CODEPERF_INSTALL_CONFIG_URL:-__CODEPERF_INSTALL_CONFIG_URL__}"
  [ -n "${CODEPERF_ENV:-}" ] || ENV_NAME="dev"
  ENV_NAME="${CODEPERF_ENV:-dev}"

  local request
  request="$(python3 - "$REMOTE_URL" "$COMMIT_SHA" "$BRANCH_NAME" "$PROJECT_NAME" "${CODEPERF_ENV:-dev}" \
    "$AUTHOR_NAME" "$AUTHOR_EMAIL" "$COMMIT_TIME" "$COMMIT_MESSAGE" <<'PY'
import json
import sys
payload = {
    "remoteUrl": sys.argv[1],
    "commit": sys.argv[2],
    "branch": sys.argv[3],
    "project": sys.argv[4],
    "env": sys.argv[5],
    "authorName": sys.argv[6],
    "authorEmail": sys.argv[7],
    "commitTime": sys.argv[8],
    "commitMessage": sys.argv[9]
}
# Windows Git Bash 下 Python 标准输出可能使用 GBK；转义非 ASCII 字符可避免中文提交信息导致服务端按 UTF-8 解析失败。
print(json.dumps(payload, ensure_ascii=True, separators=(",", ":")))
PY
)"

  local response
  response="$(post_json "$CODEPERF_INSTALL_CONFIG_URL" "$request")"

  INSTALL_ENABLED="$(python3 - <<'PY' "$response"
import json
import sys
data = json.loads(sys.argv[1])
print("true" if data.get("enabled", True) else "false")
PY
)"
  if [ "$INSTALL_ENABLED" = "false" ]; then
    log "服务端配置已禁用，跳过 CodePerf Agent 接入"
    exit 0
  fi

  SERVER_URL="$(python3 - <<'PY' "$response"
import json
import sys
data = json.loads(sys.argv[1])
print(data.get("serverUrl", ""))
PY
)"
  AGENT_URL="$(python3 - <<'PY' "$response"
import json
import sys
data = json.loads(sys.argv[1])
print(data.get("agentUrl", ""))
PY
)"
  AGENT_SHA256="$(python3 - <<'PY' "$response"
import json
import sys
data = json.loads(sys.argv[1])
print(data.get("agentSha256", ""))
PY
)"
  APP_NAME="$(python3 - <<'PY' "$response"
import json
import sys
data = json.loads(sys.argv[1])
print(data.get("appName", ""))
PY
)"
  ENV_NAME="$(python3 - <<'PY' "$response"
import json
import sys
data = json.loads(sys.argv[1])
print(data.get("env", "dev"))
PY
)"
  TARGET_PACKAGES="$(python3 - <<'PY' "$response"
import json
import sys
data = json.loads(sys.argv[1])
print(",".join(data.get("targetPackages", [])))
PY
)"
  ENTRY_METHOD="$(python3 - <<'PY' "$response"
import json
import sys
data = json.loads(sys.argv[1])
entry = data.get("entry", {})
print(entry.get("method", "POST"))
PY
)"
  ENTRY_PATH="$(python3 - <<'PY' "$response"
import json
import sys
data = json.loads(sys.argv[1])
entry = data.get("entry", {})
print(entry.get("path", "/"))
PY
)"
  SLOW_SQL_MS="$(python3 - <<'PY' "$response"
import json
import sys
data = json.loads(sys.argv[1])
print(data.get("slowSqlMs", 500))
PY
)"
  SAMPLE_MS="$(python3 - <<'PY' "$response"
import json
import sys
data = json.loads(sys.argv[1])
print(data.get("sampleMs", 10))
PY
)"
  MODE="$(python3 - <<'PY' "$response"
import json
import sys
data = json.loads(sys.argv[1])
print(data.get("mode", "session"))
PY
)"

  [ -n "$SERVER_URL" ] || fail "配置接口缺少 serverUrl"
  [ -n "$AGENT_URL" ] || fail "配置接口缺少 agentUrl"
  [ -n "$APP_NAME" ] || fail "配置接口缺少 appName"
  [ -n "$TARGET_PACKAGES" ] || fail "配置接口缺少 targetPackages"
}

property_value() {
  printf '%s' "$1" | tr '\r\n' '  '
}

yaml_scalar() {
  printf '%s' "$1" | sed "s/'/''/g"
}

download_agent() {
  mkdir -p "$CODEPERF_DIR"
  local tmp_file="${AGENT_FILE}.tmp"
  log "下载 codeperf-agent.jar: ${AGENT_URL}"
  curl -fSL --retry 2 --connect-timeout 10 --max-time 120 -o "$tmp_file" "$AGENT_URL" \
    || fail "下载 codeperf-agent.jar 失败"
  [ -s "$tmp_file" ] || fail "下载的 codeperf-agent.jar 为空"
  if [ -n "$AGENT_SHA256" ]; then
    local actual expected
    actual="$(sha256sum "$tmp_file" | awk '{print tolower($1)}')"
    expected="$(printf '%s' "$AGENT_SHA256" | tr '[:upper:]' '[:lower:]')"
    if [ "$actual" != "$expected" ]; then
      rm -f "$tmp_file"
      fail "SHA256 校验失败，expected=${expected}, actual=${actual}"
    fi
  fi
  mv "$tmp_file" "$AGENT_FILE"
}

write_agent_config() {
  local target_packages_yaml=""
  local old_ifs="$IFS"
  IFS=','
  for pkg in $TARGET_PACKAGES; do
    pkg="$(printf '%s' "$pkg" | xargs)"
    if [ -n "$pkg" ]; then
      target_packages_yaml="${target_packages_yaml}  - ${pkg}
"
    fi
  done
  IFS="$old_ifs"
  [ -n "$target_packages_yaml" ] || fail "配置接口返回的 targetPackages 不能为空"

  cat > "$AGENT_CONFIG_FILE" <<EOF
serverUrl: $(yaml_scalar "$SERVER_URL")
appName: $(yaml_scalar "$APP_NAME")
env: $(yaml_scalar "$ENV_NAME")
uploadEnabled: true
buildInfoPath: ${IMAGE_AGENT_DIR}/build-info.properties
targetPackages:
${target_packages_yaml}entry:
  method: $(yaml_scalar "$ENTRY_METHOD")
  path: $(yaml_scalar "$ENTRY_PATH")
slowSqlMs: ${SLOW_SQL_MS}
sampleMs: ${SAMPLE_MS}
mode: $(yaml_scalar "$MODE")
output: ${IMAGE_AGENT_DIR}/perf-data.raw
EOF
}

write_build_info() {
  cat > "$BUILD_INFO_FILE" <<EOF
remoteUrl=$(property_value "$REMOTE_URL")
commit=$(property_value "$COMMIT_SHA")
branch=$(property_value "$BRANCH_NAME")
env=$(property_value "$ENV_NAME")
project=$(property_value "${CODEPERF_PROJECT:-$APP_NAME}")
appName=$(property_value "$APP_NAME")
authorName=$(property_value "$AUTHOR_NAME")
authorEmail=$(property_value "$AUTHOR_EMAIL")
commitTime=$(property_value "$COMMIT_TIME")
commitMessage=$(property_value "$COMMIT_MESSAGE")
EOF
}

inject_dockerfile() {
  local dockerfile="$1"
  local backup="${dockerfile}.codeperf.bak"
  cp "$dockerfile" "$backup"

  local tmp_file
  tmp_file="$(mktemp)"
  awk -v start="$INJECT_START" -v end="$INJECT_END" '
    $0 == start { skipping = 1; next }
    $0 == end { skipping = 0; next }
    skipping != 1 { print }
  ' "$dockerfile" > "$tmp_file"

  cat >> "$tmp_file" <<EOF

${INJECT_START}
COPY target/codeperf/ ${IMAGE_AGENT_DIR}/
ENV JAVA_TOOL_OPTIONS="\${JAVA_TOOL_OPTIONS} -javaagent:${IMAGE_AGENT_DIR}/codeperf-agent.jar=${IMAGE_AGENT_DIR}/agent.yml"
${INJECT_END}
EOF

  mv "$tmp_file" "$dockerfile"
  grep -q "$INJECT_START" "$dockerfile" || fail "Dockerfile 注入失败: 缺少开始标记"
  grep -q -- "-javaagent:${IMAGE_AGENT_DIR}/codeperf-agent.jar=${IMAGE_AGENT_DIR}/agent.yml" "$dockerfile" \
    || fail "Dockerfile 注入失败: 缺少 javaagent 参数"
}

main() {
  require_command curl
  require_command git
  require_command awk
  require_command sed
  require_command sha256sum
  require_command python3

  local dockerfile
  dockerfile="$(resolve_dockerfile)"
  log "使用 Dockerfile: ${dockerfile}"

  collect_git_identity
  fetch_install_config
  download_agent
  write_agent_config
  write_build_info
  inject_dockerfile "$dockerfile"

  log "已生成 ${AGENT_FILE}"
  log "已生成 ${AGENT_CONFIG_FILE}"
  log "已生成 ${BUILD_INFO_FILE}"
  log "已注入 Dockerfile，后续 docker build 会把 CodePerf Agent 打入镜像"
}

main "$@"
