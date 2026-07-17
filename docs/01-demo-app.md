# 01 · Demo 多模块项目设计

> 关联主文档：`00-overview-architecture.md`。本文件描述当前 demo 项目的真实结构与验证目标。

## 1. 设计目标

Demo 项目用于验证 CodePerf 在真实企业多模块仓库中的扫描效果，而不是为了适配某条规则而构造孤立代码。它模拟常见的 admin、app、common 分层，覆盖控制器、服务编排、Repository、客户端 SDK、缓存、跨模块调用等写法。

当前 demo 模块：

- `codeperf-demo-admin`：管理端批量处理、报表、审核等场景。
- `codeperf-demo-app`：用户端查询、下单、聚合展示等场景。
- `codeperf-demo-common`：公共客户端、领域对象、缓存或通用服务。

旧单模块 `codeperf-demo` 已删除，不再作为构建、扫描或文档基准。

## 2. 验证重点

Demo 主要验证以下能力：

- 多模块源码根识别是否正确。
- CLI 控制台是否按模块输出中文摘要。
- HTML 报告是否按模块、文件、问题展示，支持右侧目录快速定位。
- 静态规则是否能发现循环内 MySQL、MongoDB、Redis、GaussDB、HTTP、RPC、SDK 等 I/O 风险。
- 跨方法调用链是否能把循环位置与最终 I/O 调用关联起来。
- Git 归因是否能区分本次变更风险、历史风险与未归因风险。

## 3. 扫描配置

根目录 `.codeperf.yml` 声明 demo 的源码根与模块边界：

```yaml
project: codeperf-demo-multi-module

staticScan:
  enabled: true
  mode: changed
  sourceRoots:
    - codeperf-demo-admin/src/main/java
    - codeperf-demo-app/src/main/java
    - codeperf-demo-common/src/main/java
  failOn: WARN

modules:
  - name: codeperf-demo-admin
    sourceRoots:
      - codeperf-demo-admin/src/main/java
  - name: codeperf-demo-app
    sourceRoots:
      - codeperf-demo-app/src/main/java
  - name: codeperf-demo-common
    sourceRoots:
      - codeperf-demo-common/src/main/java
```

`staticScan.sourceRoots` 决定扫描范围，`modules` 决定报告中的模块归属与统计展示。

## 4. 本地验证命令

```bash
mvn -pl codeperf-demo-common,codeperf-demo-admin,codeperf-demo-app compile
mvn -pl codeperf-cli package -DskipTests
java -jar codeperf-cli/target/codeperf-cli.jar scan --all
```

预期结果是 demo 中存在若干刻意保留的真实业务风险样例，因此 `scan --all` 可能返回非 0 退出码。这里的非 0 不代表 CLI 异常，而代表门禁发现了达到阈值的风险。
