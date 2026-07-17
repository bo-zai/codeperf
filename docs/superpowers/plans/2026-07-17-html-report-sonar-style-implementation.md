# HTML Report Sonar Style Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 CodePerf 本地 HTML 报告改造成 Sonar 风格的三栏诊断工作台。

**Architecture:** 保留 `SourceScanHtmlReportWriter` 作为对外入口，新增小型资源类承载 CSS 与 JS，避免继续扩大单文件。HTML 输出改为顶部质量门禁、左侧过滤栏、中间 issue 列表、右侧详情/索引，检测结果和 JSON schema 不变。

**Tech Stack:** Java 8、JUnit 5、原生 HTML/CSS/JavaScript、JavaParser 扫描结果模型。

---

### Task 1: 调整 HTML 报告测试期望

**Files:**
- Modify: `codeperf-cli/src/test/java/com/codeperf/cli/report/SourceScanHtmlReportWriterTest.java`

- [ ] **Step 1: 将主测试改为校验 Sonar 风格工作台结构**

在 `should_WriteDeveloperFocusedHtmlReport_When_SourceScanResultProvided` 中，将旧的卡片式断言替换为以下核心断言：

```java
assertTrue(html.contains("CodePerf 本地代码扫描报告"));
assertTrue(html.contains("质量门禁"));
assertTrue(html.contains("检测失败"));
assertTrue(html.contains("class=\"app-shell\""));
assertTrue(html.contains("class=\"filter-panel\""));
assertTrue(html.contains("class=\"issue-workspace\""));
assertTrue(html.contains("class=\"detail-panel\""));
assertTrue(html.contains("class=\"issue-row active\""));
assertTrue(html.contains("data-detail"));
assertTrue(html.contains("data-module=\"root\""));
assertTrue(html.contains("data-io=\"DB\""));
assertTrue(html.contains("data-confidence=\"HIGH\""));
assertTrue(html.contains("data-scope=\"UNKNOWN\""));
assertTrue(html.contains("循环内外部 I/O 调用可能被生产数据量放大"));
assertTrue(html.contains("src/main/java/com/acme/OrderService.java"));
assertTrue(html.contains("OrderService.java:6"));
assertTrue(html.contains("源码片段"));
assertTrue(html.contains("调用链"));
assertTrue(html.contains("修复建议"));
assertTrue(html.contains("未归因"));
assertTrue(html.contains("orderMapper.selectById(id); // &lt;risk&gt;"));
assertTrue(html.contains("orderMapper.selectById(id) &lt;risk&gt;"));
assertTrue(html.contains("Broken.java: unexpected &lt;token&gt;"));
assertFalse(html.contains("<table"));
assertFalse(html.contains("top-card"));
assertFalse(html.contains("hero"));
```

- [ ] **Step 2: 将多风险测试改为校验列表与详情数量一致**

在 `should_WriteFileIssueSummaryAndNestedNavigation_When_MultipleFindingsInSameFile` 中保留三条 finding，断言内容调整为：

```java
assertTrue(html.contains("class=\"file-card\""));
assertTrue(html.contains("PrePushRiskDemoService.java"));
assertTrue(html.contains("3 个问题"));
assertTrue(html.contains("HIGH 2"));
assertTrue(html.contains("MEDIUM 1"));
assertTrue(html.contains("DB 2"));
assertTrue(html.contains("SDK 1"));
assertTrue(html.contains("id=\"issue-0\""));
assertTrue(html.contains("id=\"issue-1\""));
assertTrue(html.contains("id=\"issue-2\""));
assertTrue(html.contains("id=\"detail-0\""));
assertTrue(html.contains("id=\"detail-1\""));
assertTrue(html.contains("id=\"detail-2\""));
assertTrue(html.contains(">L5<"));
assertTrue(html.contains(">L10<"));
assertTrue(html.contains(">L11<"));
assertTrue(html.contains("data-target=\"detail-0\""));
assertTrue(html.contains("data-target=\"detail-1\""));
assertTrue(html.contains("data-target=\"detail-2\""));
```

- [ ] **Step 3: 运行测试确认失败**

Run: `mvn -pl codeperf-cli test -DskipTests=false -Dtest=SourceScanHtmlReportWriterTest`

Expected: FAIL，失败原因是当前 HTML 仍输出旧的 `hero/top-card/side-index` 结构。

### Task 2: 拆出 HTML 静态资源

**Files:**
- Create: `codeperf-cli/src/main/java/com/codeperf/cli/report/SourceScanHtmlAssets.java`

- [ ] **Step 1: 新增 CSS/JS 资源类**

创建 `SourceScanHtmlAssets`，包含 `style()` 和 `script()` 两个静态方法。CSS 使用 Sonar 风格配色，JS 只负责过滤、选中详情、hash 定位和空状态显示。

关键类名必须包含：

```text
app-shell
topbar
quality-gate
filter-panel
issue-workspace
issue-row
detail-panel
detail-card
source-block
```

- [ ] **Step 2: 保持资源类不可实例化**

类定义为 `final`，构造方法私有：

```java
final class SourceScanHtmlAssets {

    private SourceScanHtmlAssets() {
    }
}
```

### Task 3: 重构 SourceScanHtmlReportWriter 输出结构

**Files:**
- Modify: `codeperf-cli/src/main/java/com/codeperf/cli/report/SourceScanHtmlReportWriter.java`

- [ ] **Step 1: 替换 document start**

`appendDocumentStart` 改为引用 `SourceScanHtmlAssets.style()`，body 使用：

```html
<body><div class="report-page">
```

- [ ] **Step 2: 替换顶部区域**

删除旧 `appendHero` 调用，新增 `appendTopBar`，输出：

```html
<header class="topbar">
  <div class="topbar-title">CodePerf 本地代码扫描报告</div>
  <div class="quality-gate failed">质量门禁 检测失败</div>
  <div class="summary-metrics">...</div>
</header>
```

- [ ] **Step 3: 替换主体布局**

`render` 主体调整为：

```java
appendTopBar(html, result, sortedFindings, moduleResolver);
html.append("<div class=\"app-shell\">");
appendFilterPanel(html, sortedFindings, moduleResolver);
appendIssueWorkspace(html, sortedFindings, moduleResolver);
appendDetailPanel(html, sortedFindings, projectRoot, moduleResolver);
html.append("</div>");
appendParseErrors(html, result);
appendScript(html);
```

- [ ] **Step 4: 中间列表按模块和文件分组**

新增 `appendIssueWorkspace`，按模块、文件输出 `file-card` 和 `issue-row`。每条 issue row 必须有稳定属性：

```html
id="issue-0"
data-issue-row
data-target="detail-0"
data-module="codeperf-demo-admin"
data-io="DB"
data-severity="WARN"
data-confidence="HIGH"
data-scope="UNKNOWN"
```

- [ ] **Step 5: 右侧详情为每个 finding 输出 detail-card**

新增 `appendDetailPanel`，为每条 finding 输出一个 `detail-card`。默认第一条 active，其余隐藏。详情中保留源码片段、调用链、归因、修复建议、循环范围和证据。

- [ ] **Step 6: 删除旧 Top 风险大卡片与右侧静态目录**

删除或停止调用旧 `appendTopFindings`、旧 `appendFindingGroups`、旧 `appendSideIndex`。保留可复用的 `groupedByModule`、`readSourceSnippet`、`escape` 等 helper。

### Task 4: 跑测试并修正细节

**Files:**
- Modify: `codeperf-cli/src/main/java/com/codeperf/cli/report/SourceScanHtmlReportWriter.java`
- Modify: `codeperf-cli/src/main/java/com/codeperf/cli/report/SourceScanHtmlAssets.java`
- Modify: `codeperf-cli/src/test/java/com/codeperf/cli/report/SourceScanHtmlReportWriterTest.java`

- [ ] **Step 1: 运行 HTML writer 单测**

Run: `mvn -pl codeperf-cli test -DskipTests=false -Dtest=SourceScanHtmlReportWriterTest`

Expected: PASS，3 个测试全部通过。

- [ ] **Step 2: 运行 CLI 模块测试**

Run: `mvn -pl codeperf-cli test -DskipTests=false`

Expected: PASS，CLI 全部测试通过。

### Task 5: 真实 demo 验证

**Files:**
- Generated: `.codeperf/report/source-report.html`
- Generated: `.codeperf/report/source-report.json`

- [ ] **Step 1: 打包 CLI**

Run: `mvn -pl codeperf-cli package -DskipTests`

Expected: BUILD SUCCESS。

- [ ] **Step 2: 执行全量扫描**

Run: `java -jar codeperf-cli\target\codeperf-cli.jar scan --all`

Expected: Exit code `1`，因为 demo 中保留预期阻断风险。输出应包含：

```text
[codeperf] 扫描文件=8，风险总数=5，阻断风险=5，结果=失败，解析错误=0
[codeperf] 模块 codeperf-demo-admin：风险=2
[codeperf] 模块 codeperf-demo-app：风险=2
[codeperf] 模块 codeperf-demo-common：风险=1
[codeperf] htmlReportUrl=file:///D:/workspace/codeperf/.codeperf/report/source-report.html
```

- [ ] **Step 3: 检查报告 HTML 关键结构**

Run:

```powershell
Select-String -Path .codeperf\report\source-report.html -Pattern "app-shell","filter-panel","issue-workspace","detail-panel","quality-gate"
```

Expected: 每个关键类名至少出现一次。

### Task 6: 最终检查

**Files:**
- Modify: `codeperf-cli/src/main/java/com/codeperf/cli/report/SourceScanHtmlReportWriter.java`
- Create: `codeperf-cli/src/main/java/com/codeperf/cli/report/SourceScanHtmlAssets.java`
- Modify: `codeperf-cli/src/test/java/com/codeperf/cli/report/SourceScanHtmlReportWriterTest.java`

- [ ] **Step 1: 检查 Java 8 兼容**

Run:

```powershell
rg -n "readAllBytes|List\\.of|Map\\.of|var " codeperf-cli\src\main codeperf-cli\src\test
```

Expected: 不出现本次新增的 Java 9+ API。

- [ ] **Step 2: 检查工作区变更**

Run: `git status --short`

Expected: 只包含本次 UI 方案、实现和此前用户认可的相关变更；不回滚用户已有改动。
