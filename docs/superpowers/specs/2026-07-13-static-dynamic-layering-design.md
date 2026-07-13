# CodePerf Static/Dynamic Capability Layering Design

## 1. Goal

CodePerf will be refactored by capability, not by Maven modules in the first phase.

The new architecture separates:

- Static detection: the primary risk discovery engine.
- Dynamic detection: pre-production runtime evidence collection.
- Server analysis: task coordination, evidence merge, reporting, and gate decisions.
- CLI workflow: Git/CI client for task creation, static scanning, upload, and gate waiting.

The first phase focuses on preventing loop I/O amplification incidents: code introduces a loop, the loop calls external I/O, test data is small, and production data amplifies latency.

## 2. Non-Goals

The first phase will not:

- Use attach as part of the official detection flow.
- Run agent in production as a required detection mechanism.
- Build a full APM system.
- Cover all performance issues as first-class goals.
- Split Maven modules before the capability boundaries are implemented and reviewed.

The existing attach flow may remain temporarily as local lab/debug compatibility, but it must not be part of the enterprise workflow.

## 3. Capability Boundaries

### 3.1 Static Detection

Static detection is the primary engine.

First-phase rule:

```text
LoopIoAmplificationRule
```

It detects methods that contain loops where the loop body calls external I/O:

- DB: Repository, DAO, Mapper, JDBC, ORM access.
- HTTP: Feign, RestTemplate, WebClient, OkHttp, HttpClient, RestClient.
- RPC: Dubbo, gRPC, Thrift, company RPC clients.
- SDK/Gateway: company service SDKs and gateway/facade clients.

Static detection must output:

- Source file path.
- Source line number.
- Class and method.
- Loop range when available.
- External call owner and method.
- I/O type.
- Static confidence.
- Suggested remediation.

Source location strategy:

- Read line numbers from bytecode `LineNumberTable` when available.
- Map class name to source path using default source roots.
- Allow explicit `--source-root` override.
- Default source root inference should include Maven-style roots such as `src/main/java`.

Static rules must use an internal extension interface so later rules can be added without redesigning the scanner. First-phase implementation should keep rule registration internal and simple, while allowing YAML configuration for I/O class/method patterns.

### 3.2 Dynamic Detection

Dynamic detection is evidence, not the primary discovery mechanism.

The dynamic flow must not use attach. The agent is loaded with `-javaagent` when the pre-production application starts.

The agent is responsible only for:

- Instrumentation.
- Runtime evidence collection.
- HTTP upload to CodePerf Server.

The agent must not:

- Create final reports.
- Decide final gate result.
- Attach to an already running JVM.
- Require CLI to start the target application.

Agent startup form:

```text
-javaagent:/opt/codeperf-agent.jar=config=/etc/codeperf/agent.yml
```

Detailed configuration lives in `agent.yml`, rendered by CI or the deployment platform from a template.

Example configuration shape:

```yaml
server:
  url: "http://codeperf.company"
  token: "${CODEPERF_TOKEN}"

task:
  id: "${CODEPERF_TASK_ID}"
  app: "order-service"
  env: "preprod"

capture:
  mode: "entry"
  entries:
    - method: "POST"
      path: "/api/orders/report"
  targetPackages:
    - "com.company.order"
  maxRequests: 20

upload:
  batchSize: 10
  flushIntervalMs: 5000
```

First-phase dynamic evidence should stay narrow:

- Entry identity.
- Request duration.
- Whether a static risk method executed.
- External I/O type.
- External call count.
- Total external call duration.
- Error count.

It should not attempt full APM tracing in the first phase.

### 3.3 CodePerf Server

CodePerf Server is the coordination and reporting center.

Responsibilities:

- Create `analysis_task_id`.
- Receive static scan results from CLI/CI.
- Receive dynamic evidence from agent.
- Store task data in MySQL.
- Store core fields structurally and raw evidence in JSON columns.
- Merge static risks, dynamic evidence, and production scale profile.
- Generate one unified report.
- Return final gate decision to CI.

Storage choice:

```text
Primary database: MySQL
Optional Redis: not required in phase one
```

Static, dynamic, and profile data are associated by `analysis_task_id`.

### 3.4 CLI

The CLI becomes the Git/CI client.

It no longer controls target JVM runtime through attach.

Responsibilities:

- Create analysis tasks on CodePerf Server.
- Run static `scan` and `scan-diff`.
- Upload static results.
- Help validate or render agent config when useful.
- Wait for Server gate result.
- Return CI-compatible exit codes.

Suggested command areas:

```text
task create
task status
scan
scan-diff
scan upload
agent config validate
agent print-args
gate wait
```

Git workflow positioning:

- `pre-commit`: optional fast source/diff warning only.
- `pre-push`: compile first, then bytecode scan-diff.
- CI: create task, compile, scan-diff, upload, deploy pre-production app with agent, run tests, wait for Server gate.

The CLI must not start the target application. CI or the deployment/test platform owns application startup and traffic triggering.

## 4. End-to-End Flow

Enterprise CI flow:

```text
1. CI asks CodePerf Server to create an analysis task.
2. Server returns analysis_task_id.
3. CI compiles the application.
4. CLI runs static scan-diff on compiled classes.
5. CLI uploads static findings with analysis_task_id.
6. CI renders agent.yml from template.
7. CI deploys pre-production application with -javaagent=config=agent.yml.
8. CI/test platform triggers interface automation or smoke tests.
9. Agent collects narrow dynamic evidence and uploads it to Server.
10. Server merges static risks, dynamic evidence, and production scale profile.
11. Server generates one report with sections for static risk, dynamic evidence, production scale inference, and final gate result.
12. CLI gate wait polls Server and exits with the configured gate status.
```

## 5. Report Model

The report is a single unified report, not separate static and dynamic reports.

Required sections:

- Static structural risks.
- Dynamic pre-production evidence.
- Production scale inference.
- Final gate conclusion.
- Remediation suggestions.

The report must distinguish:

| Section | Meaning |
|---|---|
| Static structural risk | Code contains loop I/O amplification shape |
| Dynamic evidence | Pre-production runtime executed the risk path |
| Production scale inference | Production P95/P99 scale may amplify the risk |
| Final gate conclusion | Server-calculated pass/fail result |

Dynamic evidence reuses static source locations by matching class, method, and external call identity.

## 6. Production Scale Profile

Production scale profile is required for production-risk inference.

It contains only aggregated, desensitized metrics:

- P50/P95/P99 loop input size.
- Max known input size.
- Entry-level or business-object-level scale.
- Source and generation time of the profile.

It must not include production business details or raw rows.

If profile data is missing, the report must not claim safety. It should report that production amplification cannot be fully evaluated.

## 7. Design Decisions Locked

- First phase uses capability layering, not Maven module split.
- Static detection is primary.
- Dynamic detection is auxiliary evidence.
- First phase focuses only on loop I/O amplification.
- Static rules need extension interfaces for later rules.
- Rule implementation is internal interface based, with limited YAML pattern configuration.
- Dynamic detection uses `-javaagent`, not attach.
- Agent details are configured through `agent.yml`.
- CI renders the agent config from templates.
- CodePerf Server receives both static and dynamic data.
- Static scan runs in CLI/CI, not on Server.
- Server uses MySQL as primary storage.
- Static and dynamic evidence are linked by `analysis_task_id`.
- Server returns final gate decision.
- CLI is the Git/CI client and does not start the target application.

## 8. Open Implementation Notes

The implementation plan should decide exact class names and schema details, but must preserve the boundaries in this document.

The first implementation increment should prioritize:

- Static finding source locations.
- `LoopIoAmplificationRule`.
- Static rule extension interface.
- CLI task and scan upload flow.
- Server task/result receiving API.
- Agent config model and HTTP upload contract.

