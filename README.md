# CodePerf

CodePerf 是面向 Git/CI 流程的循环 I/O 放大风险检测工具。第一阶段聚焦一类线上事故：代码在循环中调用 DB、HTTP、RPC 或 SDK，测试环境数据量小未暴露，生产规模放大后接口变慢。

## 当前职责

- `codeperf-cli`：在 pre-push/CI 中做静态字节码扫描、创建分析任务、上传静态结果、查询门禁。
- `codeperf-agent`：在预发应用启动时通过 `-javaagent` 加载，按 `agent.yml` 打桩采集运行证据并上报。
- `codeperf-server`：接收静态结果和动态证据，合并报告并输出 gate 结论。

## 基本流程

```bash
mvn package

# 1. 启动服务端
java -jar codeperf-server/target/codeperf-server.jar

# 2. CI 创建分析任务
TASK_ID=$(java -jar codeperf-cli/target/codeperf-cli.jar task \
  --server http://127.0.0.1:9090 \
  --project order-service \
  --commit abc123 \
  --branch feature/loop-io \
  --env preprod)

# 3. 静态扫描并上传
java -jar codeperf-cli/target/codeperf-cli.jar scan-diff \
  --base origin/main \
  --head HEAD \
  --target-package com.codeperf.demo \
  --classes-dir codeperf-demo/target/classes \
  --source-root codeperf-demo/src/main/java \
  --output perf-static.json \
  --server http://127.0.0.1:9090 \
  --task-id "$TASK_ID" \
  --upload

# 4. 预发应用用 -javaagent 启动，agent.yml 中写入同一个 TASK_ID
java -javaagent:codeperf-agent/target/codeperf-agent.jar=config/agent.yml \
  -jar codeperf-demo/target/codeperf-demo.jar

# 5. CI 查询门禁
java -jar codeperf-cli/target/codeperf-cli.jar gate \
  --server http://127.0.0.1:9090 \
  --task-id "$TASK_ID" \
  --fail-on WARN
```

## agent.yml 示例

```yaml
serverUrl: http://127.0.0.1:9090
analysisTaskId: ${ANALYSIS_TASK_ID}
uploadEnabled: true
targetPackages:
  - com.codeperf.demo
entry:
  method: POST
  path: /api/orders/report
slowSqlMs: 500
sampleMs: 10
mode: session
output: build/codeperf/perf-data.raw
```

正式方案不使用运行时 attach；动态检测只作为预发证据增强，不代表生产实测。
