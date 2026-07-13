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
      CODEPERF_JAVA_CMD: javaBin,
      CODEPERF_JAVA_CMD_SHELL: process.platform === "win32" ? "true" : "false",
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
