
### 代码风格

- Java 使用 4 空格缩进、`PascalCase` 类名、`camelCase` 方法名与变量名
- 包名全小写，多级包名用点分隔（如 `com.codeperf.agent`）
- 目录与文件命名遵循现有模式：类名与文件名一致（如 `AgentBootstrap.java`）
- 常量使用 `UPPER_SNAKE_CASE`，定义在类顶部

### Maven 构建规范

项目使用 Maven 构建，目标版本为 Java 8，提交前必须遵守：

| 规则 | 正确示例 | 错误示例 |
|---|---|---|
| 单一语句一行 | `String name = user.getName();` | `String name=user.getName();` |
| 大括号不省略 | `if (count > 0) { ... }` | `if (count > 0) ...` |
| 左大括号同行 | `public void run() {` | `public void run()\n{` |
| 运算符两侧空格 | `i + 1` | `i+1` |
| 关键字后空格 | `if (condition)` | `if(condition)` |
| 逗号后空格 | `run(cfg, sampler, writer)` | `run(cfg,sampler,writer)` |

**导入组织：**

- 包声明在第一行
- 导入按包层级分组，IDE 自动管理导入顺序
- 避免使用通配符导入（`import java.util.*`），使用具体类导入
- 导入语句后空一行，再开始类定义

**通用：**

- 无尾部空白字符
- 禁止在代码中包含私钥或敏感信息
- JSON/YAML/XML 格式校验

### 业界规范概述

遵循以下业界主流规范的基本原则（详见对应手册）：

- **阿里巴巴 Java 开发手册**：命名规约、常量定义、异常处理、并发处理、集合处理等
- **Google Java Style Guide**：源文件结构、格式化、命名、注释等
- **Effective Java（Joshua Bloch）**：对象创建与销毁、方法设计、异常处理、并发等最佳实践

#### 异常处理

- 不捕获顶层 `Exception`/`Throwable`，捕获具体异常类型
- 异常必须有处理逻辑，禁止空 `catch` 块
- 业务异常使用自定义异常类，系统异常使用标准异常
- 异常信息包含上下文（如参数值、操作名称）

#### 日志规范

- 使用日志框架（SLF4J + Logback/Log4j2），禁止 `System.out.println`
- 日志级别规范：ERROR（错误）、WARN（警告）、INFO（关键业务）、DEBUG（调试）
- 使用参数化日志：`log.info("user {} login", userId)`，避免字符串拼接
- 异常日志打印堆栈：`log.error("msg", e)`，禁止仅打印 `e.getMessage()`

#### 并发与线程安全

- 线程池通过 `ThreadPoolExecutor` 创建，禁止 `Executors` 静态方法（避免 OOM）
- 共享变量使用 `volatile`、`AtomicXxx` 或锁保护
- 锁粒度最小化，避免嵌套锁（防止死锁）
- 禁止在循环中创建线程

#### 集合与 Stream API

- 集合初始化指定容量：`new ArrayList<>(expectedSize)`，避免扩容开销
- 禁止在 `foreach` 中修改集合（使用迭代器或副本）
- `Stream` 操作保持可读性，复杂逻辑拆分多步或用传统循环

#### 空值处理

- 返回集合时返回空集合而非 `null`
- Optional 用于方法返回值，不用于字段、参数
- 参数校验放在方法入口，使用 `Objects.requireNonNull` 或自定义校验

#### 资源管理

- 使用 try-with-resources 管理可关闭资源（`InputStream`、`Connection` 等）
- 数据库连接、文件句柄及时关闭，防止泄漏

#### POJO 设计

- POJO 类（DTO/VO/DO）使用 Lombok 注解减少样板代码：
  - `@Data`：生成 getter/setter、equals/hashCode、toString、requiredArgsConstructor
  - `@Getter/@Setter`：单独控制字段可见性
  - `@AllArgsConstructor/@NoArgsConstructor`：构造方法
  - `@Builder`：Builder 模式（字段多时）
  - `@Slf4j`：日志注入
- 禁止公有字段，必须通过注解生成 getter/setter
- 不使用 Lombok 的类需手动实现 equals/hashCode/toString
- **实体类（Entity）每个字段必须添加 JavaDoc 注释**，说明字段用途
  - 示例：`/** 用户邮箱 */`
  - 复杂字段需补充取值说明：`/** 用户状态：ACTIVE-激活，INACTIVE-停用 */`

#### 常量与枚举

- 常量定义在类顶部或常量类，使用 `static final`
- 状态、类型等固定值使用枚举，禁止用常量整数或字符串
- 枚举实现接口可扩展行为

#### API 设计

- 接口方法签名简洁，参数不超过 7 个
- 公有 API 添加 `@deprecated` 标记而非直接删除
- 方法单一职责，避免"上帝方法"

### 数据库设计规范

- **禁止定义外键约束**，表间关联关系通过注释声明
  - 原因：外键会引入表级锁，高并发场景下严重影响性能；跨库场景无法使用外键；DDL 变更时外键增加复杂度
- **所有字段必须添加 `COMMENT`**，说明字段用途或取值含义
- **关联关系通过注释声明**，格式：`逻辑关联 表名.字段名`
  - 示例：`repository_id BIGINT NOT NULL COMMENT '代码仓库ID，逻辑关联code_repository.id'`
- **日期时间字段统一使用 `DATETIME` 类型**
  - 原因：`TIMESTAMP` 存在 2038 年问题；时区转换可能导致数据不一致；`DATETIME` 范围更广（0001-9999 年）
  - 示例：`created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'`

### Sonar 规范

- 控制方法圈复杂度，普通方法建议不超过 `15`
- 控制方法参数数量，普通方法建议不超过 `7`
- 对重复出现的错误文案、状态文案、字段描述文案，优先提取为类级常量或小型 helper，避免散落的重复字面量
- 避免空 `catch` 块，至少打印日志或添加注释说明忽略原因

### 文件大小与类拆分规范

#### 文件大小控制

| 规则 | 标准 |
|------|------|
| 单文件行数上限 | Java 文件建议不超过 **800 行**（含注释和空行） |
| 纯代码行数上限 | 实际逻辑代码不超过 **500-600 行** |
| 单文件职责上限 | 一个文件应只包含 **一个核心类或一组紧密相关的辅助类** |

#### 类拆分时机

当出现以下信号时，应主动拆分而非继续追加：

- 文件行数接近上限（超过 600 行时预警）
- 类包含多个松耦合的内部类或方法组
- 类承担多种职责（如既做数据解析又做 API 调用）
- 新增功能与现有代码职责边界清晰可拆分

#### 拆分原则

按职责拆分：

- 数据模型 → `domain/` 或 `model/`
- 业务逻辑 → `service/` 或 `manager/`
- API 处理 → `controller/` 或 `handler/`
- 工具方法 → `util/` 或 `helper/`

拆分示例：

```text
// 拆分前：UserManager.java（1200行，混杂多种职责）
class UserManager {         // 用户 CRUD
  class UserValidator {...} // 用户校验
  class UserRepository {...} // 数据访问
  void sendEmail() {...}    // 邮件发送
  void formatData() {...}   // 数据格式化
}

// 拆分后：
user/
├── UserManager.java     // 核心业务（~200行）
├── UserValidator.java   // 校验逻辑（~150行）
├── UserRepository.java  // 数据访问（~180行）
├── UserEmailService.java // 邮件服务（~100行）
└── UserFormatter.java   // 数据格式化（~80行）
```

### 代码注释规范

#### 基本要求

- **语言要求**：所有注释必须使用简体中文，包括 JavaDoc、行内注释、TODO/FIXME 标记

#### 注释时机（以下情况必须添加注释）

- **类级**：类定义应有 JavaDoc，说明类的用途、关键属性、使用示例（如适用）
- **方法级**：公共方法必须有 JavaDoc，说明功能、参数含义（`@param`）、返回值（`@return`）、可能的异常（`@throws`）
- **行内注释**：
  - 复杂算法或非直观逻辑的实现步骤
  - 业务规则或业务逻辑的决策依据
  - 边界条件处理、异常捕获的原因
  - 性能优化相关的技术决策
  - 临时方案或已知限制（配合 TODO/FIXME）
  - 正则表达式、复杂公式等难以直接理解的代码片段

#### 注释内容要求

- 注释应解释 **WHY（为什么这样做）**，而非简单重复 **WHAT（代码做什么）**
- 注释应提供代码本身无法表达的信息：设计意图、约束原因、相关背景
- 可引用相关设计文档（如 `见 docs/02-agent-core.md 第 2 节`）
- 避免无意义注释（如 `// 获取用户名称` 对应 `user.getName()`）

#### JavaDoc 格式示例

```java
/**
 * 装配采集核心：解析参数 → 建采样器/落盘器 → 初始化 Recorder → 启动采样 → 织入插桩。
 * premain 与 agentmain 都委托到此处，共用同一套核心。
 * 见 docs/02-agent-core.md 第 2 节。
 */
public static synchronized void start(String args, Instrumentation inst) {
    ...
}
```

#### 注释维护

- **同步更新原则**：修改代码时，相关注释必须同步更新，确保注释与代码一致
- 删除代码时，相关注释一并删除，不要保留过时的注释
- 发现注释与代码不一致时，优先检查代码正确性，然后修正注释

#### 禁止事项

- 不要保留注释掉的代码块，使用版本控制管理历史代码
- 不要添加已废弃或不再生效的注释
- 不要在注释中包含敏感信息（密码、密钥、内部 IP 等）
- 不要用注释掩盖代码质量问题（应直接修复代码）
- **不要在注释中出现"中文注释"、"以下是注释"等无意义的标签式文字**
- 不要添加显而易见的注释（如 `// 定义变量 x` 对应 `int x = 1;`）
- 不要在注释中重复方法名或变量名

#### 特殊标记

- **TODO**：标记待完成的功能或优化项，格式：`// TODO: 简要描述待完成内容`
- **FIXME**：标记已知问题或待修复的 bug，格式：`// FIXME: 简要描述问题及影响`
- **HACK**：标记临时方案或 workaround，格式：`// HACK: 简要描述临时方案及后续计划`
- 以上标记应在后续迭代中及时处理和清理

### Subagent 工作方式

- 默认直接在当前仓库中处理开发内容，不使用 worktree；如需隔离工作区，需由用户明确提出

## 测试要求

### 基本要求

- 所有测试位于 `src/test/java/`
- Java 测试统一使用 JUnit 5
- 测试类命名以 `Test` 结尾（如 `AgentBootstrapTest.java`）
- 测试方法命名使用 `should_ExpectedBehavior_When_Condition` 或简洁的中文描述

### 运行方式

使用 Maven 运行测试：

```bash
# 运行全部测试
mvn test

# 运行单个测试类
mvn test -Dtest=AgentBootstrapTest

# 运行某个模块的测试
mvn test -pl codeperf-agent

# 跳过测试（仅用于快速构建，不建议在提交前使用）
mvn package -DskipTests
```

### 前端与交付校验

- 提交前执行 `mvn clean compile test` 确保编译和测试通过
- 建议执行 `mvn package` 确保打包成功
- 代码变更应通过 IDE 的代码检查（如 IDEA 的 Inspections）

### Commit 与 PR

- 提交信息使用 Conventional Commits：`feat(scope): summary`、`fix(scope): summary`、`docs(scope): summary`
- 示例：`feat(agent): add Java stack sampler`、`fix(cli): correct config parsing`、`docs(core): update agent bootstrap doc`

### 开发规范（按照难易程度选择开发范式）

- 对简单问题或者 bugfix，直接进行开发和修复
- 对于较复杂的问题，使用 brainstorm 和 superpowers 工具进行规划和开发
- 对于横跨多个模块的特性开发和问题处理，请先使用 deep-plan 工具进行深入分析和指定计划，再使用 TDD 的范式进行开发和实现