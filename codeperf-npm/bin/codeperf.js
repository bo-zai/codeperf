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

function javaCommand() {
  return process.env.CODEPERF_JAVA_CMD || "java";
}

function main() {
  const jar = cliJarPath();
  if (!fs.existsSync(jar)) {
    console.error(`[codeperf] codeperf-cli jar not found: ${jar}`);
    console.error("[codeperf] Please build it first: mvn -pl codeperf-cli package");
    return 2;
  }

  const result = childProcess.spawnSync(javaCommand(), ["-jar", jar].concat(process.argv.slice(2)), {
    stdio: "inherit",
    shell: process.env.CODEPERF_JAVA_CMD_SHELL === "true",
  });

  if (result.error) {
    console.error(`[codeperf] failed to start java: ${result.error.message}`);
    return 2;
  }
  return typeof result.status === "number" ? result.status : 2;
}

process.exit(main());
