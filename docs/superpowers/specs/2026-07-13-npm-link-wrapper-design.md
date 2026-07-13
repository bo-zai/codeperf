# CodePerf npm link Wrapper 设计

## 背景

当前 `codeperf-cli` 已收敛为本地 AST 静态扫描和 agent 配置辅助工具，但真实用户不应该长期通过 `java -jar codeperf-cli/target/codeperf-cli.jar` 使用。第一阶段需要一个轻量 npm 包模拟企业开发者的命令体验：

```bash
codeperf scan
codeperf doctor
codeperf install-hooks
```

本轮目标只支持本地 `npm link` 验证，不实现公司 npm 私服发布，不实现 npm 包内置 jar，不实现安装时远程下载 jar。

## 目标

- 新增独立目录 `codeperf-npm/`，作为 npm wrapper 包。
- 通过 `npm link` 提供本地 `codeperf` 命令。
- `codeperf` 命令转发所有参数到 Java CLI jar。
- 当 Java CLI jar 不存在时，输出明确修复提示。
- 保持扫描核心仍在 `codeperf-cli`，npm wrapper 不解析配置、不扫描代码、不写报告。

## 非目标

- 不发布 npm 包到公司私服。
- 不实现 `postinstall` 自动构建 Java CLI。
- 不把 jar 复制进 npm 包。
- 不改变 `codeperf-cli` 的扫描逻辑。
- 不改变 agent 的启动方式。
- 不引入 CI 流水线接入。

## 目录结构

```text
codeperf-npm/
  package.json
  bin/
    codeperf.js
```

`codeperf-npm` 是安装分发层，不加入 Maven reactor。它依赖 Node.js 运行环境，但不引入第三方 npm 依赖。

## 命令行为

用户本地验证流程：

```bash
mvn -pl codeperf-cli package
cd codeperf-npm
npm link
cd ../codeperf-demo
codeperf scan --all
```

`bin/codeperf.js` 行为：

1. 根据脚本所在目录定位仓库根目录。
2. 拼出 Java CLI jar 路径：`../codeperf-cli/target/codeperf-cli.jar`。
3. 如果 jar 不存在，返回非 0，并提示执行：

   ```bash
   mvn -pl codeperf-cli package
   ```

4. 如果 jar 存在，执行：

   ```bash
   java -jar <jar> <透传参数>
   ```

5. Java 进程退出码原样返回给 shell。

## 与 install-hooks 的关系

`codeperf-cli install-hooks` 生成的 pre-push hook 内容保持：

```sh
codeperf scan
```

本地 `npm link` 后，hook 可以直接调用全局链接的 `codeperf` 命令。这比写死 `java -jar` 更贴近企业真实使用方式。

## 错误处理

- 找不到 jar：提示先构建 Java CLI，退出码 `2`。
- 找不到 Java：输出 Java 启动失败信息，退出码 `2`。
- Java CLI 正常执行但检测到风险：保留 Java CLI 的退出码，例如 `scan --all` 检测到 `WARN` 时返回 `1`。

## 测试策略

- 静态检查 `package.json` 的 `bin.codeperf` 指向 `bin/codeperf.js`。
- 本地手工验证：

  ```bash
  mvn -pl codeperf-cli package
  cd codeperf-npm
  npm link
  codeperf doctor
  cd ../codeperf-demo
  codeperf scan --all
  ```

- 预期 `codeperf scan --all` 在 demo 中返回 `1`，因为 demo 存在一个 `WARN` 风险。这是检测成功，不是 wrapper 失败。

## 后续扩展

正式企业分发前需要单独评审：

- npm 包是否内置 jar。
- 是否由 `postinstall` 下载指定版本 jar。
- npm 包版本与 Java CLI 版本如何绑定。
- 公司 npm 私服发布流程和权限。
- Windows、macOS、Linux 的命令兼容性。
