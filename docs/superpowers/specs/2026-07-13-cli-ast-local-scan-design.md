# CodePerf CLI AST Local Scan Refactor Design

## 1. 背景

CodePerf 的项目目标已从“流水线中的通用性能检测编排器”收敛为“提前发现循环内外部 I/O 被数据量放大的代码结构风险”。

当前代码中的 CLI 已经偏向字节码扫描、任务创建、结果上传和 gate 查询。但结合公司实际约束后，这个方向需要调整：

- 流水线阶段不能执行 `codeperf`。
- 流水线阶段只能由使用方手工附加 `-javaagent` 参数。
- CLI 不应成为流水线门禁工具。
- 本地静态检测不应强依赖 `target/classes`。
- 静态检测需要更适合 Git 变更文件和源码位置报告。

因此，新一轮 CLI 重构目标是：

```text
CLI = 本地 AST 静态扫描 + 接入配置助手
Agent = 测试/预发运行时采集组件
Server = 动态证据接收与报告归档组件
```

本设计文档取代此前“CLI 作为 Git/CI 客户端”的产品定位，但不要求立即删除历史代码。实现阶段应先新增清晰边界，再逐步下线不符合新定位的命令。

## 2. 目标

第一阶段只解决一个核心问题：

```text
开发者改动的 Java 文件中，是否引入了循环内外部 I/O 调用风险。
```

CLI 必须做到：

- 只扫描 Git 变更 Java 文件，默认不全量扫描。
- 使用 Java 源码 AST，而不是字节码，作为静态检测主路径。
- 不要求先执行 Maven/Gradle 编译。
- 输出源码文件、行号、循环位置、调用点和调用链证据。
- 支持有限跨方法调用链分析。
- 生成 `.codeperf.yml` 和 `.codeperf/agent.yml` 模板。
- 给出手工配置 agent 的 JVM 参数示例。
- 可选安装 `pre-push` hook，用于本地提前提醒。

## 3. 非目标

第一阶段不做：

- CI 阶段静态扫描。
- CI gate。
- `codeperf ci-run`。
- `codeperf agent prepare` 作为流水线步骤。
- CLI 启动业务应用。
- CLI attach 到运行中 JVM。
- 生产环境 attach。
- 完整 APM。
- 全程序精准静态分析。
- 反射、动态代理、运行时 Bean 名称解析的精准调用链。

## 4. 用户真实接入流程

平台侧发布 CodePerf CLI。推荐长期形态是 npm CLI 外壳，但检测内核可以继续使用 Java 实现。

业务项目本地接入：

```bash
npm install -D @company/codeperf
npx codeperf init
npx codeperf scan
```

可选安装本地 hook：

```bash
npx codeperf install-hooks
```

开发者日常流程保持不变：

```bash
git add .
git commit -m "feat: xxx"
git push
```

测试/预发环境由使用方手工配置 agent。流水线或启动脚本只出现 `-javaagent`，不出现 `codeperf` 命令：

```bash
java -javaagent:/opt/codeperf/codeperf-agent.jar=/opt/app/.codeperf/agent.yml \
  -jar app.jar
```

## 5. CLI 命令设计

### 5.1 `codeperf init`

生成：

```text
.codeperf.yml
.codeperf/agent.yml
```

职责：

- 识别 Git 根目录。
- 识别常见 Java 源码目录。
- 生成默认静态扫描配置。
- 生成 agent 配置模板。
- 打印手工接入 agent 的 JVM 参数示例。

`init` 不应修改流水线配置，不应启动应用，不应访问生产环境。

### 5.2 `codeperf scan`

默认行为：

```text
扫描 Git 变更 Java 文件。
```

职责：

- 找到 Git 根目录。
- 读取 `.codeperf.yml`。
- 计算变更 Java 文件。
- 使用 Java AST 解析源码。
- 执行 `LoopIoAmplificationAstRule`。
- 输出控制台摘要和本地报告文件。
- 根据 `failOn` 返回退出码。

可选参数：

```bash
codeperf scan --all
codeperf scan --base origin/master --head HEAD
codeperf scan --format json
```

### 5.3 `codeperf doctor`

职责：

- 检查 Git 根目录是否可识别。
- 检查 `.codeperf.yml` 是否存在且可解析。
- 检查 `sourceRoots` 是否存在。
- 检查 Java 是否可用。
- 检查 `.codeperf/agent.yml` 是否存在。
- 检查配置中的 agent jar 路径是否存在。
- 检查 I/O 规则配置是否为空或明显冲突。

`doctor` 不执行静态扫描，不访问服务端。

### 5.4 `codeperf install-hooks`

可选能力。

职责：

- 安装或更新 `pre-push` hook。
- hook 内只调用 `codeperf scan`。
- 不安装强制不可绕过的本地门禁。

pre-push 定位是提前提醒，不是企业最终治理手段。

## 6. 配置设计

`.codeperf.yml` 以 Git 根目录为基准解析路径。CLI 从任意子目录执行时，都必须先向上找到 Git 根目录和配置文件。

单模块示例：

```yaml
project: order-service

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
  ioTypes:
    - mysql
    - mongodb
    - redis
    - gaussdb
    - http
    - rpc
    - sdk

agent:
  enabled: true
  serverUrl: http://codeperf.company.com
  configPath: .codeperf/agent.yml
  jarPath: /opt/codeperf/codeperf-agent.jar
  targetPackages:
    - com.company.order
```

多模块示例：

```yaml
project: trade-platform

modules:
  - name: order-service
    sourceRoots:
      - order-service/src/main/java
    targetPackages:
      - com.company.order

  - name: user-service
    sourceRoots:
      - user-service/src/main/java
    targetPackages:
      - com.company.user
```

## 7. AST 静态检测设计

### 7.1 技术选择

推荐使用：

```text
JavaParser + JavaParser Symbol Solver
```

原因：

- 能解析 Java 源码 AST。
- 能获取源码行号。
- 能遍历循环、lambda、方法调用。
- 能解析部分类型和方法声明。
- 适合本地 CLI，不需要编译产物。

第一阶段不要求完整 classpath 精准解析。Symbol Solver 能解析多少用多少，解析失败时降级到名称、字段类型、import 和配置规则匹配。

### 7.2 变更文件扫描

默认只扫描：

```text
git diff --name-only <baseRef> <headRef>
```

过滤条件：

- 只保留 `.java` 文件。
- 删除的文件跳过。
- 默认只扫描 `sourceRoots` 覆盖的文件。
- 测试源码由 `includeTests` 控制。

如果当前分支没有远端 base，CLI 应给出明确错误和修复建议，而不是静默全量扫描。

### 7.3 循环识别

必须识别：

- `for`
- enhanced `for`
- `while`
- `do while`
- `Iterable.forEach(lambda)`
- `stream.forEach(lambda)`

第一阶段不要求识别所有复杂流式算子链，但 `forEach` 是必须支持的，因为它是循环语义。

### 7.4 I/O 调用识别

第一阶段通过规则库判断外部 I/O：

- MySQL / GaussDB：`Mapper`、`Dao`、`Repository`、JDBC、MyBatis 典型方法名。
- MongoDB：`MongoTemplate`、`MongoRepository`。
- Redis：`RedisTemplate`、`StringRedisTemplate`、`RedissonClient`。
- HTTP：`RestTemplate`、`WebClient`、`OkHttpClient`、`HttpClient`、Feign。
- RPC / SDK：类名或包名包含 `Client`、`Gateway`、`Facade`、公司内部 SDK 前缀。

判断依据按置信度组合：

- 调用对象变量名。
- 字段声明类型。
- import 名称。
- 方法名前缀。
- 注解。
- Symbol Solver 解析到的类型。

报告必须区分置信度：

```text
HIGH: 类型或注解明确指向外部 I/O。
MEDIUM: 类名/字段名/方法名强匹配。
LOW: 仅弱命名规则匹配。
```

## 8. 跨方法调用链设计

第一阶段支持有限调用链，不做完整全程序分析。

支持：

- 当前类内方法调用追踪。
- 当前文件内方法调用追踪。
- 同模块内明确类型的普通类方法追踪。
- 最大深度默认 2，可配置到 3。
- 递归和重复路径保护。
- 输出调用链证据。

示例：

```java
for (Long userId : userIds) {
    loadUser(userId);
}

private User loadUser(Long userId) {
    return userMapper.selectById(userId);
}
```

报告示例：

```text
Loop I/O Amplification Risk

文件：OrderService.java
循环位置：line 35-37
调用链：
OrderService.buildReport:36
  -> OrderService.loadUser:82
  -> UserMapper.selectById

风险类型：DB
置信度：HIGH
```

暂不支持：

- 反射。
- 动态代理。
- 运行时 Bean 名称解析。
- 多实现接口精准选择。
- 跨 jar 第三方源码调用链。
- 无界深度调用图。

接口调用策略：

- 能找到唯一实现类时，可继续追踪。
- 找到多个实现类时，默认停止追踪或输出低置信度提示。
- 找不到实现类时，停止追踪。

## 9. 报告与退出码

本地扫描输出两类结果：

- 控制台摘要。
- JSON 或 HTML 本地报告。

报告字段：

- 文件路径。
- 循环起止行。
- 调用点行号。
- 调用链。
- I/O 类型。
- 置信度。
- 严重度。
- 修复建议。

退出码：

```text
0: 未达到 failOn 阈值。
1: 达到 failOn 阈值。
2: 配置错误、Git 错误、解析器初始化失败等工具错误。
```

源码解析失败不应默认导致退出码 2。单文件解析失败应输出 `PARSE_ERROR` 结果，并根据配置决定是否失败。

## 10. Agent 接入助手设计

CLI 不在流水线中执行，但 `init` 和 `doctor` 可以帮助准备 agent 接入材料。

`codeperf init` 生成 `.codeperf/agent.yml`：

```yaml
serverUrl: http://codeperf.company.com
analysisTaskId: ${CODEPERF_ANALYSIS_TASK_ID}
uploadEnabled: true
targetPackages:
  - com.company.order
entry:
  method: POST
  path: /api/orders/report
sampleMs: 10
mode: session
```

CLI 打印人工配置说明：

```text
请将以下 JVM 参数手工配置到测试/预发应用启动参数中：

-javaagent:/opt/codeperf/codeperf-agent.jar=/opt/app/.codeperf/agent.yml
```

该说明是接入辅助，不是自动修改流水线。

## 11. 迁移策略

当前已有字节码扫描、`ci-run`、`local-scan`、任务上传和 gate 查询等能力。新方案不要求一次性删除，但需要重新定位：

- 新增 AST 扫描作为默认 `scan` 主路径。
- 字节码扫描降级为 legacy 或 experimental。
- `ci-run` 不再作为推荐命令。
- README 主流程改为本地 AST scan + 手工 agent 配置。
- demo 验证不再依赖 `target/classes`。
- 后续确认无使用方后，再删除不符合新定位的命令。

## 12. 测试策略

单元测试：

- Git 变更文件过滤。
- JavaParser AST 循环识别。
- 直接循环内 Mapper/Redis/Mongo/HTTP 调用识别。
- `forEach` lambda 识别。
- 同类一层调用链识别。
- 最大深度和递归保护。
- 配置解析和路径基准解析。

集成测试：

- 在 `codeperf-demo` 中新增源码级风险样例。
- 从 demo 子目录执行 CLI，验证自动找到 Git 根和配置。
- 验证不需要 `mvn package` 也能完成 AST scan。
- 验证报告包含源码行号和调用链证据。

## 13. 风险与边界

主要风险：

- AST 静态分析存在误报和漏报。
- 多模块符号解析需要逐步增强。
- Spring 接口多实现无法完全精准判断。
- 公司内部 SDK 命名需要配置化维护。
- 如果只扫描变更文件，未变更文件中的历史风险不会被发现。

风险接受原则：

```text
第一阶段目标是提前发现新增循环 I/O 结构风险，不是证明线上一定变慢。
```

## 14. 已确认决策

- 静态检测第一阶段使用源码 AST，不以字节码为主。
- 默认只扫描 Git 变更 Java 文件。
- 支持有限跨方法调用链，默认最大深度 2。
- CLI 不在流水线阶段执行。
- 流水线阶段只能由使用方手工配置 agent。
- CLI 不提供 `ci-run` 作为新方案主路径。
- CLI 不 attach、不启动业务应用。
- CLI 主要命令收敛为 `init`、`scan`、`doctor`、可选 `install-hooks`。
- Agent 独立通过 `-javaagent` 接入测试/预发运行环境。
