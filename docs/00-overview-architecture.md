# CodePerf 架构总览（00 · 主设计文档）

> 本文件是项目主设计文档，记录整体定位、模块划分、采集分层、分析规则与可移植性决策。
> 开发流程：每个关键功能编码前先在 `docs/` 写设计文档，再编码；编码若有改动，及时回写本目录对应文档。

## 1. 工具定位

CodePerf 是面向 Java 代码的**提交前性能检测工具**。目标：研发完成开发、代码合入主干前，提前发现潜在性能问题。

检测策略 = **混合（静态 + 运行态）**：
- 静态分析：秒级、零成本，抓确定性反模式，可做门禁（MVP 暂不做，留 1.5 期）。
- 运行态画像：精准定位真��热点、SQL、内存，回答“哪段代码慢、慢多少、为什么慢”。
- 交叉验证：静态可疑点 ∩ 运行态确认的真实热点 = 高可信问题，降低误报（CI 期）。

## 2. 关键约束（决定一切设计）

| 约束 | 取值 | 影响 |
|---|---|---|
| 入口形态 | 以 HTTP 接口为主 | 用 Spring MVC 拦截识别入口请求 |
| 团队是否有单测 | 无 | 不能靠“复用测试”驱动 |
| 运行环境 | 不用开发者本地（终态在 CI/CD 远端 Linux） | 驱动靠“对运行中的应用发流量 + agent 采集” |
| 目标应用 JDK | Java 8 / JDK 1.8 | **否决 JFR**（JDK8 无），CPU 采样改用纯 Java 线程栈采样 |
| 本机开发环境 | Windows 11 | async-profiler 不可用，故采样默认纯 Java 实现 |

## 3. 分期路线

- **MVP（当前）**：本地 `attach` 到运行中的 JVM(pid) → 开发者手动发 HTTP 请求 → agent 采集一次请求的多维画像 → 分析 → HTML 报告。
- **1.5 期**：静态规则集、基线对比（主干 vs 当前分支）、内存 GC/锁分析。
- **CI/CD 期（终态）**：流水线部署时用 `-javaagent` 静态挂载 agent，请求脚本/鉴权自动发流量，出报告 + 退出码卡门禁；Linux runner 上可额外接 async-profiler 出火焰图。

> MVP 与 CI 期**复用同一套采集核心 / 分析 / 报告**，差异只在最外层“挂载方式 + 流量驱动 + 运行位置”。

## 4. 技术选型与取舍

- **字节码插桩用 ByteBuddy**（封装于 ASM 之上，attach/redefine 支持好、开发效率高），不用裸 ASM。
- **不引入 Arthas 作依赖**：Arthas 定位是“人在线交互排障”，不提供编程 SDK；其最值钱的 profiler 火焰图底层是 async-profiler（Windows 不可用）；而 MVP 价值大头（SQL/N+1 语义聚合、会话化采集、规则引擎、报告）Arthas 都不提供。借鉴其 advice weaver 思路，但不依赖。
- **CPU 采样用纯 Java 线程栈采样**：后台线程周期性抓目标线程栈聚合成调用树/热点。跨平台、JDK8 可用、零原生依赖。设计为可插拔 `Profiler` 接口，CI 期可加 `AsyncProfilerCollector`。
- **SQL 采集在 JDBC 层**（插桩 `java.sql.*`）：ORM 无关，MyBatis/JPA/JdbcTemplate 通吃。
- **分配量近似**用 `ThreadMXBean.getThreadAllocatedBytes()` 在入口前后取差。

## 5. 模块划分

```
codeperf-parent (pom)
├── codeperf-demo     靶子应用：Spring Boot 2.7 + H2 + JdbcTemplate，埋已知性能坑（开发/回归夹具）
├── codeperf-agent    采集核心：双入口(premain/agentmain) + ByteBuddy 插桩 + JDBC 拦截 + 采样器 + 会话化采集 + 数据 dump
└── codeperf-cli      命令行：attach 动态挂 agent / 控制会话 / 拉数据 / report 生成报告 + 退出码门禁
```

> 模块逐功能加入 parent 的 `<modules>`：先写设计文档再编码。当前仅 `codeperf-demo` 已纳入。

## 6. 运行态采集分层

| 层级 | 技术 | 产出 | 对应规则 |
|---|---|---|---|
| 调用树 + 方法耗时/次数 | ByteBuddy 插桩（仅 `--target-package`，不碰 JDK/框架内部） | 每方法 wall-time、调用次数、call-graph | CPU 热点、请求耗时长 |
| SQL 画像 | JDBC 接口插桩 | SQL 指���频率表、N+1、慢 SQL | N+1、慢 SQL、循环内 SQL |
| CPU 热点 | 纯 Java 线程栈采样 | 热点 Top-N、火焰图数据 | CPU 热点 |
| 分配量 | `ThreadMXBean` 前后差 | 每请求分配字节数 | 分配量异常 |

采集模式：**会话化**——CLI 开启会话并设定 `entry`(HTTP 路径+方法) / `target-package` / `duration`；agent 只采集匹配 `entry` 的请求；会话结束 dump 原始数据，并恢复字节码、卸载插桩。

## 7. 分析规则引擎（严重度排序）

| 规则 | 数据来源 | 严重度 |
|---|---|---|
| N+1：同 SQL 指纹在单请求内执行 ≥ 阈值 | JDBC | 🔴 严重 |
| 慢 SQL：单次执行 > 阈值(如 500ms) | JDBC | 🔴 严重 |
| CPU 热点：单方法占比 > 阈值(如 30%) | 采样 | 🔴 严重 |
| 请求耗时长：入口 > 阈值(如 3s) | 插桩 | 🟡 警告 |
| 循环内 SQL：SQL 出现在闭环调用中 | 插桩+JDBC | 🟡 警告 |
| 分配量异常：单请求 > 阈值(如 100MB) | 分配量 | 🟡 警告 |
| 大对象新建 | 插桩 | 🔵 提示 |

阈值集中配置，便于 CI 期做门禁。

## 8. 部署与使用（MVP）

产出物：`codeperf-agent.jar` + `codeperf-cli.jar`。

```bash
# 1. 正常启动靶子/被测应用（须用 JDK8 运行）
# 2. attach 并设置入口与目标包
java -jar codeperf-cli.jar attach --pid <PID> \
     --target-package com.codeperf.demo \
     --entry "POST /api/orders/report" --duration 30s
# 3. 手动发请求（curl/Postman/浏览器）打目标接口
# 4. 会话结束后生成报告
java -jar codeperf-cli.jar report --input ./perf-data.raw --output ./perf-report.html
```

注意：CLI 的 attach 在 JDK8 下依赖 `$JAVA_HOME/lib/tools.jar`，须在 JDK��非 JRE）下运行。

## 9. 可移植性决策（为 CI/Linux 终态预留，v1 即落地）

1. agent 同时实现 `premain`(可 `-javaagent` 静态挂，CI 用) + `agentmain`(attach 动态挂，本地 MVP 用)，共用同一采集核心。
2. 挂载层 / 控制通道 / 流量驱动 与 采集核心 / 分析 / 报告 解耦，换形态只动外层。
3. 采样器抽象为可插拔 `Profiler` 接口。
4. 路径/临时目录用跨平台 API，不硬编码分隔符。
5. 报告器内置“按最高严重度返回退出码”，门禁能力天生具备。

## 10. 本机开发环境备忘

- JDK8 安装于 `D:\Java8\jdk1.8.0_341`（= JAVA_HOME，含 `lib/tools.jar`）。
- PATH 上默认 `java` 为 19，**运行 demo/agent 必须显式用 JDK8 的 `java`**。
- 未安装系统 Maven（构建经由 IntelliJ 内置 Maven，或后续加 Maven Wrapper）。
