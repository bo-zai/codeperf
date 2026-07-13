# npm link Wrapper Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a local `npm link` wrapper that exposes the `codeperf` command and delegates to the existing Java CLI jar.

**Architecture:** Create a separate `codeperf-npm/` package that contains only npm metadata and a Node.js bin script. The wrapper locates the repository root from its own file location, verifies `codeperf-cli/target/codeperf-cli.jar` exists, then runs `java -jar` with all user arguments and preserves the Java CLI exit code.

**Tech Stack:** Node.js built-in modules, npm `bin`, Maven-built Java CLI, PowerShell verification on Windows.

---

## File Structure

- Create `codeperf-npm/package.json`: npm package metadata and `bin.codeperf` mapping.
- Create `codeperf-npm/bin/codeperf.js`: executable Node.js wrapper around the Java CLI jar.
- Create `codeperf-npm/test/wrapper.test.js`: no-dependency Node.js tests for package metadata, missing-jar behavior, and argument forwarding.
- Modify `README.md`: document local `npm link` installation and verification.

## Task 1: Add npm Package Metadata

**Files:**
- Create: `codeperf-npm/package.json`
- Create: `codeperf-npm/test/wrapper.test.js`

- [ ] **Step 1: Write failing package metadata test**

Create `codeperf-npm/test/wrapper.test.js`:

```javascript
const assert = require("assert");
const fs = require("fs");
const path = require("path");

const packagePath = path.resolve(__dirname, "../package.json");

function testPackageBinMapping() {
  const pkg = JSON.parse(fs.readFileSync(packagePath, "utf8"));

  assert.strictEqual(pkg.name, "codeperf");
  assert.strictEqual(pkg.private, true);
  assert.deepStrictEqual(pkg.bin, {
    codeperf: "bin/codeperf.js",
  });
  assert.deepStrictEqual(pkg.files, [
    "bin/",
    "package.json",
  ]);
}

testPackageBinMapping();
console.log("wrapper tests passed");
```

- [ ] **Step 2: Run test and verify failure**

Run:

```bash
node codeperf-npm/test/wrapper.test.js
```

Expected:

```text
Error: ENOENT: no such file or directory
```

- [ ] **Step 3: Add `package.json`**

Create `codeperf-npm/package.json`:

```json
{
  "name": "codeperf",
  "version": "1.0.0-local",
  "private": true,
  "description": "Local npm wrapper for CodePerf Java CLI",
  "license": "UNLICENSED",
  "bin": {
    "codeperf": "bin/codeperf.js"
  },
  "files": [
    "bin/",
    "package.json"
  ],
  "scripts": {
    "test": "node test/wrapper.test.js"
  },
  "engines": {
    "node": ">=14"
  }
}
```

- [ ] **Step 4: Run test and verify pass**

Run:

```bash
npm --prefix codeperf-npm test
```

Expected:

```text
wrapper tests passed
```

## Task 2: Add Java CLI Wrapper Script

**Files:**
- Modify: `codeperf-npm/test/wrapper.test.js`
- Create: `codeperf-npm/bin/codeperf.js`

- [ ] **Step 1: Extend failing tests for wrapper script**

Replace `codeperf-npm/test/wrapper.test.js` with:

```javascript
const assert = require("assert");
const fs = require("fs");
const os = require("os");
const path = require("path");
const childProcess = require("child_process");

const packagePath = path.resolve(__dirname, "../package.json");
const wrapperPath = path.resolve(__dirname, "../bin/codeperf.js");

function testPackageBinMapping() {
  const pkg = JSON.parse(fs.readFileSync(packagePath, "utf8"));

  assert.strictEqual(pkg.name, "codeperf");
  assert.strictEqual(pkg.private, true);
  assert.deepStrictEqual(pkg.bin, {
    codeperf: "bin/codeperf.js",
  });
  assert.deepStrictEqual(pkg.files, [
    "bin/",
    "package.json",
  ]);
}

function testMissingJarMessage() {
  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), "codeperf-wrapper-"));
  const fakeWrapper = path.join(tempRoot, "codeperf-npm", "bin", "codeperf.js");
  fs.mkdirSync(path.dirname(fakeWrapper), { recursive: true });
  fs.copyFileSync(wrapperPath, fakeWrapper);

  const result = childProcess.spawnSync(process.execPath, [fakeWrapper, "doctor"], {
    cwd: tempRoot,
    encoding: "utf8",
  });

  assert.strictEqual(result.status, 2);
  assert.match(result.stderr, /codeperf-cli jar not found/);
  assert.match(result.stderr, /mvn -pl codeperf-cli package/);
}

function testArgumentForwardingAndExitCode() {
  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), "codeperf-wrapper-"));
  const fakeWrapper = path.join(tempRoot, "codeperf-npm", "bin", "codeperf.js");
  const jarPath = path.join(tempRoot, "codeperf-cli", "target", "codeperf-cli.jar");
  const javaBin = path.join(tempRoot, process.platform === "win32" ? "java.cmd" : "java");
  const logPath = path.join(tempRoot, "java-args.json");

  fs.mkdirSync(path.dirname(fakeWrapper), { recursive: true });
  fs.mkdirSync(path.dirname(jarPath), { recursive: true });
  fs.writeFileSync(jarPath, "fake jar");
  fs.copyFileSync(wrapperPath, fakeWrapper);

  if (process.platform === "win32") {
    fs.writeFileSync(javaBin, `@echo off\r\nnode -e "require('fs').writeFileSync(process.argv[1], JSON.stringify(process.argv.slice(2)))" "${logPath}" %*\r\nexit /b 7\r\n`);
  } else {
    fs.writeFileSync(javaBin, `#!/usr/bin/env sh\nnode -e "require('fs').writeFileSync(process.argv[1], JSON.stringify(process.argv.slice(2)))" "${logPath}" "$@"\nexit 7\n`);
    fs.chmodSync(javaBin, 0o755);
  }

  const result = childProcess.spawnSync(process.execPath, [fakeWrapper, "scan", "--all"], {
    cwd: tempRoot,
    env: Object.assign({}, process.env, {
      PATH: tempRoot + path.delimiter + process.env.PATH,
    }),
    encoding: "utf8",
  });

  assert.strictEqual(result.status, 7);
  assert.deepStrictEqual(JSON.parse(fs.readFileSync(logPath, "utf8")), [
    "-jar",
    jarPath,
    "scan",
    "--all",
  ]);
}

testPackageBinMapping();
testMissingJarMessage();
testArgumentForwardingAndExitCode();
console.log("wrapper tests passed");
```

- [ ] **Step 2: Run test and verify failure**

Run:

```bash
npm --prefix codeperf-npm test
```

Expected:

```text
Error: ENOENT: no such file or directory
```

The missing file should be `codeperf-npm/bin/codeperf.js`.

- [ ] **Step 3: Implement wrapper script**

Create `codeperf-npm/bin/codeperf.js`:

```javascript
#!/usr/bin/env node

const fs = require("fs");
const path = require("path");
const childProcess = require("child_process");

function repoRoot() {
  return path.resolve(__dirname, "../..");
}

function cliJarPath() {
  return path.join(repoRoot(), "codeperf-cli", "target", "codeperf-cli.jar");
}

function main() {
  const jar = cliJarPath();
  if (!fs.existsSync(jar)) {
    console.error(`[codeperf] codeperf-cli jar not found: ${jar}`);
    console.error("[codeperf] Please build it first: mvn -pl codeperf-cli package");
    return 2;
  }

  const result = childProcess.spawnSync("java", ["-jar", jar].concat(process.argv.slice(2)), {
    stdio: "inherit",
  });

  if (result.error) {
    console.error(`[codeperf] failed to start java: ${result.error.message}`);
    return 2;
  }
  return typeof result.status === "number" ? result.status : 2;
}

process.exit(main());
```

- [ ] **Step 4: Run wrapper tests and verify pass**

Run:

```bash
npm --prefix codeperf-npm test
```

Expected:

```text
wrapper tests passed
```

## Task 3: Update README for npm link Flow

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Update README usage section**

Modify the local CLI usage section so it says:

```markdown
本地开发推荐先构建 Java CLI，再通过 `npm link` 暴露 `codeperf` 命令：

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
```

Keep the existing `java -jar` commands under a short fallback paragraph:

```markdown
如果暂时不使用 npm link，也可以直接执行 jar：

```bash
java -jar codeperf-cli/target/codeperf-cli.jar scan --all
```
```

- [ ] **Step 2: Check README does not promise npm publish**

Run:

```bash
rg "npm publish|私服发布|postinstall|下载 jar" README.md
```

Expected:

```text
No matches.
```

## Task 4: End-to-End npm link Verification

**Files:**
- No source edits unless verification exposes a defect.

- [ ] **Step 1: Build Java CLI jar**

Run:

```bash
mvn -pl codeperf-cli package
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 2: Run npm wrapper tests**

Run:

```bash
npm --prefix codeperf-npm test
```

Expected:

```text
wrapper tests passed
```

- [ ] **Step 3: Link local npm package**

Run:

```bash
cd codeperf-npm
npm link
cd ..
```

Expected:

```text
added 1 package
```

The exact npm wording can vary by npm version. Treat exit code `0` as success.

- [ ] **Step 4: Verify global command resolves**

Run:

```bash
codeperf doctor
```

Expected:

```text
[codeperf] doctor 检查通过
```

- [ ] **Step 5: Verify demo scan from subdirectory**

Run from `codeperf-demo`:

```bash
codeperf scan --all
```

Expected:

```text
[codeperf] sourceFiles=7, findings=1, parseErrors=0
```

Expected exit code is `1`, because demo intentionally contains a `WARN` finding and `.codeperf.yml` uses `failOn: WARN`.

- [ ] **Step 6: Run final full package verification**

Run:

```bash
mvn package
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 7: Run quality scans**

Run:

```bash
rg "printStackTrace|TO[D]O|TB[D]" codeperf-cli codeperf-agent codeperf-server codeperf-npm
rg "npm publish|postinstall|下载 jar" README.md codeperf-npm docs/superpowers/specs/2026-07-13-npm-link-wrapper-design.md
```

Expected:

```text
No matches.
```

The second scan may match the design spec if those terms appear only in explicit non-goals or future-work sections. Do not treat that as an implementation failure.

- [ ] **Step 8: Commit implementation**

Run:

```bash
git add README.md codeperf-npm docs/superpowers/plans/2026-07-13-npm-link-wrapper-implementation.md
git diff --cached --check
git commit -m "feat: add npm link wrapper"
```

Expected:

```text
One implementation commit containing the npm wrapper, tests, README update, and plan.
```

## Self-Review Checklist

- Spec coverage: `codeperf-npm/`, `npm link`, argument forwarding, missing jar message, and Java CLI exit-code preservation are covered.
- Scope check: no npm publish, no postinstall, no jar embedding, no Java CLI scan logic changes.
- Test coverage: package metadata, missing jar behavior, argument forwarding, npm link manual verification, root and demo command execution.
- Type consistency: `package.json` maps `bin.codeperf` to `bin/codeperf.js`, and tests use that exact path.
