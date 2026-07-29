# CodePerf Server Management Report Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `codeperf-server` 内提供 Thymeleaf 管理页，用于查看任务列表和单任务综合报告，把静态结构风险、动态运行证据和最终结论放到同一个可浏览页面中。

**Architecture:** 页面层直接依赖现有任务仓储和报告汇总逻辑，不新建独立前端工程。新增一个页面服务层专门组装列表页和详情页所需数据，控制器只负责路由与模型填充，模板负责展示和少量样式。静态和动态数据继续由现有任务模型承载，页面只做只读聚合。

**Tech Stack:** Spring Boot MVC, Thymeleaf, Bootstrap 5.1.3 CDN, JUnit 5, MockMvc, MyBatis-Plus, Java 8

---

### Task 1: 补齐页面渲染测试

**Files:**
- Modify: `codeperf-server/src/test/java/com/cmb/codeperf/server/controller/ReportPageControllerTest.java`

- [ ] **Step 1: 写失败测试**

```java
@Test
public void should_RenderReportList_When_TasksExist() throws Exception {
    String taskId = createTaskWithStaticAndDynamicEvidence();

    mvc.perform(get("/reports"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("CodePerf 管理系统")))
            .andExpect(content().string(containsString("order-service")))
            .andExpect(content().string(containsString(taskId)))
            .andExpect(content().string(containsString("查看报告")));
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -pl codeperf-server -Dtest=ReportPageControllerTest test -DskipTests=false`

Expected: `/reports` 与 `/reports/{taskId}` 404。

- [ ] **Step 3: 保持测试不变，继续实现**

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -pl codeperf-server -Dtest=ReportPageControllerTest test -DskipTests=false`

Expected: 页面返回 200，包含管理系统标题、任务项目名、任务 ID 和综合报告关键字。

### Task 2: 新增页面服务与视图模型

**Files:**
- Create: `codeperf-server/src/main/java/com/cmb/codeperf/server/model/vo/report/ReportListItemVO.java`
- Create: `codeperf-server/src/main/java/com/cmb/codeperf/server/model/vo/report/ReportListPageVO.java`
- Create: `codeperf-server/src/main/java/com/cmb/codeperf/server/model/vo/report/ReportDetailPageVO.java`
- Create: `codeperf-server/src/main/java/com/cmb/codeperf/server/model/vo/report/ReportFindingCardVO.java`
- Create: `codeperf-server/src/main/java/com/cmb/codeperf/server/model/vo/report/ReportDynamicEvidenceVO.java`
- Create: `codeperf-server/src/main/java/com/cmb/codeperf/server/model/vo/report/ReportPageService.java`

- [ ] **Step 1: 写页面聚合测试**

```java
@Test
public void should_BuildReportDetailPage_When_TaskHasStaticAndDynamicData() {
    ReportDetailPageVO page = service.getDetailPage(taskId);

    assertEquals("order-service", page.getProjectName());
    assertEquals(1, page.getFindingCards().size());
    assertEquals("LOOP_IO_AMPLIFICATION", page.getFindingCards().get(0).getRuleId());
    assertEquals("POST /api/orders/report", page.getDynamicEvidence().getEntryKey());
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -pl codeperf-server -Dtest=ReportPageServiceTest test -DskipTests=false`

Expected: 页面服务类不存在或返回空数据。

- [ ] **Step 3: 实现最小组装逻辑**

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -pl codeperf-server -Dtest=ReportPageServiceTest test -DskipTests=false`

Expected: 页面 VO 包含任务摘要、静态风险列表、动态证据和综合结论。

### Task 3: 扩展仓储接口以支持页面查询

**Files:**
- Modify: `codeperf-server/src/main/java/com/cmb/codeperf/server/service/repository/AnalysisTaskRepository.java`
- Modify: `codeperf-server/src/main/java/com/cmb/codeperf/server/service/repository/memory/InMemoryAnalysisTaskRepository.java`
- Modify: `codeperf-server/src/main/java/com/cmb/codeperf/server/service/repository/mysql/MybatisPlusAnalysisTaskRepository.java`

- [ ] **Step 1: 写失败测试**

```java
@Test
public void should_ListRecentTasks_When_RepositoryQueried() {
    List<AnalysisTaskBO> tasks = repository.listRecentTasks(20);
    assertFalse(tasks.isEmpty());
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -pl codeperf-server -Dtest=AnalysisTaskRepositoryTest test -DskipTests=false`

Expected: 新方法尚未实现。

- [ ] **Step 3: 增加任务列表、静态发现、动态证据读取能力**

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -pl codeperf-server -Dtest=AnalysisTaskRepositoryTest test -DskipTests=false`

Expected: 两种仓储实现都可返回最近任务和明细数据。

### Task 4: 新增 Thymeleaf 控制器与模板

**Files:**
- Create: `codeperf-server/src/main/java/com/cmb/codeperf/server/controller/ReportPageController.java`
- Create: `codeperf-server/src/main/resources/templates/reports/list.html`
- Create: `codeperf-server/src/main/resources/templates/reports/detail.html`
- Modify: `codeperf-server/pom.xml`

- [ ] **Step 1: 写页面路由测试**

```java
@Test
public void should_RenderReportDetail_When_TaskExists() throws Exception {
    String taskId = createTaskWithStaticAndDynamicEvidence();

    mvc.perform(get("/reports/" + taskId))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("综合检测报告")))
            .andExpect(content().string(containsString("OrderService.java")));
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -pl codeperf-server -Dtest=ReportPageControllerTest test -DskipTests=false`

Expected: 找不到页面控制器或模板。

- [ ] **Step 3: 加入 thymeleaf 依赖并实现控制器、模板**

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -pl codeperf-server -Dtest=ReportPageControllerTest test -DskipTests=false`

Expected: 页面渲染成功，包含 Bootstrap 风格结构与风险详情。

### Task 5: 全量回归与整理

**Files:**
- Modify: 受影响的页面服务、模板和测试文件

- [ ] **Step 1: 运行服务端回归测试**

Run: `mvn -pl codeperf-server test -DskipTests=false`

- [ ] **Step 2: 检查页面 HTML 输出是否包含核心字段**

Expected: 列表页有任务清单，详情页有静态风险、动态证据和综合结论。

- [ ] **Step 3: 清理无用代码和多余占位**

Expected: 没有空模板、空控制器方法或重复的页面组装逻辑。

- [ ] **Step 4: 提交实现**

```bash
git add codeperf-server docs/superpowers/plans/2026-07-29-server-management-report.md
git commit -m "feat(server): add thymeleaf report pages"
```
