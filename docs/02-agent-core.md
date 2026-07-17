# 02 · Agent 采集核心设计

`codeperf-agent` 是预发动态证据采集器。官方启动方式仅支持 `-javaagent`，不支持运行时 attach。Agent 负责打桩、采集、上报，不做最终报告合并和门禁判断。

## 1. 启动方式

```bash
java -javaagent:codeperf-agent.jar=config/agent.yml -jar app.jar
```

Manifest 只声明：

```text
Premain-Class: com.codeperf.agent.AgentEntry
Can-Redefine-Classes: true
Can-Retransform-Classes: true
```

## 2. agent.yml

```yaml
serverUrl: http://codeperf-server:9090
uploadEnabled: true
appName: order-service
env: dev
buildInfoPath: /opt/codeperf/build-info.properties
targetPackages:
  - com.company.order
entry:
  method: POST
  path: /api/orders/report
slowSqlMs: 500
sampleMs: 10
mode: session
output: /opt/codeperf/perf-data.raw
```

CI/CD 的安装脚本先调用 `/api/agent/install-config` 获取 agent 下载地址、目标包名、入口路径等安装参数，再生成 `agent.yml` 和 `build-info.properties`。`build-info.properties` 记录 `remoteUrl + commit + branch + env`，Server 使用这组稳定身份把动态证据关联到 CLI 静态扫描创建的同一个分析任务。

## 3. 采集边界

- 方法插桩：只对 `targetPackages` 下的业务代码采集调用树。
- JDBC 插桩：采集 SQL 指纹、次数、耗时，用于辅助识别循环内 DB 放大证据。
- 栈采样：只采活跃请求线程，避免全量线程噪声。
- 请求入口：通过 Spring MVC 分发点识别 `entry.method + entry.path`。

## 4. 上报模型

会话结束后，Agent 会：

1. 将 `SessionData` 写入本地 `output` 文件，便于预发排查。
2. 当 `uploadEnabled=true` 且配置了旧版 `analysisTaskId` 时，将同一份 JSON POST 到 `/api/tasks/{analysisTaskId}/dynamic-evidence`。
3. 当没有 `analysisTaskId` 时，Agent 会读取 `build-info.properties`，将动态证据 POST 到 `/api/dynamic-evidence`，由 Server 按 `remoteUrl + commit + branch + env` 关联任务。

动态证据只说明“预发环境运行时观察到的调用与耗时”，不能表述为生产实测。
