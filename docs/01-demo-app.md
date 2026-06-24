# 01 · Demo 靶子应用设计

> 关联主文档：`00-overview-architecture.md`。本文件描述 `codeperf-demo` 模块的设计。
> 用途：① 在不依赖团队多服务环境的前提下，作为 CodePerf 开发期可 attach 的靶子；
> ② 内置**已知性能坑**，用来检验工具“报得准不准”；③ 作为回归测试夹具。

## 1. 技术栈

- Spring Boot 2.7.18（最后支持 Java 8 的大版本线）
- spring-boot-starter-web（HTTP 入口）
- spring-boot-starter-jdbc + JdbcTemplate（SQL 走 JDBC 层，便于 agent 在 JDBC 层拦截，且 ORM 无关）
- H2 内嵌数据库（无外部依赖，进程内即可跑）
- 运行 JDK：1.8（必须用 `D:\Java8\jdk1.8.0_341` 的 java 启动）

## 2. 数据模型（schema.sql）

```
users(id BIGINT PK, name VARCHAR)
orders(id BIGINT PK, user_id BIGINT, item_name VARCHAR, amount DECIMAL)
```

种子数据由 `DataSeeder`（CommandLineRunner）程序化写入，便于调整规模：
- 默认 50 个用户；
- 每个用户 3~8 条订单；
- item_name 从一个固定候选集合中取（制造大量重复，凸显去重逻辑的 O(n²)）。

## 3. HTTP 接口

### 3.1 `POST /api/orders/report`（主坑接口）
编排式生成“订单报表”，**故意**串联多个性能反模式：

| 步骤 | 行为 | 反模式 | 期望被检出 |
|---|---|---|---|
| A 查用户 | `SELECT * FROM users`（1 次） | — | 正常基线 |
| B 查订单 | **对每个用户**执行 `SELECT * FROM orders WHERE user_id=?`（N 次） | **N+1 查询** | 🔴 N+1（同 SQL 指纹单请求内执行 N≥阈值次） |
| C 去重 | 用 `if(!list.contains(x)) list.add(x)` 在循环中对所有订单做 item 去重 | **O(n²) 的 List.contains** | CPU 热点方法 / （1.5期静态规则） |
| D 计算 | 在单个方法里跑一个重计算循环（大量迭代 + Math 运算）模拟“算分” | **CPU 热点** | 🔴 CPU 热点（单方法占比高） |
| E 分配 | 构造一个较大的 `byte[]` 与大 `StringBuilder` 拼报文 | **大对象 / 高分配** | 🟡 分配量异常 / 🔵 大对象 |

返回 JSON 摘要：用户数、订单数、去重后 item 数、算分结果、报文长度、耗时。

### 3.2 `GET /api/users/{id}`（基线接口）
轻量：1 次按主键查用户 + 1 次查其订单。作为“正常接口”对照，验证工具不会对干净接口误报。

## 4. 与检测规则的对应关系（用于端到端验收）

attach 到本应用、对 `POST /api/orders/report` 发一次请求后，工具报告应至少命中：
- 🔴 **N+1**：`orders WHERE user_id=?` 在一次请求内执行 ≈ 用户数 次。
- 🔴 **CPU 热点**：算分方法（步骤 D）与/或去重方法（步骤 C）占比高。
- 🟡 **分配量/大对象**：步骤 E 的大 `byte[]`。
- 入口请求总耗时偏高。

而 `GET /api/users/{id}` 不应产生 🔴 级问题。

## 5. 包结构

```
com.codeperf.demo
├── DemoApplication            启动类
├── DataSeeder                 CommandLineRunner，建库后写种子数据
├── repo/UserRepository        JdbcTemplate 封装：findAllUsers / findUserById / findOrdersByUserId
├── domain/User, domain/Order  简单 POJO
├── service/OrderReportService 编排 A~E 步骤（性能坑集中在此）
└── web/ReportController       两个 HTTP 接口
```

## 6. 配置（application.yml）

- 内嵌 H2，`spring.sql.init.mode=always` 确保执行 schema.sql。
- 端口 8080。
- 关闭不必要的自动配置以减少噪声（可选）。

## 7. 启动与验证（开发期）

```bash
# 用 JDK8 构建并运行（IntelliJ 或命令行）
"$JAVA_HOME/bin/java" -jar codeperf-demo/target/codeperf-demo-1.0.0-SNAPSHOT.jar
# 触发主坑接口
curl -X POST http://localhost:8080/api/orders/report
# 基线接口
curl http://localhost:8080/api/users/1
```

> 规模参数（用户数/订单数/算分迭代次数/分配大小）集中在 `DataSeeder` 与 `OrderReportService` 顶部常量，便于放大坑的可见度。改动这些常量后须同步更新本文档第 2、3 节的数值。
