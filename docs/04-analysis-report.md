# 04 · 分析引擎与报告

> 关联：`00-overview-architecture.md`、`02-agent-core.md`、`03-cli.md`。
> 编码若与本文档不一致，须及时回写（项目铁律）。

## 1. 职责

分析引擎读取 agent 产出的原始数据 JSON（`perf-data.raw`），应用多条检测规则，生成结构化问题列表，最终输出自包含 HTML 报告。同时返回最高严重度级别用于 CI 门禁。

## 2. 输入数据结构

Agent 产出的 JSON 结构（见 02 §8）:

```
SessionData {
  entryMethod, entryPath, targetPackages, startTimeEpochMs, javaVersion
  requests: [{
    httpMethod, path, status, wallTimeMs, threadName, threadId, allocBytes
    callTree: { method, count, totalTimeMs, selfTimeMs, children[] }
    sqls: [{ fingerprint, sampleSql, count, totalMs, maxMs, slow }]
    samples: [{ frames[] }]
  }]
}
```

分析端使用 Jackson `JsonNode` 树模型解析，避免依赖 agent 模块的 POJO 类。

## 3. 检测规则

每条规则产生 0-N 个 `Finding`（问题 + 严重度 + 证据）。

### 3.1 N+1 查询检测 (NPlusOneRule)

逐请求遍历 `sqls`，对每条 SQL 指纹判断：
- count <= 2 → 正常（如首次查询 users + 查询 orders）
- 3 <= count <= 9 → **INFO**：疑似 N+1
- 10 <= count <= 49 → **WARN**：明显 N+1
- count >= 50 → **CRITICAL**：严重 N+1

附加证据：显示 sampleSql + count + 总耗时。

### 3.2 慢 SQL 检测 (SlowSqlRule)

逐请求遍历 `sqls`：
- 若 `slow==true` 且 maxMs >= 1000 → **CRITICAL**
- 若 `slow==true` 且 maxMs >= 500 → **WARN**
- 若 `slow==true` 且 maxMs < 500 → **INFO**

附加证据：显示 sampleSql + maxMs + count。

### 3.3 CPU 热点检测 (CpuHotspotRule)

汇总所有 `samples`，统计每个方法出现在栈顶（frames[0]）的次数：
- 占比 >= 30% 且 sample 总数 >= 20 → **WARN**
- 占比 >= 10% 且 sample 总数 >= 10 → **INFO**

附加证据：显示方法名 + 出现次数/总采样数。

### 3.4 高内存分配检测 (HighAllocRule)

逐请求检查 `allocBytes`：
- allocBytes >= 500MB → **CRITICAL**
- allocBytes >= 100MB → **WARN**
- allocBytes >= 50MB → **INFO**

附加证据：显示 allocBytes（格式化 MB）+ wallTimeMs。

### 3.5 高延迟检测 (HighLatencyRule)

逐请求检查 `wallTimeMs`：
- wallTimeMs >= 5000ms → **CRITICAL**
- wallTimeMs >= 2000ms → **WARN**
- wallTimeMs >= 1000ms → **INFO**

附加证据：显示 wallTimeMs + path + status。

## 4. 严重度枚举

```java
public enum Severity { INFO(1), WARN(2), CRITICAL(3) }
```

- `none(0)`: 不卡门禁
- `info(1)`: 有 info 级别及以上 → 非零
- `warn(2)`: 有 warn 级别及以上 → 非零
- `critical(3)`: 有 critical 级别 → 非零

## 5. HTML 报告结构

自包含 HTML（内联 CSS），包含以下区块：

1. **概览**：entry、时间、总请求数、问题总数（按严重度分桶）
2. **问题列表**：按严重度降序排列，每条显示严重度标签 + 类型 + 描述 + 证据
3. **请求详情**：每个请求的调用树（缩进文本）、SQL 汇总表、CPU 热点 top N

CSS 使用深色头部 + 白色内容区的简洁风格，严重度标签带颜色（红=CRITICAL，橙=WARN，蓝=INFO）。

## 6. 模块结构

所有分析代码位于 `com.codeperf.analysis` 包，属于 `codeperf-cli` 模块：

```
com.codeperf.analysis
├── AnalysisFacade      # 入口：parse JSON → run rules → generate HTML → return exit code
├── Severity            # 严重度枚举
├── Finding             # 单条检测发现
├── AnalysisEngine      # 编排所有规则，汇总 Finding
├── rules/
│   ├── AnalysisRule    # 接口: List<Finding> analyze(JsonNode session)
│   ├── NPlusOneRule
│   ├── SlowSqlRule
│   ├── CpuHotspotRule
│   ├── HighAllocRule
│   └── HighLatencyRule
└── HtmlReport          # HTML 生成器
```

## 7. 使用示例

```bash
# 仅分析
java -jar codeperf-cli.jar report --input perf-data.raw --output report.html --fail-on warn
# exit code: 0=pass, 非零=fail
```
