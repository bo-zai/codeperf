# LoopIoAmplificationRule 规则设计

## 1. 规则定位

`LoopIoAmplificationRule` 是静态主规则，用于识别“循环内外部 I/O 调用”。

它不是动态规则，不依赖测试请求是否执行，也不依赖生产采集。

目标是尽早发现这种结构：

```java
for (Item item : items) {
    externalClient.query(item.getId());
}
```

## 2. 检测输入

第一阶段基于编译产物 `.class` 检测，沿用当前 ASM 字节码扫描能力。

需要收集：

- 方法列表。
- 循环区间。
- 方法调用指令。
- 调用 owner、method name、descriptor、是否接口调用。
- 类注解，例如 `@FeignClient`。
- 后续可扩展调用图，用于识别循环内调用本地方法后再间接 I/O。

## 3. 循环识别

当前项目已实现基于字节码回边的循环识别：

```text
如果跳转目标指令位置 <= 当前指令位置，则记录为循环区间。
```

该方式可覆盖常见 `for`、`while`、增强 for、部分迭代器循环。

第一阶段可继续沿用该实现。

## 4. 外部 I/O 识别

规则应将循环体内调用分为以下类型。

| 类型 | 识别信号 | 置信度 |
|---|---|---|
| DB | Repository、DAO、Mapper、JdbcTemplate、MyBatis、JPA Repository | HIGH |
| HTTP | Feign、RestTemplate、WebClient、OkHttpClient、HttpClient、RestClient | HIGH |
| RPC | Dubbo、gRPC、Thrift、公司内部 RPC Client | HIGH/MEDIUM |
| 外部 SDK | 类名包含 Client、Sdk、Gateway、Facade，且不在当前业务包内 | MEDIUM |
| 跨包 Service | owner 以 Service 结尾且不属于当前 target package | LOW |

第一阶段应允许配置公司内部关键字，例如：

```yaml
ioClassPatterns:
  - "*Client"
  - "*Rpc"
  - "*Gateway"
  - "com.company.remote.*"
```

## 5. 置信度

建议保留 `HIGH/MEDIUM/LOW` 三档。

| 置信度 | 含义 |
|---|---|
| HIGH | 明确外部 I/O，例如 Repository、Mapper、Feign、RestTemplate |
| MEDIUM | 高概率外部 I/O，例如通用 Client、SDK、Gateway |
| LOW | 疑似外部 I/O，例如跨包 Service |

置信度不等于严重度。严重度需要结合生产规模画像判断。

## 6. 严重度

严重度应由以下因素综合决定：

```text
静态置信度
生产 P95/P99 循环规模
测试/预发运行证据
外部调用类型
是否已有批量替代方案
```

建议初始策略：

| 条件 | 严重度 |
|---|---|
| HIGH 置信度 + 生产 P95 大规模 | CRITICAL |
| HIGH 置信度 + 生产规模未知 | WARN |
| MEDIUM 置信度 + 生产 P95 大规模 | WARN |
| LOW 置信度 + 缺少动态证据 | INFO |
| 已证明循环固定小规模 | INFO 或忽略 |

## 7. 报告内容

每条发现至少包含：

- 风险类型：循环内外部 I/O 放大。
- 位置：类名、方法名。
- 循环内调用：owner + method。
- I/O 类型：DB、HTTP、RPC、SDK。
- 静态置信度。
- 测试/预发证据，如果存在。
- 生产规模画像，如果存在。
- 放大倍数估算。
- 修复建议。

示例输出：

```text
风险：循环内 HTTP 调用
位置：OrderService.enrichOrders
调用：inventoryClient.queryStock()
静态置信度：HIGH
测试证据：本次请求执行 5 次
生产画像：orders P95 = 2000
推断：生产 P95 下可能放大为约 2000 次远程调用
建议：改为 queryStockBatch(orderIds)，或循环前预取库存数据
```

## 8. 误报场景

需要在报告中允许人工解释或降级：

- 循环次数固定很小。
- 循环内调用有本地缓存，实际不会发出远程 I/O。
- 调用方法名称像 Client，但实际是本地计算。
- 循环内调用的是批量接口的一部分封装。
- 该代码只在启动期或离线任务执行，不在用户请求链路中。

## 9. 漏报场景

第一阶段可能漏掉：

- 循环内调用本地方法，本地方法内部再调用外部 I/O。
- 外部调用通过反射、动态代理、脚本或统一框架隐藏。
- 公司内部 SDK 命名不符合默认规则。
- Kotlin/Scala 等语言编译产物生成复杂字节码，循环识别可能不完整。

后续需要通过调用图、可配置规则和框架适配降低漏报。

## 10. 修复建议模板

报告应提供可操作建议：

- 将逐条调用改为批量调用。
- 循环前一次性查询或预取数据。
- 使用 Map/Set 做内存关联。
- 对重复查询结果增加请求级缓存。
- 对不可避免的远程调用增加并发上限、超时、熔断和降级。
- 对非关键链路改为异步处理。

