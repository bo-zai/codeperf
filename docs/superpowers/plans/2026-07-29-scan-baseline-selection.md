# CodePerf 扫描基线选择实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 `codeperf scan` 按运行场景自动选择正确的 Git 基线，彻底移除对 `origin/master` 的固定依赖。

**Architecture:** 保留“按变更文件扫描”的核心策略，不改静态分析规则本身。新增一个轻量的基线解析层，按 `pre-push hook > upstream > 配置兜底 > 本地工作区` 的顺序决定本次 diff 的范围；`ScanCommand` 只消费解析结果，不再自己拼基线。`init` 和文档同步去掉 `origin/master` 的强假设，避免新项目继续复制旧默认值。

**Tech Stack:** Java 8, JUnit 5, Maven, Git CLI, SnakeYAML, JCommander.

---

### Task 1: 引入扫描基线解析层

**Files:**
- Create: `codeperf-cli/src/main/java/com/cmb/codeperf/cli/git/GitScanRange.java`
- Create: `codeperf-cli/src/main/java/com/cmb/codeperf/cli/git/GitScanRangeResolver.java`
- Create: `codeperf-cli/src/test/java/com/cmb/codeperf/cli/git/GitScanRangeResolverTest.java`

- [ ] **Step 1: 写失败测试**

覆盖四个场景，断言解析结果不再依赖 `origin/master`：

1. `CODEPERF_PUSH_OLD_SHA` / `CODEPERF_PUSH_NEW_SHA` 存在时，返回 `pre-push` 范围。
2. 没有 push 环境变量，但当前分支有 upstream 时，返回 `upstream` 范围，`headRef` 固定为 `HEAD`，`baseRef` 使用 `git merge-base HEAD <upstream>` 的结果。
3. 没有 upstream，但配置里显式写了 `baseRef/headRef` 时，返回 `config` 范围。
4. 上述都不可用时，回退到 `worktree` 范围，供本地临时检查使用。

建议断言样例：

```java
assertEquals("pre-push", range.getSource());
assertEquals("abc123", range.getBaseRef());
assertEquals("def456", range.getHeadRef());
```

- [ ] **Step 2: 运行测试确认失败**

运行：

```bash
mvn -pl codeperf-cli test -Dtest=GitScanRangeResolverTest
```

预期：测试失败，提示 resolver 还不存在或尚未接入真实 Git 基线逻辑。

- [ ] **Step 3: 实现最小代码**

实现 `GitScanRange` 作为只读值对象，包含：

- `baseRef`
- `headRef`
- `source`
- `diffMode`

实现 `GitScanRangeResolver`，只负责把“当前运行场景”翻译成上面四个字段，不直接碰扫描逻辑。

- [ ] **Step 4: 重新运行测试**

运行：

```bash
mvn -pl codeperf-cli test -Dtest=GitScanRangeResolverTest
```

预期：通过。

- [ ] **Step 5: 提交**

```bash
git add codeperf-cli/src/main/java/com/cmb/codeperf/cli/git/GitScanRange.java codeperf-cli/src/main/java/com/cmb/codeperf/cli/git/GitScanRangeResolver.java codeperf-cli/src/test/java/com/cmb/codeperf/cli/git/GitScanRangeResolverTest.java
git commit -m "feat(cli): resolve scan baseline by execution scenario"
```

### Task 2: 让 `scan` 消费解析后的基线

**Files:**
- Modify: `codeperf-cli/src/main/java/com/cmb/codeperf/cli/cmd/ScanCommand.java`
- Modify: `codeperf-cli/src/test/java/com/cmb/codeperf/cli/cmd/ScanCommandTest.java`

- [ ] **Step 1: 写失败测试**

补充或更新 `ScanCommandTest`，验证三件事：

1. pre-push 场景会使用 hook 传入的 `old/new` 范围。
2. 没有 hook 时，会优先使用 upstream。
3. 控制台输出会明确打印基线来源，避免用户以为工具仍然固定依赖某个主分支。

建议校验输出包含类似文本：

```text
[codeperf] 扫描基线=pre-push
[codeperf] 扫描基线=upstream
```

- [ ] **Step 2: 运行测试确认失败**

运行：

```bash
mvn -pl codeperf-cli test -Dtest=ScanCommandTest
```

预期：测试失败，说明 `ScanCommand` 还在直接依赖配置里的固定 `baseRef/headRef`。

- [ ] **Step 3: 实现最小修改**

在 `ScanCommand` 中引入 `GitScanRangeResolver`，把原来的：

- `resolveBaseRef(...)`
- `resolveHeadRef(...)`

替换为“先解析扫描基线，再统一传给 diff / blame / 输出逻辑”。

输出层增加一行明确说明：

```text
[codeperf] 扫描基线=upstream, 范围=merge-base..HEAD
```

这样用户能区分：

- 本次是 hook 精确推送范围
- 还是 upstream 兜底
- 还是本地工作区临时检查

- [ ] **Step 4: 重新运行测试**

运行：

```bash
mvn -pl codeperf-cli test -Dtest=ScanCommandTest
```

预期：通过。

- [ ] **Step 5: 提交**

```bash
git add codeperf-cli/src/main/java/com/cmb/codeperf/cli/cmd/ScanCommand.java codeperf-cli/src/test/java/com/cmb/codeperf/cli/cmd/ScanCommandTest.java
git commit -m "feat(cli): select scan diff range by runtime context"
```

### Task 3: 去掉新项目模板里的 `origin/master`

**Files:**
- Modify: `codeperf-cli/src/main/java/com/cmb/codeperf/cli/config/StaticScanConfig.java`
- Modify: `codeperf-cli/src/main/java/com/cmb/codeperf/cli/cmd/InitCommand.java`
- Modify: `codeperf-cli/src/test/java/com/cmb/codeperf/cli/config/CodePerfCliConfigTest.java`
- Modify: `codeperf-cli/src/test/java/com/cmb/codeperf/cli/cmd/InitCommandTest.java`
- Modify: `README.md`

- [ ] **Step 1: 写失败测试**

把测试改成不再接受“新初始化模板默认写死 `origin/master`”：

- `CodePerfCliConfigTest` 需要验证：新默认配置不再把 `baseRef` 解释成固定主分支假设。
- `InitCommandTest` 需要验证：生成的 `.codeperf.yml` 不再强制写入 `origin/master` 作为唯一默认值。

建议改成的行为是：

- 变更文件扫描仍然可用
- 模板不再暗示“所有仓库都以 `master` 作为主干”

- [ ] **Step 2: 运行测试确认失败**

运行：

```bash
mvn -pl codeperf-cli test -Dtest=InitCommandTest,CodePerfCliConfigTest
```

预期：测试失败，说明模板和默认值还停留在旧假设。

- [ ] **Step 3: 实现最小修改**

把 `StaticScanConfig.baseRef` 的默认值从 `origin/master` 改为更中性的值，并同步调整 `InitCommand` 的模板输出。

建议策略：

- 新模板不主动写死主分支名
- 说明文字改为“仅作为兜底配置，优先使用 hook / upstream”
- README 示例同步改掉 `origin/master`

这样可以避免新仓库把旧假设继续扩散出去。

- [ ] **Step 4: 重新运行测试**

运行：

```bash
mvn -pl codeperf-cli test -Dtest=InitCommandTest,CodePerfCliConfigTest
```

预期：通过。

- [ ] **Step 5: 提交**

```bash
git add codeperf-cli/src/main/java/com/cmb/codeperf/cli/config/StaticScanConfig.java codeperf-cli/src/main/java/com/cmb/codeperf/cli/cmd/InitCommand.java codeperf-cli/src/test/java/com/cmb/codeperf/cli/config/CodePerfCliConfigTest.java codeperf-cli/src/test/java/com/cmb/codeperf/cli/cmd/InitCommandTest.java README.md
git commit -m "docs(cli): remove master-specific scan defaults"
```

### Task 4: 回归验证与场景确认

**Files:**
- Modify: `codeperf-cli/src/test/java/com/cmb/codeperf/cli/cmd/ScanCommandTest.java`
- Modify: `codeperf-cli/src/test/java/com/cmb/codeperf/cli/git/GitDiffResolverTest.java`

- [ ] **Step 1: 补一组回归测试**

覆盖两个容易出错的点：

1. `scan` 只扫描本次变更文件，不会退化成全仓库扫描。
2. 当 `git diff` 基线来自 hook / upstream / worktree 时，行为一致、输出可解释。

- [ ] **Step 2: 运行完整 CLI 测试**

运行：

```bash
mvn -pl codeperf-cli test
```

预期：全部通过。

- [ ] **Step 3: 在真实仓库手工验证**

在一个没有 `master` 主分支的仓库里执行：

```bash
codeperf scan
```

确认输出中：

- 不再出现“默认 master 基线”的误导性信息
- 基线来源清晰
- 扫描结果仍然只覆盖变更范围

- [ ] **Step 4: 提交**

```bash
git add codeperf-cli/src/test/java/com/cmb/codeperf/cli/cmd/ScanCommandTest.java codeperf-cli/src/test/java/com/cmb/codeperf/cli/git/GitDiffResolverTest.java
git commit -m "test(cli): cover baseline resolution regression cases"
```

## Coverage Check

- `pre-push` 精确范围：Task 1, Task 2
- upstream 兜底：Task 1, Task 2
- 配置兜底：Task 1, Task 3
- 本地工作区检查：Task 1, Task 4
- 去掉 `origin/master` 模板假设：Task 3
- 变更文件扫描不退化：Task 2, Task 4

