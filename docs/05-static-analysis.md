# 05 · 静态分析引擎（v1.5）

> 关联：`00-overview-architecture.md`、`04-analysis-report.md`。本文件描述 v1.5 静态字节码扫描引擎及与动态采集的交叉验证机制。
> 编码若与本文档不一致，须及时回写（项目铁律）。

## 1. 动机

MVP 只有动态采集，存在两个盲区：
- **漏报风险**：只采了一个请求，没覆盖到的路径可能有问题
- **置信度单一**：所有发现都是"已发生"的事实，缺少"预判→验证"链条

v1.5 加静态扫描：秒级完成、零部署、不用发请求。静态结果与动态结果交叉验证，提升问题置信度。

## 2. 扫描对象

扫描**已编译的 `.class` 文件**（target 目录），用 ASM 解析字节码。

- 无需源码、无需新增依赖（ASM 已是 ByteBuddy 的传递依赖，坐标 `org.ow2.asm:asm:9.x`）
- 被扫类范围 = `--target-package` 参数指定

## 3. 静态规则（4 条）

每条规则产出一个 `StaticFinding`，包含 `confidence`（LOW/MEDIUM/HIGH），表示"仅凭静态分析有多大把握"。

### 3.1 循环内调用远程操作 → 疑似 N+1

N+1 的本质是：将 1 次批量 I/O 拆成了 N 次独立 I/O，不管 I/O 目标是**数据库**还是**远程 API**。

#### 3.1.1 分支 A：调 ORM/Repository（数据库 N+1）

在字节码层面，直接检测"循环内调了数据访问层方法"：

- 循环范围内出现 `INVOKEINTERFACE` / `INVOKEVIRTUAL`，且目标类名包含 `Repository` / `DAO` / `Mapper`
- 置信度：**HIGH**

**用例**：`OrderReportService.generateReport()` 的 for-each 内调 `findOrdersByUserId`

#### 3.1.2 分支 B：调远程 API（网络 N+1）

在字节码层面，检测"循环内调了会发出 HTTP/RPC 的方法"。判定信号：

| 信号 | 字节码特征 | 示例 |
|---|---|---|
| Feign 客户端 | `INVOKEINTERFACE`，目标接口名含 `FeignClient`、`Client`，或注解 `@FeignClient` 存在于该类 | `inventoryClient.checkStock(id)` |
| RestTemplate | `INVOKEVIRTUAL`，目标类为 `org.springframework.web.client.RestTemplate`，方法名匹配 `getForObject/getForEntity/postForObject/postForEntity/exchange` | `restTemplate.getForObject(...)` |
| WebClient (Reactive) | `INVOKEINTERFACE`，目标类为 `org.springframework.web.reactive.function.client.WebClient`，方法名匹配 `get/put/post/delete` 后跟 `retrieve`/`exchange` | `webClient.get().uri(...)` |
| 通用 HTTP 客户端 | `INVOKEVIRTUAL`，目标类名含 `HttpClient`、`OkHttpClient`、`RestClient`，且调用非构造方法 | `httpClient.execute(...)` |
| 跨服务 Service 调用 | `INVOKEVIRTUAL`，目标类名以 `Service` 结尾且不在当前包前缀下（说明是外部模块的 Service，很可能发远程调用） | `remoteOrderService.create(...)` |

检测流程：
1. 先做循环边界检测，定位所有循环区间
2. 遍历循环体内的每条方法调用指令
3. 若满足上述任一信号 → 产出 N+1 嫌疑发现，置信度：

| 信号 | 置信度 |
|---|---|
| Repository/DAO/Mapper | **HIGH** |
| Feign 客户端接口 | **HIGH** |
| RestTemplate | **HIGH** |
| WebClient | **HIGH** |
| 通用 HttpClient | **MEDIUM** |
| 跨包 Service | **LOW**（可能是本地 Service，需动态确认） |

综合评定时，若一个循环触发多条信号，记录**最高置信度**那一条。

**用例**：
```java
// 网络 N+1 典型场景
for (Order o : orders) {
    Inventory i = inventoryClient.checkStock(o.getItemId());  // Feign: HIGH
}
for (String id : ids) {
    restTemplate.getForObject("http://svc/api/" + id, ...);   // RestTemplate: HIGH
}
```

### 3.2 循环内 List.contains() 或嵌套循环 → O(n²)

**检测逻辑**：
- 循环内出现 `List.contains` 或 `ArrayList.contains` 调用 → O(n²)，置信度 **HIGH**
- 两层嵌套循环（循环体内又含循环）→ 置信度 **MEDIUM**

**用例**：`distinctItemNames()` 的 `result.contains(item)`

### 3.3 计算密集型方法被入口路径调用

**检测逻辑**：
- 方法体内 `Math.*` 调用次数 ≥ 5 次，或方法内出现大 int 常量 > 1,000,000（`ICONST/BIPUSH/SIPUSH/LDC` 任一来源，作为大循环边界的近似信号）
- 置信度：**MEDIUM**

> **as-built 说明（v1.5）**：设计原意是"且该方法可从 HTTP entry 路径到达（caller 链可达）"。v1.5 未构建调用图，**入口可达性下放到第 4 节的交叉验证**完成——静态先无差别标记 MEDIUM，再由动态调用树决定它是"已确认"还是"待验证"。
> **已知局限**：大常量信号不区分用途，循环边界常量与大数组长度常量都会命中，因此 `buildReportText()` 的 `1048576` 会同时触发本规则与 3.4，属可接受的重复提示。

**用例**：`computeScore()` 含 40M 循环 + Math.sqrt + Math.sin

### 3.4 循环内大数组分配

**检测逻辑**：
- 循环体内出现 `NEWARRAY` / `ANEWARRAY` / `MULTIANEWARRAY`，估算字节数 ≥ 1024 **或长度为变量**（无法静态确定时也告警）
  - 字节数估算：基本类型数组 = 长度 × 元素宽度（boolean/byte=1，char/short=2，int/float=4，long/double=8）；引用数组按 8 字节/元素估算
  - 长度取自紧邻分配指令前压栈的常量（`ICONST/BIPUSH/SIPUSH/LDC`）；取不到则视为变量尺寸（size=-1）
- 置信度：**MEDIUM**
- 非循环中的大数组分配（≥ 1MB）→ 置信度 **LOW**

**用例**：`buildReportText()` 循环内 `new byte[1048576]`

## 4. 交叉验证机制

当同时拥有静态扫描结果（`scan` 命令产出）和动态采集数据（`attach` 产出），`report` 命令自动合并。

### 匹配策略

静态发现的 `classMethod`（如 `com.demo.OrderReportService.generateReport`）与动态调用树节点 `CallNode.method` 做规范化后缀匹配：
- 规范化：把 `#`、`/` 统一为 `.`，并截掉参数签名（`(` 之后部分）
- 例：静态 `OrderReportService.generateReport`，动态节点 `...OrderReportService.generateReport(...)` → 匹配成功

### 置信度升级/降级

| 静态 | 动态 | 最终判定 | v1.5 实现 |
|---|---|---|---|
| 发现 X，置信度 H/M/L | 发现 X（同方法在调用树中被执行） | **已确认**，confidence=HIGH，保留静态 severity | ✅ 已实现 |
| 发现 X，置信度 H/M/L | 无动态数据（未采集/未触发） | **待验证**，保留静态 severity，confidence 降一级，标注"需动态验证" | ✅ 已实现 |
| 发现 X | 动态显示正常（count=1 等） | **误报**，降为 INFO 或不展示（视阈值） | ⏳ 后续迭代（v1.5 未做 count 级降噪） |
| 无静态发现 | 动态发现 X | **仅动态发现**，保持原有严重度（动态数据永远不丢弃） | ✅ 天然满足（动态规则始终独立运行） |

> **as-built 说明（v1.5）**：匹配粒度为"方法是否被执行"，尚未做"同 SQL / 同调用次数"的细粒度比对，也未实现 count=1 的误报降噪。"取两者中更严重的 severity"在 v1.5 简化为"保留静态 severity"，动态自身的 Finding 仍独立产出，故整体不会丢严重度。

## 5. 模块结构

所有静态分析代码位于 `codeperf-cli` 模块下新包 `com.codeperf.analysis.staticanalysis`（`static` 是 Java 关键字不能作包名，故用 `staticanalysis`）：

```
com.codeperf.analysis.staticanalysis
├── StaticScanner          # 入口：遍历目录下属于 targetPackage 的 .class → 跑全部规则 → StaticResult
├── StaticResult           # 扫描整体结果（targetPackage / classesScanned / findings），Jackson 序列化为 perf-static.json
├── StaticFinding          # 单条静态发现（type/severity/confidence/description/evidence/classMethod）
├── BytecodeRule           # 规则接口: List<StaticFinding> analyze(List<ClassAnalysis> classes, String targetPackage)
├── ClassAnalysis          # 类/方法级分析数据模型（含 MethodAnalysis / CallSite / AllocSite 内部类）
├── BytecodeAnalyzer       # ASM ClassVisitor：byte[] → ClassAnalysis（循环区间/调用点/分配点/Math 计数/最大 int 常量）
├── ClasspathResolver      # 自动解析 target/classes、build/classes/java/main、out/production/classes、bin
└── rules/
    ├── NPlusOneSuspect     # 循环内调 Repository/DAO/Mapper（DB），或 Feign/RestTemplate/WebClient/HttpClient/跨包 Service（网络）
    ├── NSquaredSuspect     # 循环内 List.contains / 嵌套循环
    ├── HeavyComputeSuspect # Math 密集 / 大 int 常量
    └── LargeAllocSuspect   # 循环内 new array / 非循环大数组
```

> **as-built 说明（v1.5）**：`BytecodeRule.analyze` 接收的是**已解析的 `List<ClassAnalysis>`**而非原始 `ClassReader`——`BytecodeAnalyzer` 先把每个 `.class` 解析成 `ClassAnalysis`（一遍 ASM 访问），规则只在数据模型上查询，互不重复解析字节码。`BytecodeRule` 与 `ClassAnalysis` 置于 `staticanalysis` 顶层包（规则与数据模型共享），`rules/` 子包仅放 4 条规则实现。

**循环检测算法**：`BytecodeAnalyzer` 为每条指令维护递增索引；遇到跳转/switch 指令时，若其目标 label 的指令索引 ≤ 当前索引（回边），即记为一个循环区间 `[目标, 当前]`。`MethodAnalysis.inLoop(idx)` 判断指令是否落在任一区间内；`NSquaredSuspect` 通过"某区间被另一区间真包含"判定嵌套循环。

CLI 新增子命令：

```
com.codeperf.cli.cmd
└── ScanCommand           # scan 参数 + 编排逻辑
```

## 6. scan 命令参数

| 参数 | 必填 | 默认 | 说明 |
|---|---|---|---|
| `--target-package` | 是 | — | 扫描的应用包前缀 |
| `--classes-dir` | 否 | 自动探测 | 编译产物目录（target/classes 等） |
| `--output` | 否 | `perf-static.json` | 静态分析结果输出 |
| `--report` | 否 | — | 若指定，直接生成 HTML 报告（不需要动态数据） |

## 7. 与动态 report 的合并

`report` 命令增加 `--static` 参数：

```bash
# 先扫静态
java -jar codeperf-cli.jar scan --target-package com.codeperf.demo --output static.json

# attach 采集...
java -jar codeperf-cli.jar attach ...

# 合并出报告
java -jar codeperf-cli.jar report \
  --input perf-data.raw \
  --static static.json \
  --output report.html \
  --fail-on warn
```

如果同时给出 `--input` 和 `--static`，AnalysisFacade 先分别跑静态规则和动态规则，再执行交叉验证合并。

## 8. 使用示例

```bash
JDK8=/d/Java8/jdk1.8.0_341

# 仅静态扫描（秒级出结果，不启动任何应用）
"$JDK8/bin/java" -jar codeperf-cli.jar scan \
  --target-package com.codeperf.demo \
  --report static-report.html

# 或者：静态 + 动态完整流程
"$JDK8/bin/java" -jar codeperf-cli.jar scan \
  --target-package com.codeperf.demo --output static.json
# ... attach + 发请求 ...
"$JDK8/bin/java" -jar codeperf-cli.jar report \
  --input perf-data.raw --static static.json --fail-on warn
```

## 9. v1.5 实现状态与验证

**已落地**：4 条静态规则（N+1 含 DB + 网络分支、O(n²)、计算密集、大数组分配）、`scan` 子命令、`report --static` 交叉验证、静态 HTML 报告。

**对 `codeperf-demo` 编译产物的扫描结果**（`scan`，退出码 0）：

| 来源方法 | 命中规则 | confidence |
|---|---|---|
| `OrderReportService.generateReport` | N+1（循环内 `UserRepository#findOrdersByUserId`） | HIGH |
| `OrderReportService.distinctItemNames` | O(n²)（循环内 `List.contains`） | HIGH |
| `OrderReportService.computeScore` | 计算密集（40M 循环） | MEDIUM |
| `OrderReportService.buildReportText` | 大数组分配（循环内 `new byte[1MB]`）+ 计算密集 | MEDIUM |
| `DataSeeder.run` | O(n²) / 计算密集 | MEDIUM |

**交叉验证结果**（`report --input perf-data.raw --static ... --fail-on warn`）：
- `generateReport` / `distinctItemNames` / `computeScore` / `buildReportText` → **已确认**（动态调用树命中）
- `DataSeeder.run`（启动期，不在 HTTP 调用树）→ **待验证**，confidence 自动降级
- 门禁正确触发，退出码 **3**（动态侧存在 CRITICAL ≥ warn 阈值）

**已知局限**（留待后续迭代）：
- 计算密集规则未做调用图可达性分析，依赖交叉验证近似"入口可达"
- 大 int 常量信号不区分循环边界与数组长度，存在与 3.4 的重复提示
- 交叉验证未实现 count=1 误报降噪，未做同 SQL 细粒度比对
