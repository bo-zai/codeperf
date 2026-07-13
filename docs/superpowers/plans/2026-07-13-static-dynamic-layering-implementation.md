# Static/Dynamic Capability Layering Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor CodePerf into a capability-layered Git/CI detection system where static loop I/O risk discovery is primary, dynamic `-javaagent` evidence is auxiliary, and CodePerf Server owns report/gate decisions.

**Architecture:** Keep existing `codeperf-cli` and `codeperf-agent` modules, add a new `codeperf-server` delivery component, and avoid splitting static/dynamic Maven modules in this phase. CLI becomes the Git/CI client; agent is loaded by `-javaagent=config=...`; Server stores task/evidence/report data in MySQL and returns gate decisions.

**Tech Stack:** Java 8, Maven multi-module, ASM, ByteBuddy, JCommander, Jackson, Spring Boot 2.7.x for `codeperf-server`, MySQL, JUnit 4.

---

## File Structure

- Modify `pom.xml`: add `codeperf-server` module and shared dependency versions for Spring Boot web/JDBC/MySQL driver if needed.
- Modify `codeperf-cli`: keep existing `scan` as compatibility, add task/gate/upload/config-oriented commands, and add static source location support.
- Modify `codeperf-agent`: keep premain path, remove official reliance on attach flow, add `agent.yml` parsing and HTTP upload client.
- Create `codeperf-server`: Spring Boot app exposing task creation, static upload, dynamic upload, gate status, and report query APIs.
- Add tests under each module using the module's existing test layout. If test directories are absent, create `src/test/java` in the touched module.
- Keep `AttachCommand` and `AttachHelper` present as local debug compatibility in this phase, but remove them from the official flow and help text where new enterprise commands are documented.

## Task 1: Static Finding Source Locations

**Files:**
- Modify: `codeperf-cli/src/main/java/com/codeperf/analysis/staticanalysis/ClassAnalysis.java`
- Modify: `codeperf-cli/src/main/java/com/codeperf/analysis/staticanalysis/BytecodeAnalyzer.java`
- Modify: `codeperf-cli/src/main/java/com/codeperf/analysis/staticanalysis/StaticFinding.java`
- Modify: `codeperf-cli/src/main/java/com/codeperf/analysis/staticanalysis/StaticResult.java`
- Test: `codeperf-cli/src/test/java/com/codeperf/analysis/staticanalysis/BytecodeAnalyzerTest.java`

- [ ] **Step 1: Add a failing test for bytecode line capture**

Create a fixture class inside the test source with a method containing a loop and a call on a known source line. Compile it through the test build, read its `.class` bytes, call `BytecodeAnalyzer.analyze(bytes)`, and assert the resulting `CallSite` has a positive line number.

Run: `mvn -pl codeperf-cli -Dtest=BytecodeAnalyzerTest test`

Expected: FAIL because `CallSite` currently has no line number property.

- [ ] **Step 2: Extend analysis models with source location fields**

Add line fields to `ClassAnalysis.CallSite` and loop range line metadata to `MethodAnalysis`. Add source-oriented fields to `StaticFinding`: `sourceFile`, `lineNumber`, `loopStartLine`, `loopEndLine`, `className`, `methodName`, `callOwner`, `callName`.

Keep existing constructor compatibility by adding an overloaded constructor that populates old fields and leaves location fields empty or zero.

- [ ] **Step 3: Read `LineNumberTable` in `BytecodeAnalyzer`**

In `BytecodeAnalyzer.InsnCollector`, implement `visitLineNumber(int line, Label start)` and track the current source line for subsequent instructions. When adding a `CallSite` or allocation, copy the current line number into the model. When recording loop ranges from bytecode instruction ranges, preserve the best available start/end line from the tracked instruction-to-line map.

- [ ] **Step 4: Verify source line capture**

Run: `mvn -pl codeperf-cli -Dtest=BytecodeAnalyzerTest test`

Expected: PASS and the assertion confirms a positive line number for the loop-body call.

- [ ] **Step 5: Commit**

```bash
git add codeperf-cli/src/main/java/com/codeperf/analysis/staticanalysis codeperf-cli/src/test/java/com/codeperf/analysis/staticanalysis/BytecodeAnalyzerTest.java
git commit -m "feat: capture static finding source locations"
```

## Task 2: Static Rule Extension Interface

**Files:**
- Create: `codeperf-cli/src/main/java/com/codeperf/analysis/staticanalysis/rule/StaticRule.java`
- Create: `codeperf-cli/src/main/java/com/codeperf/analysis/staticanalysis/rule/StaticRuleContext.java`
- Create: `codeperf-cli/src/main/java/com/codeperf/analysis/staticanalysis/rule/StaticRuleRegistry.java`
- Modify: `codeperf-cli/src/main/java/com/codeperf/analysis/staticanalysis/StaticScanner.java`
- Test: `codeperf-cli/src/test/java/com/codeperf/analysis/staticanalysis/rule/StaticRuleRegistryTest.java`

- [ ] **Step 1: Add a failing registry test**

Write a test that creates a `StaticRuleRegistry.defaultRegistry()`, asserts it contains a rule named `loop-io-amplification`, and runs the registry against an empty class list without throwing.

Run: `mvn -pl codeperf-cli -Dtest=StaticRuleRegistryTest test`

Expected: FAIL because the registry does not exist.

- [ ] **Step 2: Add the static rule interface**

Define `StaticRule` with methods:

```java
String id();
String displayName();
List<StaticFinding> analyze(StaticRuleContext context);
```

Define `StaticRuleContext` with immutable fields:

```java
List<ClassAnalysis> classes;
String targetPackage;
List<String> sourceRoots;
StaticRuleConfig config;
```

If `StaticRuleConfig` is not introduced in this task, create it as an empty value object in the same package so subsequent tasks can add pattern fields without changing the interface.

- [ ] **Step 3: Add registry and scanner integration**

Define `StaticRuleRegistry` with `defaultRegistry()` and `rules()` methods. Update `StaticScanner` to call registry rules through `StaticRuleContext`. Keep existing old `BytecodeRule` implementations compiling during the transition by adapting them through thin wrappers or by migrating only the loop I/O rule in Task 3.

- [ ] **Step 4: Verify registry behavior**

Run: `mvn -pl codeperf-cli -Dtest=StaticRuleRegistryTest test`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add codeperf-cli/src/main/java/com/codeperf/analysis/staticanalysis codeperf-cli/src/test/java/com/codeperf/analysis/staticanalysis/rule/StaticRuleRegistryTest.java
git commit -m "feat: add static rule extension interface"
```

## Task 3: Loop I/O Amplification Rule

**Files:**
- Create: `codeperf-cli/src/main/java/com/codeperf/analysis/staticanalysis/rules/LoopIoAmplificationRule.java`
- Modify: `codeperf-cli/src/main/java/com/codeperf/analysis/staticanalysis/rules/NPlusOneSuspect.java`
- Modify: `codeperf-cli/src/main/java/com/codeperf/analysis/staticanalysis/rule/StaticRuleRegistry.java`
- Test: `codeperf-cli/src/test/java/com/codeperf/analysis/staticanalysis/rules/LoopIoAmplificationRuleTest.java`

- [ ] **Step 1: Add failing tests for loop I/O categories**

Create tests with compiled fixture classes covering:

- Loop calling `UserRepository.findById`.
- Loop calling `InventoryClient.queryStock`.
- Loop calling `RestTemplate.getForObject`.
- Non-loop call to the same clients.

Assert only loop-body calls produce findings and that findings include `ioType`, `callOwner`, `callName`, and source line.

Run: `mvn -pl codeperf-cli -Dtest=LoopIoAmplificationRuleTest test`

Expected: FAIL because the new rule does not exist.

- [ ] **Step 2: Implement `LoopIoAmplificationRule`**

Use the existing `NPlusOneSuspect` classification logic as the seed, but change the rule identity and output:

```text
id: loop-io-amplification
display name: 循环内外部 I/O 放大风险
finding type: Loop I/O Amplification
```

Classify I/O types:

- DB for Repository, DAO, Dao, Mapper, JdbcTemplate.
- HTTP for Feign, RestTemplate, WebClient, OkHttpClient, HttpClient, RestClient.
- RPC for Dubbo, Rpc, Grpc, Thrift.
- SDK for Sdk, Gateway, Facade, Client patterns that are not already HTTP/RPC.
- SERVICE for cross-package Service as LOW confidence.

- [ ] **Step 3: Preserve old N+1 behavior as compatibility**

Keep `NPlusOneSuspect` compiling, but make the default registry use `LoopIoAmplificationRule` for the first-phase static flow. Existing static report generation can still display old findings if explicitly invoked by old code paths, but new scanner output should use the new rule identity.

- [ ] **Step 4: Verify loop I/O tests**

Run: `mvn -pl codeperf-cli -Dtest=LoopIoAmplificationRuleTest test`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add codeperf-cli/src/main/java/com/codeperf/analysis/staticanalysis codeperf-cli/src/test/java/com/codeperf/analysis/staticanalysis/rules/LoopIoAmplificationRuleTest.java
git commit -m "feat: detect loop io amplification statically"
```

## Task 4: Source Root and Scan-Diff CLI Flow

**Files:**
- Modify: `codeperf-cli/src/main/java/com/codeperf/cli/Main.java`
- Modify: `codeperf-cli/src/main/java/com/codeperf/cli/cmd/ScanCommand.java`
- Create: `codeperf-cli/src/main/java/com/codeperf/cli/cmd/ScanDiffCommand.java`
- Create: `codeperf-cli/src/main/java/com/codeperf/cli/git/GitDiffResolver.java`
- Test: `codeperf-cli/src/test/java/com/codeperf/cli/git/GitDiffResolverTest.java`

- [ ] **Step 1: Add failing tests for git diff file resolution**

Test that `GitDiffResolver` parses changed `.java` paths from `git diff --name-only base head` output and ignores non-Java files.

Run: `mvn -pl codeperf-cli -Dtest=GitDiffResolverTest test`

Expected: FAIL because `GitDiffResolver` does not exist.

- [ ] **Step 2: Add `--source-root` to `scan`**

Update `ScanCommand` to accept repeated or comma-separated `--source-root` values. Default to Maven roots when omitted:

```text
src/main/java
src/test/java
```

Pass source roots into `StaticScanner`.

- [ ] **Step 3: Add `scan-diff` command**

Register `scan-diff` in `Main`. Parameters:

```text
--base
--head
--target-package
--classes-dir
--source-root
--output
--server
--task-id
--upload
```

First implementation filters findings to classes whose source files are touched by the diff. If a class-to-source mapping cannot be resolved, include the finding and mark it with a warning field in the JSON evidence.

- [ ] **Step 4: Verify CLI compile and git resolver**

Run: `mvn -pl codeperf-cli test`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add codeperf-cli/src/main/java/com/codeperf/cli codeperf-cli/src/test/java/com/codeperf/cli
git commit -m "feat: add scan diff cli flow"
```

## Task 5: Server Module and MySQL Schema

**Files:**
- Modify: `pom.xml`
- Create: `codeperf-server/pom.xml`
- Create: `codeperf-server/src/main/java/com/codeperf/server/CodePerfServerApplication.java`
- Create: `codeperf-server/src/main/resources/application.yml`
- Create: `codeperf-server/src/main/resources/schema.sql`
- Test: `codeperf-server/src/test/java/com/codeperf/server/CodePerfServerApplicationTest.java`

- [ ] **Step 1: Add failing server context test**

Create a Spring Boot context test that starts the server application with an in-memory test profile or mocked datasource configuration.

Run: `mvn -pl codeperf-server test`

Expected: FAIL because `codeperf-server` is not yet a module.

- [ ] **Step 2: Add `codeperf-server` Maven module**

Add module to root `pom.xml`. Create `codeperf-server/pom.xml` with dependencies:

- `spring-boot-starter-web`
- `spring-boot-starter-jdbc`
- MySQL connector runtime dependency
- `jackson-databind`
- test starter if compatible with current parent dependency management

- [ ] **Step 3: Add initial schema**

Create tables:

```text
analysis_task
static_result
dynamic_evidence
production_profile
analysis_report
```

Use MySQL JSON columns for raw static findings, dynamic evidence payload, profile snapshot, and rendered report details. Include indexed columns for `task_id`, `project`, `commit_sha`, `branch`, `status`, `risk_level`, and timestamps.

- [ ] **Step 4: Verify server module**

Run: `mvn -pl codeperf-server test`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add pom.xml codeperf-server
git commit -m "feat: add codeperf server module"
```

## Task 6: Server Task and Upload APIs

**Files:**
- Create: `codeperf-server/src/main/java/com/codeperf/server/api/TaskController.java`
- Create: `codeperf-server/src/main/java/com/codeperf/server/api/StaticResultController.java`
- Create: `codeperf-server/src/main/java/com/codeperf/server/api/DynamicEvidenceController.java`
- Create: `codeperf-server/src/main/java/com/codeperf/server/api/GateController.java`
- Create: `codeperf-server/src/main/java/com/codeperf/server/service/AnalysisTaskService.java`
- Test: `codeperf-server/src/test/java/com/codeperf/server/api/TaskApiTest.java`

- [ ] **Step 1: Add failing API tests**

Using Spring MVC test support, verify:

- `POST /api/tasks` returns a generated `analysisTaskId`.
- `POST /api/tasks/{taskId}/static-results` stores static JSON.
- `POST /api/tasks/{taskId}/dynamic-evidence` stores dynamic JSON.
- `GET /api/tasks/{taskId}/gate` returns task status and risk level.

Run: `mvn -pl codeperf-server -Dtest=TaskApiTest test`

Expected: FAIL because controllers do not exist.

- [ ] **Step 2: Implement controllers and service**

Use request/response DTOs scoped to the server module. Store raw payloads in JSON columns as strings serialized by Jackson. Initial gate result can be derived from static risk level only until merge/report logic is implemented in Task 9.

- [ ] **Step 3: Verify API tests**

Run: `mvn -pl codeperf-server -Dtest=TaskApiTest test`

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add codeperf-server/src/main/java/com/codeperf/server codeperf-server/src/test/java/com/codeperf/server/api/TaskApiTest.java
git commit -m "feat: add server task upload apis"
```

## Task 7: CLI Server Integration

**Files:**
- Create: `codeperf-cli/src/main/java/com/codeperf/cli/http/CodePerfServerClient.java`
- Create: `codeperf-cli/src/main/java/com/codeperf/cli/cmd/TaskCommand.java`
- Create: `codeperf-cli/src/main/java/com/codeperf/cli/cmd/GateCommand.java`
- Modify: `codeperf-cli/src/main/java/com/codeperf/cli/Main.java`
- Test: `codeperf-cli/src/test/java/com/codeperf/cli/http/CodePerfServerClientTest.java`

- [ ] **Step 1: Add failing HTTP client tests**

Test request construction for:

- Creating a task.
- Uploading static JSON.
- Waiting for gate response.

Use a lightweight local fake HTTP server in the test or mock `HttpURLConnection` through a small transport abstraction.

Run: `mvn -pl codeperf-cli -Dtest=CodePerfServerClientTest test`

Expected: FAIL because the client does not exist.

- [ ] **Step 2: Implement `CodePerfServerClient`**

Use Java 8-compatible HTTP APIs. Do not introduce a heavy HTTP client dependency in phase one. Support JSON request/response via Jackson.

- [ ] **Step 3: Add `task` and `gate` CLI commands**

Add:

```text
task create --server --project --commit --branch --env
task status --server --task-id
gate wait --server --task-id --fail-on --timeout
```

`gate wait` polls Server until a terminal state or timeout, then returns a CI-compatible exit code.

- [ ] **Step 4: Wire `scan-diff --upload`**

When `--upload` is present, require `--server` and `--task-id`, then upload the scan result JSON to Server after local scan completes.

- [ ] **Step 5: Verify CLI integration tests**

Run: `mvn -pl codeperf-cli test`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add codeperf-cli/src/main/java/com/codeperf/cli codeperf-cli/src/test/java/com/codeperf/cli
git commit -m "feat: integrate cli with codeperf server"
```

## Task 8: Agent Config and HTTP Upload

**Files:**
- Create: `codeperf-agent/src/main/java/com/codeperf/agent/config/AgentYamlConfig.java`
- Modify: `codeperf-agent/src/main/java/com/codeperf/agent/config/AgentConfig.java`
- Create: `codeperf-agent/src/main/java/com/codeperf/agent/upload/DynamicEvidenceUploader.java`
- Modify: `codeperf-agent/src/main/java/com/codeperf/agent/AgentBootstrap.java`
- Test: `codeperf-agent/src/test/java/com/codeperf/agent/config/AgentConfigTest.java`

- [ ] **Step 1: Add failing config parsing tests**

Test parsing an agent argument:

```text
config=/tmp/codeperf-agent.yml
```

and a YAML file containing `server`, `task`, `capture`, and `upload` sections. Assert `task.id`, `server.url`, entries, target packages, and upload interval are loaded.

Run: `mvn -pl codeperf-agent -Dtest=AgentConfigTest test`

Expected: FAIL because YAML config parsing does not exist.

- [ ] **Step 2: Add YAML parser dependency**

Add SnakeYAML to `codeperf-agent/pom.xml` and shade it with the agent jar to avoid target application classpath conflicts.

- [ ] **Step 3: Implement `AgentYamlConfig` and merge into `AgentConfig`**

Support environment variable substitution for `${CODEPERF_TASK_ID}` and `${CODEPERF_TOKEN}`. Keep existing semicolon argument parsing only for local debug compatibility.

- [ ] **Step 4: Add dynamic evidence uploader**

Implement HTTP POST to:

```text
POST /api/tasks/{taskId}/dynamic-evidence
```

The uploader sends narrow evidence JSON and retries transient failures with bounded retry count. Failed upload must not break target business execution.

- [ ] **Step 5: Verify agent config tests**

Run: `mvn -pl codeperf-agent test`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add codeperf-agent/pom.xml codeperf-agent/src/main/java/com/codeperf/agent codeperf-agent/src/test/java/com/codeperf/agent
git commit -m "feat: configure agent with yaml and http upload"
```

## Task 9: Dynamic External Call Evidence

**Files:**
- Modify: `codeperf-agent/src/main/java/com/codeperf/agent/collect/InstrumentationInstaller.java`
- Create: `codeperf-agent/src/main/java/com/codeperf/agent/collect/ExternalCallRecord.java`
- Modify: `codeperf-agent/src/main/java/com/codeperf/agent/collect/Recorder.java`
- Create: `codeperf-agent/src/main/java/com/codeperf/agent/collect/advice/ExternalHttpAdvice.java`
- Test: `codeperf-agent/src/test/java/com/codeperf/agent/collect/RecorderExternalCallTest.java`

- [ ] **Step 1: Add failing recorder tests for external call aggregation**

Call a new recorder API directly in tests:

```text
startRequest
recordExternalCallStart
recordExternalCallFinish
finishRequest
```

Assert evidence contains external call type, owner, method, count, total duration, and errors.

Run: `mvn -pl codeperf-agent -Dtest=RecorderExternalCallTest test`

Expected: FAIL because external call evidence does not exist.

- [ ] **Step 2: Add external call aggregation model**

Create `ExternalCallRecord` and add it to request/session evidence. Aggregate by `ioType + owner + method`.

- [ ] **Step 3: Add first-phase HTTP client instrumentation**

Instrument the most common HTTP clients already named in the design:

- RestTemplate methods: `getForObject`, `getForEntity`, `postForObject`, `postForEntity`, `exchange`.
- WebClient entry methods may be recorded as static evidence only in phase one if reactive runtime interception becomes too broad.
- Feign interfaces are recorded by matching configured class patterns when method calls enter configured target packages.

Keep this narrow. The phase-one target is evidence, not full distributed tracing.

- [ ] **Step 4: Upload dynamic evidence at request finish or batch flush**

At request finish, enqueue evidence for upload. Use configured `batchSize` and `flushIntervalMs`. Upload failure must be logged and swallowed.

- [ ] **Step 5: Verify agent tests**

Run: `mvn -pl codeperf-agent test`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add codeperf-agent/src/main/java/com/codeperf/agent codeperf-agent/src/test/java/com/codeperf/agent
git commit -m "feat: collect dynamic external call evidence"
```

## Task 10: Server Merge, Report, and Gate Result

**Files:**
- Create: `codeperf-server/src/main/java/com/codeperf/server/service/ReportService.java`
- Create: `codeperf-server/src/main/java/com/codeperf/server/service/GateDecisionService.java`
- Create: `codeperf-server/src/main/java/com/codeperf/server/api/ReportController.java`
- Modify: `codeperf-server/src/main/java/com/codeperf/server/api/GateController.java`
- Test: `codeperf-server/src/test/java/com/codeperf/server/service/GateDecisionServiceTest.java`

- [ ] **Step 1: Add failing gate decision tests**

Create tests for:

- HIGH confidence static loop I/O with missing profile returns WARN and message says production scale cannot be fully evaluated.
- HIGH confidence static loop I/O with production P95 above configured threshold returns CRITICAL.
- Dynamic evidence matching static location enriches the report but does not create source location by itself.

Run: `mvn -pl codeperf-server -Dtest=GateDecisionServiceTest test`

Expected: FAIL because decision service does not exist.

- [ ] **Step 2: Implement merge model**

Merge static finding and dynamic evidence by:

```text
className
methodName
callOwner
callName
ioType
```

The static finding owns source location. Dynamic evidence adds counts, duration, and error data.

- [ ] **Step 3: Implement report sections**

Generate JSON report with sections:

- staticStructuralRisks
- dynamicEvidence
- productionScaleInference
- finalGateConclusion
- remediationSuggestions

HTML rendering can remain basic in this phase; JSON report correctness is the test target.

- [ ] **Step 4: Verify server report tests**

Run: `mvn -pl codeperf-server test`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add codeperf-server/src/main/java/com/codeperf/server codeperf-server/src/test/java/com/codeperf/server
git commit -m "feat: merge evidence and calculate gate result"
```

## Task 11: Documentation and Official Flow Update

**Files:**
- Modify: `README.md`
- Create: `docs/06-static-dynamic-layering.md`
- Modify: `docs/03-cli.md`
- Modify: `docs/02-agent-core.md`
- Test: documentation review by exact command outputs listed below

- [ ] **Step 1: Update README positioning**

Describe CodePerf as a Git/CI loop I/O amplification risk detector. The README must say official dynamic flow uses `-javaagent`, not attach.

- [ ] **Step 2: Add architecture doc**

Create `docs/06-static-dynamic-layering.md` summarizing:

- CLI responsibilities.
- Static detection responsibilities.
- Agent responsibilities.
- Server responsibilities.
- MySQL storage.
- End-to-end CI flow.

- [ ] **Step 3: Mark attach as local debug compatibility**

In `docs/03-cli.md`, keep attach documentation only under a local lab/debug section. State it is not part of the enterprise flow.

- [ ] **Step 4: Update agent doc**

In `docs/02-agent-core.md`, document `-javaagent=config=agent.yml`, HTTP upload, and agent-as-evidence boundary.

- [ ] **Step 5: Verify docs mention required concepts**

Run:

```bash
rg "attach|javaagent|analysis_task_id|scan-diff|LoopIoAmplificationRule|MySQL" README.md docs
```

Expected: output contains the new official flow terms and marks attach as non-official local debug flow.

- [ ] **Step 6: Commit**

```bash
git add README.md docs/02-agent-core.md docs/03-cli.md docs/06-static-dynamic-layering.md
git commit -m "docs: document static dynamic enterprise flow"
```

## Task 12: End-to-End Verification Script

**Files:**
- Create: `scripts/run-layered-demo.sh`
- Test: manual dry run and Maven verification

- [ ] **Step 1: Add script skeleton**

Create a script that demonstrates the intended flow with local components:

```text
mvn package
start codeperf-server locally
task create
scan-diff upload
print or render agent config
start demo with -javaagent=config=...
curl demo endpoint
gate wait
```

The script should use environment variables for MySQL connection and skip Server startup if `CODEPERF_SERVER_URL` is already provided.

- [ ] **Step 2: Add dry-run mode**

Support:

```bash
bash scripts/run-layered-demo.sh --dry-run
```

Dry run prints commands without starting services.

- [ ] **Step 3: Verify full build**

Run:

```bash
mvn test
```

Expected: PASS for all modules.

- [ ] **Step 4: Verify dry-run script**

Run:

```bash
bash scripts/run-layered-demo.sh --dry-run
```

Expected: prints task create, scan-diff upload, javaagent config, demo request, and gate wait commands.

- [ ] **Step 5: Commit**

```bash
git add scripts/run-layered-demo.sh
git commit -m "chore: add layered flow demo script"
```

## Final Verification

- [ ] Run all tests:

```bash
mvn test
```

Expected: all modules pass.

- [ ] Verify enterprise terms are present:

```bash
rg "LoopIoAmplificationRule|analysis_task_id|scan-diff|agent.yml|javaagent|MySQL" README.md docs codeperf-cli codeperf-agent codeperf-server
```

Expected: each term appears in the expected module or docs.

- [ ] Verify official flow avoids attach:

```bash
rg "attach" README.md docs codeperf-cli/src/main/java
```

Expected: attach remains only in local debug compatibility text or legacy command implementation, not in the official enterprise flow.

- [ ] Check git status:

```bash
git status --short
```

Expected: only intentional files are modified or the working tree is clean after commits.
