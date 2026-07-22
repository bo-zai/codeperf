# CodePerf

CodePerf 是一个面向上线前评审的 Java 性能风险检测工具，当前聚焦一类明确事故：开发在已有接口中引入循环，循环体内调用 DB、Redis、MongoDB、HTTP/RPC 或外部 SDK，测试环境数据量较小未暴露问题，生产规模放大后接口响应变慢。

当前方案以本地源码 AST 静态扫描为主，测试/预发 `-javaagent` 动态证据为辅。CLI 不 attach 生产 JVM，不启动业务应用，不作为 CI gate 或任务编排工具。

## 模块职责

| 模块 | 当前职责 |
|---|---|
| `codeperf-cli` | 本地命令行工具，读取 Git 根目录 `.codeperf.yml`，基于 Java 源码 AST 扫描变更文件或全部源码，输出本地 JSON 报告，并可按需把静态报告上传到 CodePerf Server。 |
| `codeperf-npm` | 本地 npm wrapper，通过 `npm link` 暴露 `codeperf` 命令，实际转发到 `codeperf-cli/target/codeperf-cli.jar`。 |
| `codeperf-agent` | 测试/预发应用启动时通过 `-javaagent` 加载，读取运行环境提供的 agent 配置，负责打桩、采集运行证据和可选上报。 |
| `codeperf-server` | 接收任务、静态结果和动态证据，提供 gate/report 查询接口；当前支持 local/dev profile 与 MySQL/MyBatis-Plus 存储。 |
| `codeperf-demo-admin` | 多模块演示项目的后台管理模块，用于验证模块级扫描、报告聚合和循环内 DB/SDK 风险。 |
| `codeperf-demo-app` | 多模块演示项目的用户端模块，用于验证应用侧循环内 DB/SDK 风险。 |
| `codeperf-demo-common` | 多模块演示项目的公共模块，用于验证 common 层公共服务中的循环内 I/O 风险。 |

## 本地安装

当前阶段推荐在仓库内先构建 Java CLI，再通过 `npm link` 模拟真实 `codeperf` 命令。

```bash
mvn -pl codeperf-cli package
cd codeperf-npm
npm link
cd ..
```

验证命令是否可用：

```bash
codeperf doctor
```

如果暂时不使用 npm link，也可以直接执行 jar：

```bash
java -jar codeperf-cli/target/codeperf-cli.jar doctor
```

## CLI 使用

`codeperf` 当前只提供四个命令：

| 命令 | 说明 |
|---|---|
| `codeperf init` | 在 Git 根目录生成 `.codeperf.yml`。已存在的文件不会被覆盖。 |
| `codeperf doctor` | 检查 `.codeperf.yml` 和配置的源码目录是否有效。 |
| `codeperf scan` | 扫描 Git 变更的 Java 源文件，默认输出 `.codeperf/report/source-report.json`。 |
| `codeperf scan --all` | 扫描 `.codeperf.yml` 中配置的全部源码目录。 |
| `codeperf scan --upload` | 扫描后上传静态报告到 CodePerf Server；上传是显式行为，默认不启用。 |
| `codeperf install-hooks` | 安装本地 pre-push 提醒；如果 `.git/hooks/pre-push` 已存在，不会覆盖，只输出需要手工合并的片段。 |

真实业务项目接入的最小流程：

```bash
codeperf init
codeperf doctor
codeperf scan
```

本仓库 demo 验证：

```bash
codeperf scan --all
```

当前根配置使用 `codeperf-demo-admin`、`codeperf-demo-app`、`codeperf-demo-common` 三个模块作为演示项目，因此 `scan --all` 会输出类似结果：

```text
[codeperf] 扫描文件=8，风险总数=5，阻断风险=5，结果=失败，解析错误=0
[codeperf] 门禁=失败，阻断=5，新增=0，修改=0，历史=0，未归因=0
[codeperf] 模块 codeperf-demo-admin：风险=2
[codeperf] 模块 codeperf-demo-app：风险=2
[codeperf] 模块 codeperf-demo-common：风险=1
```

在当前根配置 `failOn: WARN` 下，检测到 WARN 及以上风险时命令返回退出码 `1`；扫描执行异常返回 `2`。

如果需要把本次静态扫描结果沉淀到服务端，用显式上传方式执行：

```bash
codeperf scan --upload
codeperf scan --all --upload
```

上传前仍会先生成本地 JSON 报告。上传内容是扫描结果 JSON、项目名、环境、Git commit 和分支信息，不上传 Java 源码正文；报告中会包含相对源码路径、行号、规则类型和证据信息，便于评审定位。

## 配置文件

`.codeperf.yml` 必须位于 Git 根目录。CLI 可以从子目录执行，但配置路径和源码路径都按 Git 根目录解析。

`codeperf init` 会自动生成 `.codeperf.yml`。生成逻辑会优先从 `remote.origin.url` 截取项目名，解析失败时回退到 Git 根目录文件夹名；同时会自动发现 `src/main/java` 源码目录，单模块项目生成一个 `sourceRoots`，多模块项目生成所有模块的 `sourceRoots` 和 `modules` 配置。真实项目接入后通常只需要确认服务端地址和 Git 基准分支。

单模块项目初始化后的典型默认配置：

```yaml
project: mall

staticScan:
  enabled: true
  mode: changed
  sourceRoots:
    - src/main/java
  includeTests: false
  baseRef: origin/master
  headRef: HEAD
  failOn: WARN
  callChain:
    enabled: true
    maxDepth: 2

report:
  local:
    enabled: true
    path: .codeperf/report/source-report.json
  upload:
    enabled: false
    serverUrl: http://codeperf.company.com
```

多模块项目会自动初始化为类似下面的结构：

```yaml
project: mall

staticScan:
  enabled: true
  mode: changed
  sourceRoots:
    - mall-admin/src/main/java
    - mall-common/src/main/java
    - mall-demo/src/main/java
    - mall-mbg/src/main/java
    - mall-portal/src/main/java
    - mall-search/src/main/java
    - mall-security/src/main/java
  includeTests: false
  baseRef: origin/master
  headRef: HEAD
  failOn: WARN
  callChain:
    enabled: true
    maxDepth: 2
  ioTypes:
    - mysql
    - mongodb
    - redis
    - gaussdb
    - http
    - rpc
    - sdk

modules:
  - name: mall-admin
    sourceRoots:
      - mall-admin/src/main/java
  - name: mall-common
    sourceRoots:
      - mall-common/src/main/java
  - name: mall-demo
    sourceRoots:
      - mall-demo/src/main/java
  - name: mall-mbg
    sourceRoots:
      - mall-mbg/src/main/java
  - name: mall-portal
    sourceRoots:
      - mall-portal/src/main/java
  - name: mall-search
    sourceRoots:
      - mall-search/src/main/java
  - name: mall-security
    sourceRoots:
      - mall-security/src/main/java

report:
  local:
    enabled: true
    path: .codeperf/report/source-report.json
  upload:
    enabled: false
    serverUrl: http://codeperf.company.com
```

`mode: changed` 使用 Git diff 选择变更 Java 文件；`scan --all` 会忽略 changed 模式，直接扫描全部 `sourceRoots`。

`.codeperf.yml` 字段说明：

| 配置项 | 自动生成 | 是否通常需要手改 | 作用 |
|---|---:|---:|---|
| `project` | 是 | 通常不用 | 项目标识。`codeperf init` 优先从 Git `remote.origin.url` 截取项目名，例如 `git@github.com:macrozheng/mall.git` 会生成 `mall`；解析失败时回退到 Git 根目录文件夹名。只有希望报告展示业务别名时才需要手改。 |
| `staticScan.enabled` | 是 | 通常不用 | 静态扫描开关。当前 CLI 的 `scan` 命令会读取静态扫描配置，保留该字段用于后续统一启停。 |
| `staticScan.mode` | 是 | 通常不用 | 默认扫描模式。`changed` 表示普通 `codeperf scan` 扫描 Git 变更 Java 文件；`codeperf scan --all` 会扫描全部 `sourceRoots`。 |
| `staticScan.sourceRoots` | 是 | 通常不用 | Java 源码根目录。`codeperf init` 会自动发现单模块或多模块下的 `src/main/java`；只有特殊目录结构才需要手工调整。 |
| `staticScan.includeTests` | 是 | 通常不用 | 是否扫描测试代码的意图配置。当前实际扫描范围主要由 `sourceRoots` 决定，不建议第一阶段扫描 `src/test/java`。 |
| `staticScan.baseRef` | 是 | 视分支模型调整 | Git 变更扫描基准分支。默认 `origin/master`；如果公司主干是 `origin/main`、`origin/develop` 或其他分支，需要手工修改。 |
| `staticScan.headRef` | 是 | 通常不用 | Git 变更扫描目标引用。默认 `HEAD`，表示当前提交/工作区对应的头引用。 |
| `staticScan.failOn` | 是 | 视门禁策略调整 | 失败阈值。默认 `WARN`，发现 WARN 及以上风险时 CLI 返回 `1`；如果只想先观察报告，可后续改成更宽松策略。 |
| `staticScan.callChain.enabled` | 是 | 通常不用 | 是否启用同类方法调用链追踪。开启后可识别“循环里调用本类方法，本类方法内部访问 DB/Redis/HTTP”的风险。 |
| `staticScan.callChain.maxDepth` | 是 | 谨慎调整 | 调用链最大追踪深度。默认 `2`，适合第一阶段；调大可能发现更多间接风险，也可能增加误报和扫描成本。 |
| `staticScan.ioTypes` | 否，部分示例包含 | 暂不建议依赖 | 计划中的 I/O 类型配置项。当前主要识别逻辑仍在内置 matcher 中，后续会演进为可配置规则。 |
| `modules` | 是 | 通常不用 | 多模块源码映射。`codeperf init` 会根据发现的 `*/src/main/java` 自动生成；如果仓库有非标准模块路径，可以手工调整。 |
| `report.local.enabled` | 是 | 通常不用 | 是否生成本地静态扫描报告。默认开启；企业接入初期建议保留，便于离线排查和复现。 |
| `report.local.path` | 是 | 通常不用 | 本地静态扫描报告路径。默认 `.codeperf/report/source-report.json`，也可通过 `codeperf scan --output <path>` 临时覆盖。 |
| `report.upload.enabled` | 是 | 视团队策略调整 | 是否默认上传静态扫描结果。默认关闭；建议先使用 `codeperf scan --upload` 显式验证，再考虑开启。 |
| `report.upload.serverUrl` | 是 | 是 | 静态报告上传的 CodePerf Server 地址。企业环境应改成内部服务地址。 |

Agent 运行配置不由 `codeperf init` 生成，也不由 `codeperf doctor` 检查。它属于测试/预发运行环境配置，应该由部署脚本、配置中心、Kubernetes ConfigMap、镜像环境变量或运维标准路径管理。

## 静态扫描能力

当前 CLI 的主路径是源码 AST 扫描，不要求先编译业务项目。核心规则为 `LoopIoAmplificationAstRule`，识别循环体内的外部 I/O 调用。

已覆盖的第一阶段 I/O 类型包括：

| 类型 | 典型识别对象 |
|---|---|
| DB | `Mapper`、`Repository`、`DAO` 等接收者，且方法名类似 `select`、`query`、`find`、`get`、`list`、`insert`、`update`、`delete`。 |
| Redis | `RedisTemplate`、`StringRedisTemplate`、`RedissonClient` 等。 |
| MongoDB | `MongoTemplate`、`MongoRepository` 等。 |
| HTTP | `RestTemplate`、`WebClient`、`OkHttpClient`、`HttpClient`、`Feign` 等。 |
| SDK | `Client`、`Gateway`、`Facade` 等外部客户端风格调用。 |

规则支持有限的同类方法调用链追踪，默认 `maxDepth: 2`。这不是全程序静态分析，也不承诺覆盖跨服务、反射、复杂依赖注入或动态代理下的所有调用。

## Agent 安装脚本接入

动态检测只作为测试/预发运行证据增强，不代表生产实测。企业接入推荐由 `install.sh` 在 `docker build` 前自动完成接入，业务方不需要手工改 Dockerfile，也不需要自己拼 agent 版本、采样参数和目标包名。

```bash
CODEPERF_INSTALL_CONFIG_URL=http://codeperf-server:9095/api/agent/install-config
CODEPERF_ENV=dev
./install.sh
```

脚本会先采集 Git 构建身份，再调用服务端的 `POST /api/agent/install-config` 获取安装配置。接口请求体包含：

| 字段 | 说明 |
|---|---|
| `project` | 项目标识，通常由 `codeperf init` 生成。 |
| `remoteUrl` | Git 仓库地址。 |
| `commit` | 当前提交 SHA。 |
| `branch` | 当前分支。 |
| `env` | 运行环境，如 `dev`。 |
| `authorName` | 最近一次提交作者。 |
| `authorEmail` | 最近一次提交邮箱。 |
| `commitTime` | 最近一次提交时间。 |
| `commitMessage` | 最近一次提交信息。 |

接口响应体包含：

| 字段 | 说明 |
|---|---|
| `enabled` | 是否启用安装。 |
| `serverUrl` | Agent 上报地址。 |
| `agentUrl` | `codeperf-agent.jar` 下载地址。 |
| `agentSha256` | 可选校验值。 |
| `appName` | 应用名。 |
| `env` | 环境名。 |
| `targetPackages` | 业务包前缀列表。 |
| `entry` | 入口方法与路径。 |
| `slowSqlMs` | 慢 SQL 阈值。 |
| `sampleMs` | 采样间隔。 |
| `mode` | 采集模式。 |

安装脚本生成的 `agent.yml` 示例：

```yaml
serverUrl: http://127.0.0.1:9095
uploadEnabled: true
appName: music-education-app
env: dev
buildInfoPath: /opt/codeperf/build-info.properties
targetPackages:
  - com.codeperf.demo
entry:
  method: POST
  path: /api/orders/report
sampleMs: 10
mode: session
output: build/codeperf/perf-data.raw
```

Agent 支持 `config=/path/to/agent.yml` 或直接传入 yml/yaml 文件路径。没有配置文件时仍保留分号参数解析作为本地调试兼容，但推荐使用 YAML。

脚本最小环境变量：

```bash
CODEPERF_INSTALL_CONFIG_URL=http://codeperf-server:9095/api/agent/install-config
CODEPERF_ENV=dev
```

如需鉴权，可额外提供 `CODEPERF_ACCESS_TOKEN`。如 Dockerfile 不在仓库根目录，可提供 `CODEPERF_DOCKERFILE=./deploy/Dockerfile`。

## Server

`codeperf-server` 默认端口为 `9095`，默认激活 `local` profile。`local` 与 `dev` profile 都使用 MySQL + MyBatis-Plus；测试中可通过属性覆盖为内存仓储。

启动前先创建数据库表：

```bash
mysql -u <user> -p < codeperf-server/src/main/resources/schema.sql
```

启动 local：

```bash
set SPRING_PROFILES_ACTIVE=local
set CODEPERF_LOCAL_DB_URL=jdbc:mysql://127.0.0.1:3306/codeperf?useUnicode=true^&characterEncoding=utf8^&useSSL=false^&serverTimezone=Asia/Shanghai
java -jar codeperf-server/target/codeperf-server.jar
```

启动 dev：

```bash
set SPRING_PROFILES_ACTIVE=dev
set CODEPERF_DEV_DB_URL=jdbc:mysql://127.0.0.1:3306/codeperf?useUnicode=true^&characterEncoding=utf8^&useSSL=false^&serverTimezone=Asia/Shanghai
set CODEPERF_DEV_DB_USERNAME=codeperf
set CODEPERF_DEV_DB_PASSWORD=***
java -jar codeperf-server/target/codeperf-server.jar
```

当前 Server API：

| API | 说明 |
|---|---|
| `POST /api/tasks` | 创建分析任务，返回 `analysisTaskId`。 |
| `GET /api/tasks/{taskId}` | 查询任务详情。 |
| `POST /api/tasks/{taskId}/static-results` | 上传静态扫描 JSON。 |
| `POST /api/tasks/{taskId}/dynamic-evidence` | 上传动态证据 JSON。 |
| `GET /api/tasks/{taskId}/gate` | 查询任务状态和风险级别。 |
| `GET /api/tasks/{taskId}/report` | 查询静态/动态证据是否存在以及最终风险级别。 |
| `POST /api/agent/install-config` | 为安装脚本返回 Agent 安装配置。 |

CLI 当前支持通过 `codeperf scan --upload` 上传静态扫描结果：先调用 `POST /api/tasks` 创建分析任务，再调用 `POST /api/tasks/{taskId}/static-results` 上传本地静态报告 JSON。动态证据仍由 agent 在测试/预发运行时上报。

## 构建与测试

全量构建：

```bash
mvn package
```

CLI 单模块测试：

```bash
mvn -pl codeperf-cli test
```

npm wrapper 测试：

```bash
npm --prefix codeperf-npm test
```

## 当前边界

- CLI 是本地开发工具，不负责 CI gate 或启动业务应用；静态结果上传是可选增强能力，默认关闭。
- 官方动态检测不使用运行时 attach。
- 静态扫描主路径是源码 AST；旧 bytecode 分析代码仍在模块内，但不是当前 README 推荐入口。
- 生产规模画像、完整报告合并、gate 查询命令和企业 npm 私服发布仍需后续评审与实现。
- 当前重点是提前发现“循环内外部 I/O 被数据量放大”的结构性风险，不把 CPU、内存、慢 SQL 作为第一阶段主目标。
