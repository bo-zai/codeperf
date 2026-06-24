# 02 · Agent 采集核心设计

> 关联：`00-overview-architecture.md`、`01-demo-app.md`。本文件描述 `codeperf-agent` 模块设计。
> 编码若与本文档不一致，须及时回写本文档（项目铁律）。

## 1. 职责与边界

agent 被挂进**目标 JVM**（demo 或被测应用）内部，负责：
1. 在指定包(`target-package`)的方法上插桩，采集**调用树 + 每方法耗时/调用次数**；
2. 在 JDBC 层拦截，采集 **SQL 指纹 / 次数 / 单条耗时**（N+1、慢 SQL 的原料）；
3. 后台线程**采样**目标线程调用栈，得 CPU 热点；
4. 在入口请求前后取 `ThreadMXBean` 分配字节差，得**分配量**；
5. 以**会话**为单位组织上述数据，只采集匹配 `entry` 的 HTTP 请求；
6. 会话结束把**原始数据 dump 到文件**，供 CLI 端分析。

agent **不做**分析/打分/报告（那是 CLI 端的 `analysis` 层），只产出原始数据。职责分离便于 CI 期复用。

## 2. 双入口（可移植性决策①）

```
premain(String args, Instrumentation inst)    // -javaagent 静态挂载（CI 终态）
agentmain(String args, Instrumentation inst)   // attach 动态挂载（本地 MVP）
```
两者都委托给同一个 `AgentBootstrap.start(args, inst)`，共用全部采集核心。MANIFEST 配置：
```
Premain-Class: com.codeperf.agent.AgentEntry
Agent-Class:   com.codeperf.agent.AgentEntry
Can-Redefine-Classes: true
Can-Retransform-Classes: true
```
> agentmain 场景：目标类**已被加载**，必须用 `inst.retransformClasses()` 重定义已加载类才能插上桩。premain 场景类尚未加载，靠 transformer 在加载时织入。ByteBuddy 的 `AgentBuilder` + `RedefinitionStrategy.RETRANSFORMATION` 同时覆盖两种。

## 3. Agent 参数

attach 时由 CLI 把参数串传入（`agentmain` 的 String args）。格式：`key=value;key=value`。

| 参数 | 含义 | 示例 |
|---|---|---|
| `targetPackage` | 插桩的应用包前缀（可多个，逗号分��） | `com.codeperf.demo` |
| `entry` | 入口 HTTP 匹配：`METHOD PATH` | `POST /api/orders/report` |
| `slowSqlMs` | 慢 SQL 阈值（ms） | `500` |
| `output` | 原始数据 dump 文件路径 | `./perf-data.raw` |
| `sampleMs` | 栈采样周期（ms） | `10` |
| `mode` | `session`（默认，采一次匹配请求即可）或 `duration` | `session` |

> MVP 用 attach 注入参数。premain 形态用同样的 args 字符串（`-javaagent:agent.jar=targetPackage=...;entry=...`）。

## 4. 采集模型（会话化）

一次**会话** = 对一个 `entry` 采集到的一次（或多次）匹配请求的数据集合。MVP 默认采集**第一次**匹配请求（`mode=session`），采到后标记完成。

数据结构（agent 内存中累积，会话结束序列化）：

```
SessionData
├── meta: entry, targetPackage, startTime, jvm 信息
├── requests: List<RequestData>          // MVP 通常 1 个
│     ├── httpMethod, path, status, wallTimeMs
│     ├── threadName, threadId
│     ├── allocBytes                       // ThreadMXBean 前后差
│     ├── callTree: CallNode (树)          // 方法插桩聚合
│     │     └── method, count, selfTimeMs, totalTimeMs, children[]
│     └── sqls: List<SqlRecord>
│           └── rawSql, fingerprint, count(同指纹累计), totalMs, maxMs, calls[ts,ms]
└── samples: List<StackSample>            // 栈采样原始帧（按线程）
      └── threadId, frames[] (类.方法)
```

> **关联线程**：入口请求在某个 tomcat 工作线程上执行。agent 在入口方法进入时记录 `currentThreadId`，JDBC 拦截、栈采样、方法插桩都按线程归并到当前活跃请求，避免并发请求串扰。MVP 假设手动逐个发请求，并发低，按 threadId 归并已足够。

## 5. 三个采集器实现

### 5.1 方法插桩采集器 `MethodTraceCollector`
- ByteBuddy `AgentBuilder`，`type(nameStartsWith(targetPackage))`，对其所有方法织入 `@Advice.OnMethodEnter/OnMethodExit`。
- Advice 里维护**每线程的调用栈**（`ThreadLocal<Deque<CallNode>>`）：进方法压栈记 startNano，出方法弹栈算 totalTime、累加到父节点 children、自身 selfTime = total − Σ子total。
- 仅当该线程当前归属于“活跃请求”时才记录（否则忽略，降噪+降开销）。
- **排除**：构造器、`equals/hashCode/toString`、getter/setter 可选过滤（先全插，后续按开销决定是否过滤）。不插 JDK/框架类。

### 5.2 JDBC 采集器 `JdbcCollector`
- 插桩接口实现：对 `java.sql.Statement#execute*`、`PreparedStatement#execute*` 的实现类方法织入 Advice。
  - 入口拿 SQL 文本（PreparedStatement 取构造时记录的 SQL；普通 Statement 取入参）。
  - 出口算耗时，归并到当前请求的 `sqls`，按**指纹**累计。
- **SQL 指纹**：去掉字面量与 `?` 归一（`WHERE id = ? / 123` → 同指纹），用于 N+1 识别（同指纹高频）。指纹算法：小写化、连续空白归一、数字/字符串字面量替换为 `?`、`IN (?, ?, ...)` 折叠为 `IN (?)`。
- 慢 SQL：单条 `ms > slowSqlMs` 标记。
- 不依赖任何连接池/ORM，直接打 `java.sql.*` 实现类（H2、MySQL 驱动等都经过这些接口）。

### 5.3 栈采样采集器 `StackSampler`（可插拔 Profiler 接口 — 决策③）
- 接口 `Profiler { start(); stop(); List<StackSample> drain(); }`。
- MVP 实现 `JavaStackSampler`：守护线程，每 `sampleMs` 对**活跃请求线程**调用 `Thread.getStackTrace()`，记录帧序列。
- 仅采样活跃请求线程，避免全量线程噪声与开销。
- CI/Linux 期可加 `AsyncProfilerCollector implements Profiler`，不改其他代码。

## 6. 入口识别与会话边界

- 不直接依赖 Spring 类型（避免 classpath 耦合）。做法：把 `target` 之外**额外**插桩 Spring MVC 的分发点——具体选 `org.springframework.web.servlet.DispatcherServlet#doDispatch`，在其 Advice 里读取 `HttpServletRequest` 的 method+URI，与 `entry` 匹配；匹配则**开启活跃请求**（绑定当前 threadId、记 startNano、起 allocBytes 基线、通知 sampler 关注本线程），方法退出时**关闭活跃请求**并落库本次 RequestData。
- 若目标非 Spring（纯 Java），降级方案：用 `entry` 配一个普通方法全名作为入口（`mode` 之外加 `entryMethod` 参数，MVP 暂以 Spring 为主，纯 Java 入口留后续）。
- 反射读取 `HttpServletRequest`：agent 与目标共享 `javax.servlet` 类（目标应用里有），用反射调用 `getMethod()/getRequestURI()`，避免 agent 自身打包 servlet 依赖。

## 7. 控制通道（与 CLI 协作，详见 03-cli.md）

MVP 采用**最简文件 + 轮询**控制：
- attach 时参数已带齐（entry/output/duration），agent 启动即按配置自采。
- agent 采到目标请求（或 duration 到）后，把 `SessionData` 序列化写入 `output`，并写一个 `${output}.done` 标记文件。
- CLI `attach` 后轮询 `.done` 文件出现即视为采集完成，提示用户。
> 选文件通道而非 socket/MBean：实现最简、跨平台、无端口冲突，MVP 足够。CI 期可换控制通道，不影响采集核心。

## 8. 数据序列化格式

`output` 写 **JSON**（人可读、CLI 端 Jackson/自带解析皆可）。MVP 为减依赖，agent 端用**手写极简 JSON 序列化器**（或引入 Jackson；agent 尽量瘦，倾向手写或 shade）。最终决定：agent 用 **Jackson** 并 shade 进 agent.jar，避免与目标应用 classpath 冲突。

> shade：agent.jar 把 byte-buddy、jackson 重定位（relocate）到 `com.codeperf.agent.shaded.*`，防止污染目标应用。

## 9. 开销与安全

- 仅插目标包 + JDBC + DispatcherServlet，范围可控。
- 仅在“活跃请求”窗口内记录，非目标流量近乎零开销。
- 会话结束 `AgentBuilder` 可 `reset()` 撤销织入，恢复字节码（MVP 进程随后即用完，撤销非强制，但提供）。
- 异常隔离：所有 Advice 逻辑 try/catch 包裹，任何采集异常**绝不**影响目标业务执行。

## 10. 模块依赖与产物

```
codeperf-agent
├── 依赖: byte-buddy, jackson-databind（均 shade 重定位到 com.codeperf.agent.shaded.*）, javax.servlet-api(provided, 仅编译参考)
├── 产物: codeperf-agent.jar（含 MANIFEST 双入口 + shaded deps）
└── 包结构（实际实现）:
    com.codeperf.agent
    ├── AgentEntry              premain/agentmain 双入口
    ├── AgentBootstrap          装配各采集器、解析参数、启动采样、织入插桩
    ├── config/AgentConfig      参数模型（key=value;... 解析）
    ├── collect/
    │   ├── Recorder            ★ 全局运行时记录器：所有 Advice 委托到此；
    │   │                         内含活跃请求 ThreadLocal、调用栈、SQL/分配记录、入口匹配
    │   ├── InstrumentationInstaller  用 ByteBuddy AgentBuilder 把 5 个 Advice 织入目标类
    │   ├── Profiler            可插拔采样器接口（决策③）
    │   ├── JavaStackSampler    Profiler 的 MVP 实现（纯 Java 线程栈采样）
    │   ├── SqlFingerprint      SQL 指纹归一化
    │   ├── SessionWriter       JSON dump + .done 标记（Jackson）
    │   └── advice/
    │       ├── MethodTraceAdvice      应用包方法进/出
    │       ├── EntryAdvice            DispatcherServlet#doDispatch 入口匹配
    │       ├── JdbcStatementAdvice    Statement.execute*(String ...)
    │       ├── JdbcPreparedExecAdvice PreparedStatement 无参 execute*()
    │       └── JdbcPrepareBindAdvice  Connection#prepareStatement(String ...) 绑定 SQL
    └── session/
        ├── SessionData / RequestData / CallNode / SqlRecord / StackSample  数据模型
        （ActiveRequestContext 未独立成类，已折叠进 Recorder 的 ThreadLocal）
```

> 与初版设计的差异说明（实现时收敛）：① 不拆 MethodTraceCollector/JdbcCollector/EntryInterceptor 三个采集器类，统一由 `InstrumentationInstaller` 安装、`Recorder` 承载运行时状态，降低类数量与跨类状态传递成本；② Advice 类集中于 `collect/advice/`；③ `SessionWriter` 放在 `collect/` 包；④ 活跃请求上下文用 Recorder 内的 ThreadLocal 实现，未单列 `ActiveRequestContext`。

## 11. 验收点

attach 到 demo、发一次 `POST /api/orders/report` 后，`output` JSON 应包含：
- 1 个 RequestData，path=`/api/orders/report`，wallTime 明显偏高；
- sqls 中 `orders WHERE user_id=?` 指纹 count ≈ 用户数（N+1 原料）；
- callTree 含 `OrderReportService.computeScore/distinctItemNames` 且 selfTime 高；
- allocBytes 达数百 MB 量级；
- samples 中热点帧集中在 `computeScore`。
