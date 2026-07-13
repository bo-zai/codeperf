# CodePerf

CodePerf 用于提前发现“循环内外部 I/O 被数据量放大”的代码结构风险。典型事故是开发在原有接口中新增循环，循环体内调用 DB、Redis、Mongo、HTTP/RPC 或 SDK，测试环境数据量小未暴露，测试/预发或生产规模放大后接口响应明显变慢。

## 当前职责

- `codeperf-cli`：开发者本地工具，基于 Java 源码 AST 扫描 Git 变更 Java 文件，并辅助生成 agent 接入配置模板。
- `codeperf-agent`：由使用方手工通过 `-javaagent` 配置到测试/预发应用启动参数中，负责运行时打桩和证据上报。
- `codeperf-server`：接收 agent 动态证据并归档报告数据。

CLI 不在流水线阶段执行，不负责 CI gate，不 attach，不启动业务应用。

## 本地 CLI 使用

第一阶段推荐使用源码 AST 扫描，不要求业务项目先编译。当前本地验证先构建 Java CLI，再通过 `npm link` 暴露 `codeperf` 命令：

```bash
mvn -pl codeperf-cli package
cd codeperf-npm
npm link
cd ..
codeperf doctor
codeperf scan --all
```

真实业务项目接入时，开发者只需要在已完成本地 link 或后续内网 npm 安装后执行：

```bash
codeperf init
codeperf scan
```

如果暂时不使用 npm link，也可以直接执行 jar：

```bash
java -jar codeperf-cli/target/codeperf-cli.jar scan --all
```

从 demo 子目录验证同一个根配置：

```bash
cd codeperf-demo
codeperf scan --all
```

可选安装本地 pre-push 提醒：

```bash
codeperf install-hooks
```

## 配置示例

`.codeperf.yml` 位于 Git 根目录，所有路径都以 Git 根目录为基准解析。

```yaml
project: codeperf-demo

staticScan:
  enabled: true
  mode: changed
  sourceRoots:
    - codeperf-demo/src/main/java
  includeTests: false
  baseRef: origin/master
  headRef: HEAD
  failOn: WARN
  callChain:
    enabled: true
    maxDepth: 2

agent:
  enabled: true
  serverUrl: http://127.0.0.1:9090
  configPath: .codeperf/agent.yml
  jarPath: /opt/codeperf/codeperf-agent.jar
  targetPackages:
    - com.codeperf.demo
```

## Agent 手工接入

流水线或测试/预发启动脚本中只配置 agent，不执行 `codeperf`：

```bash
java -javaagent:/opt/codeperf/codeperf-agent.jar=/opt/app/.codeperf/agent.yml \
  -jar app.jar
```

`.codeperf/agent.yml` 示例：

```yaml
serverUrl: http://127.0.0.1:9090
analysisTaskId: ${CODEPERF_ANALYSIS_TASK_ID}
uploadEnabled: true
targetPackages:
  - com.codeperf.demo
entry:
  method: POST
  path: /api/orders/report
sampleMs: 10
mode: session
```

正式方案不使用运行时 attach；动态检测只作为测试/预发运行证据增强，不代表生产实测。

## Server 数据库配置

`codeperf-server` 提供 `local` 和 `dev` 两个 profile，均使用 MySQL + MyBatis-Plus 存储。

```bash
java -jar codeperf-server/target/codeperf-server.jar --spring.profiles.active=local
java -jar codeperf-server/target/codeperf-server.jar --spring.profiles.active=dev
```

连接信息通过环境变量注入：

```bash
CODEPERF_LOCAL_DB_URL=jdbc:mysql://127.0.0.1:3306/codeperf?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
CODEPERF_LOCAL_DB_USERNAME=root
CODEPERF_LOCAL_DB_PASSWORD=***

CODEPERF_DEV_DB_URL=jdbc:mysql://dev-host:3306/codeperf?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
CODEPERF_DEV_DB_USERNAME=codeperf
CODEPERF_DEV_DB_PASSWORD=***
```
