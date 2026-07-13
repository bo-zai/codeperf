# 03 · CLI 设计

`codeperf-cli` 是 Git/CI 客户端，不负责启动业务应用，也不控制目标 JVM。官方流程中 CLI 只做三件事：创建任务、执行静态扫描并上传、查询服务端 gate。

## 1. 命令

| 命令 | 职责 |
|---|---|
| `task` | 向 CodePerf Server 创建 `analysis_task_id` |
| `scan` | 扫描本地 class 文件，输出静态结果，可上传 |
| `scan-diff` | 面向 Git diff 的静态扫描入口，可上传 |
| `gate` | 查询服务端门禁结果，用退出码卡 CI |
| `report` | 保留本地报告能力，用于读取已有采集文件生成 HTML |

## 2. Git/CI 用法

```bash
TASK_ID=$(java -jar codeperf-cli.jar task \
  --server http://codeperf-server:9090 \
  --project "$CI_PROJECT_NAME" \
  --commit "$CI_COMMIT_SHA" \
  --branch "$CI_COMMIT_BRANCH" \
  --env preprod)

java -jar codeperf-cli.jar scan-diff \
  --base origin/main \
  --head HEAD \
  --target-package com.company.order \
  --classes-dir target/classes \
  --source-root src/main/java \
  --output build/codeperf/perf-static.json \
  --server http://codeperf-server:9090 \
  --task-id "$TASK_ID" \
  --upload

java -jar codeperf-cli.jar gate \
  --server http://codeperf-server:9090 \
  --task-id "$TASK_ID" \
  --fail-on WARN
```

## 3. 边界

- CLI 不再提供 attach 能力。
- CLI 不启动被测应用。
- CLI 不注入 agent 参数；预发应用由部署脚本以 `-javaagent:codeperf-agent.jar=config/agent.yml` 启动。
- CLI 的静态扫描是第一阶段主规则入口，动态证据由 Agent 上报到 Server 后再合并。
