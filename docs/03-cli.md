# 03 · CLI 设计

> 关联：`00-overview-architecture.md`、`02-agent-core.md`。本文件描述 `codeperf-cli` 模块。
> 编码若与本文档不一致，须及时回写（项目铁律）。

## 1. 职责

CLI 是开发者直接使用的命令行入口，提供两个子命令：
- `attach`：动态 attach 到运行中的目标 JVM(pid)，把 `codeperf-agent.jar` 挂进去并传入采集参数；随后等待 agent 采集完成（轮询 `.done` 标记）。
- `report`：读取 agent 产出的原始数据 JSON，调用分析引擎，生成 HTML 报告，并按最高严重度返回退出码（门禁能力）。

> 分析引擎与报告生成的实现见 `04-analysis-report.md`（task #6）。CLI 仅负责命令解析、attach 编排、调用分析门面。

## 2. 命令与参数

参数解析用 JCommander。

### 2.1 `attach`
| 参数 | 必填 | 默认 | 说明 |
|---|---|---|---|
| `--pid` | 是 | — | 目标 JVM 进程 id |
| `--target-package` | 是 | — | 插桩的应用包前缀（逗号分隔多个） |
| `--entry` | 是 | — | 入口：`METHOD PATH`，如 `POST /api/orders/report` |
| `--agent` | 否 | 自动探测 | `codeperf-agent.jar` 路径 |
| `--output` | 否 | `perf-data.raw` | 原始数据输出文件 |
| `--slow-sql-ms` | 否 | `500` | 慢 SQL 阈值 |
| `--sample-ms` | ��� | `10` | 栈采样周期 |
| `--mode` | 否 | `session` | `session`(采首个匹配请求即停) / `duration` |
| `--wait` | 否 | `120` | 等待采集完成的秒数（轮询 `.done`） |
| `--report` | 否 | — | 若指定，采集完成后自动生成该 HTML 报告 |

流程：
1. 解析参数 → 拼装 agent 参数串 `targetPackage=...;entry=...;slowSqlMs=...;output=<abs>;sampleMs=...;mode=...`。
2. 删除可能残留的旧 `${output}` 与 `${output}.done`。
3. 通过 Attach API 把 agent 挂到 pid（见第 3 节）。
4. 提示用户「现在去手动发请求打目标接口」。
5. 轮询 `${output}.done`：出现即视为采集完成；超过 `--wait` 秒未出现则超时退出（非零码）。
6. 若设置 `--report`，调用分析门面生成报告。

### 2.2 `report`
| 参数 | 必填 | 默认 | 说明 |
|---|---|---|---|
| `--input` | 否 | `perf-data.raw` | agent 产出的原始数据 JSON |
| `--output` | 否 | `perf-report.html` | 生成的 HTML 报告 |
| `--fail-on` | 否 | `none` | 门禁阈值：`none/info/warn/critical`；达到该级别则进程返回非零退出码 |

## 3. Attach 实现（JDK8 tools.jar）

JDK8 的 Attach API 位于 `$JAVA_HOME/lib/tools.jar`，默认不在 classpath。为免去用户手动配置，CLI 在运行时：
1. 定位 tools.jar：依次尝试 `System.getProperty("java.home")` 的 `lib/tools.jar`、`../lib/tools.jar`，及环境变量 `JAVA_HOME` 下同样位置。
2. 用 `URLClassLoader` 加载 tools.jar，**反射**调用
   `com.sun.tools.attach.VirtualMachine.attach(pid)` → `loadAgent(agentJarPath, agentArgs)` → `detach()`。

> 反射 + 自定位，避免用户配 classpath。前提：**CLI 必须用 JDK（非 JRE）运行**，本机为 `D:\Java8\jdk1.8.0_341`。
> JDK9+ 该 API 已并入平台模块（无 tools.jar），属 CI 期事项；届时直接用平台 API，加一个分支即可（不影响现有结构）。

## 4. Agent jar 自动探测顺序

`--agent` 未指定时，依次尝试：
1. 与 cli jar 同目录的 `codeperf-agent.jar`；
2. 相对工作目录 `codeperf-agent/target/codeperf-agent.jar`（开发期布局）；
3. 找不到则报错要求显式 `--agent`。

## 5. 控制通道：文件轮询

与 agent 约定（见 02 §7）：agent 采到目标请求后写 `${output}` 并写 `${output}.done`。CLI `attach` 轮询 `.done` 出现。选文件通道：实现最简、跨平台、无端口冲突。

## 6. 退出码（门禁）

- `attach`：attach 失败或等待超时 → 非零；正常采集完成 → 0。
- `report`：分析后取最高严重度，与 `--fail-on` 比较；达到/超过阈值 → 非零（用于 CI 卡流水线）。严重度序：`info(1) < warn(2) < critical(3)`，`none(0)` 表示不卡。

## 7. 模块结构

```
codeperf-cli
├── 依赖: jcommander, jackson-databind；运行时反射加载 tools.jar
├── 产物: codeperf-cli.jar（含依赖，main=com.codeperf.cli.Main）
└── 包结构:
    com.codeperf.cli
    ├── Main                 JCommander 装配两个子命令、分发、设置退出码
    ├── cmd/AttachCommand    attach 参数 + 编排逻辑
    ├── cmd/ReportCommand    report 参数 + 调用分析门面
    └── attach/AttachHelper  反射定位并调用 tools.jar Attach API
（分析/报告代码在 com.codeperf.analysis，见 04 文档）
```

## 8. 使用示例

```bash
JDK8=/d/Java8/jdk1.8.0_341
# attach 并采集，采完自动出报告
"$JDK8/bin/java" -jar codeperf-cli.jar attach \
  --pid 12345 --target-package com.codeperf.demo \
  --entry "POST /api/orders/report" \
  --output ./perf-data.raw --report ./perf-report.html
# 然后手动: curl -X POST http://localhost:8080/api/orders/report

# 或仅就已有数据生成报告并作门禁
"$JDK8/bin/java" -jar codeperf-cli.jar report \
  --input ./perf-data.raw --output ./perf-report.html --fail-on warn
```

> 端到端一键脚本：`scripts/run-demo.sh`（前置检查 → scan → 起 demo → attach+curl → report 门禁）。
> 用 `DEMO_PID=<pid>` 可 attach 到已运行实例，`JDK8=<path>` 覆盖 JDK8 位置。
