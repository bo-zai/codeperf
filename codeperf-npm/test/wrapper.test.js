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
    "dist/",
    "package.json",
  ]);
  assert.strictEqual(pkg.codeperf.cliArtifact, "com.cmb.codeperf:codeperf-cli:1.0.0-SNAPSHOT:jar");
}

function testMissingJarMessage() {
  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), "codeperf-wrapper-"));
  const fakeWrapper = path.join(tempRoot, "codeperf-npm", "bin", "codeperf.js");
  const fakeMaven = path.join(tempRoot, process.platform === "win32" ? "mvn.cmd" : "mvn");
  fs.mkdirSync(path.dirname(fakeWrapper), { recursive: true });
  fs.copyFileSync(wrapperPath, fakeWrapper);
  fs.copyFileSync(packagePath, path.join(tempRoot, "codeperf-npm", "package.json"));
  writeCommand(fakeMaven, "exit 9");

  const result = childProcess.spawnSync(process.execPath, [fakeWrapper, "doctor"], {
    cwd: tempRoot,
    env: Object.assign({}, process.env, {
      CODEPERF_MVN_CMD: fakeMaven,
      CODEPERF_MVN_CMD_SHELL: process.platform === "win32" ? "true" : "false",
    }),
    encoding: "utf8",
  });

  assert.strictEqual(result.status, 2);
  assert.match(result.stderr, /codeperf-cli jar not found/);
  assert.match(result.stderr, /checked Maven artifact/);
  assert.match(result.stderr, /checked bundled jar/);
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
  fs.copyFileSync(packagePath, path.join(tempRoot, "codeperf-npm", "package.json"));

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

function testMavenArtifactIsDownloadedBeforeBundledJar() {
  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), "codeperf-wrapper-"));
  const fakeWrapper = path.join(tempRoot, "codeperf-npm", "bin", "codeperf.js");
  const fakeMaven = path.join(tempRoot, process.platform === "win32" ? "mvn.cmd" : "mvn");
  const fakeJava = path.join(tempRoot, process.platform === "win32" ? "java.cmd" : "java");
  const cacheDir = path.join(tempRoot, "cache");
  const logPath = path.join(tempRoot, "java-args.json");
  const cachedJar = path.join(cacheDir, "codeperf-cli-1.0.0-SNAPSHOT.jar");

  fs.mkdirSync(path.dirname(fakeWrapper), { recursive: true });
  fs.copyFileSync(wrapperPath, fakeWrapper);
  fs.copyFileSync(packagePath, path.join(tempRoot, "codeperf-npm", "package.json"));
  writeCommand(fakeMaven, `node -e "require('fs').mkdirSync('${escapeForNode(cacheDir)}',{recursive:true});require('fs').writeFileSync('${escapeForNode(cachedJar)}','remote jar')"\nexit 0`);
  writeJavaRecorder(fakeJava, logPath, 0);

  const result = childProcess.spawnSync(process.execPath, [fakeWrapper, "scan"], {
    cwd: tempRoot,
    env: Object.assign({}, process.env, {
      CODEPERF_CLI_CACHE_DIR: cacheDir,
      CODEPERF_MVN_CMD: fakeMaven,
      CODEPERF_MVN_CMD_SHELL: process.platform === "win32" ? "true" : "false",
      CODEPERF_JAVA_CMD: fakeJava,
      CODEPERF_JAVA_CMD_SHELL: process.platform === "win32" ? "true" : "false",
    }),
    encoding: "utf8",
  });

  assert.strictEqual(result.status, 0);
  assert.deepStrictEqual(JSON.parse(fs.readFileSync(logPath, "utf8")), [
    "-jar",
    cachedJar,
    "scan",
  ]);
}

function testBundledJarIsUsedWhenMavenDownloadFails() {
  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), "codeperf-wrapper-"));
  const fakeWrapper = path.join(tempRoot, "codeperf-npm", "bin", "codeperf.js");
  const bundledJar = path.join(tempRoot, "codeperf-npm", "dist", "codeperf-cli.jar");
  const fakeMaven = path.join(tempRoot, process.platform === "win32" ? "mvn.cmd" : "mvn");
  const fakeJava = path.join(tempRoot, process.platform === "win32" ? "java.cmd" : "java");
  const logPath = path.join(tempRoot, "java-args.json");

  fs.mkdirSync(path.dirname(fakeWrapper), { recursive: true });
  fs.mkdirSync(path.dirname(bundledJar), { recursive: true });
  fs.copyFileSync(wrapperPath, fakeWrapper);
  fs.copyFileSync(packagePath, path.join(tempRoot, "codeperf-npm", "package.json"));
  fs.writeFileSync(bundledJar, "bundled jar");
  writeCommand(fakeMaven, "exit 9");
  writeJavaRecorder(fakeJava, logPath, 0);

  const result = childProcess.spawnSync(process.execPath, [fakeWrapper, "doctor"], {
    cwd: tempRoot,
    env: Object.assign({}, process.env, {
      CODEPERF_CLI_CACHE_DIR: path.join(tempRoot, "cache"),
      CODEPERF_MVN_CMD: fakeMaven,
      CODEPERF_MVN_CMD_SHELL: process.platform === "win32" ? "true" : "false",
      CODEPERF_JAVA_CMD: fakeJava,
      CODEPERF_JAVA_CMD_SHELL: process.platform === "win32" ? "true" : "false",
    }),
    encoding: "utf8",
  });

  assert.strictEqual(result.status, 0);
  assert.deepStrictEqual(JSON.parse(fs.readFileSync(logPath, "utf8")), [
    "-jar",
    bundledJar,
    "doctor",
  ]);
}

function writeJavaRecorder(commandPath, logPath, exitCode) {
  const script = `node -e "require('fs').writeFileSync(process.argv[1], JSON.stringify(process.argv.slice(2)))" "${logPath}" %ARGS%\nexit ${exitCode}`;
  writeCommand(commandPath, script);
}

function writeCommand(commandPath, body) {
  if (process.platform === "win32") {
    fs.writeFileSync(commandPath, `@echo off\r\n${body.replace(/%ARGS%/g, "%*").replace(/\n/g, "\r\n")}\r\n`);
  } else {
    fs.writeFileSync(commandPath, `#!/usr/bin/env sh\n${body.replace(/%ARGS%/g, '"$@"')}\n`);
    fs.chmodSync(commandPath, 0o755);
  }
}

function escapeForNode(value) {
  return value.replace(/\\/g, "\\\\").replace(/'/g, "\\'");
}

testPackageBinMapping();
testMissingJarMessage();
testArgumentForwardingAndExitCode();
testMavenArtifactIsDownloadedBeforeBundledJar();
testBundledJarIsUsedWhenMavenDownloadFails();
console.log("wrapper tests passed");

