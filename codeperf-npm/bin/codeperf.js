#!/usr/bin/env node

const fs = require("fs");
const path = require("path");
const childProcess = require("child_process");

function packageRoot() {
  return path.resolve(__dirname, "..");
}

function repoRoot() {
  return path.resolve(packageRoot(), "..");
}

function packageJson() {
  return JSON.parse(fs.readFileSync(path.join(packageRoot(), "package.json"), "utf8"));
}

function targetJarPath() {
  return path.join(repoRoot(), "codeperf-cli", "target", "codeperf-cli.jar");
}

function bundledJarPath() {
  return path.join(packageRoot(), "dist", "codeperf-cli.jar");
}

function homeDir() {
  return process.env.USERPROFILE || process.env.HOME || process.cwd();
}

function cacheDirectory() {
  return process.env.CODEPERF_CLI_CACHE_DIR || path.join(homeDir(), ".codeperf", "bin");
}

function artifactCoordinate() {
  return process.env.CODEPERF_CLI_ARTIFACT
    || (packageJson().codeperf && packageJson().codeperf.cliArtifact)
    || "com.codeperf:codeperf-cli:1.0.0-SNAPSHOT:jar";
}

function artifactJarName(artifact) {
  const parts = artifact.split(":");
  if (parts.length < 3) {
    return "codeperf-cli.jar";
  }
  return `${parts[1]}-${parts[2]}.jar`;
}

function cachedJarPath(artifact) {
  return path.join(cacheDirectory(), artifactJarName(artifact));
}

function mavenCommand() {
  return process.env.CODEPERF_MVN_CMD || "mvn";
}

function downloadWithMaven(artifact, outputDirectory) {
  fs.mkdirSync(outputDirectory, { recursive: true });
  const args = [
    "dependency:copy",
    `-Dartifact=${artifact}`,
    `-DoutputDirectory=${outputDirectory}`,
    "-Dmdep.stripVersion=false",
  ];
  return childProcess.spawnSync(mavenCommand(), args, {
    stdio: "inherit",
    shell: process.env.CODEPERF_MVN_CMD_SHELL === "true",
  });
}

function resolveCliJar() {
  const targetJar = targetJarPath();
  if (fs.existsSync(targetJar)) {
    return targetJar;
  }

  const artifact = artifactCoordinate();
  const cachedJar = cachedJarPath(artifact);
  if (fs.existsSync(cachedJar)) {
    return cachedJar;
  }

  const download = downloadWithMaven(artifact, cacheDirectory());
  if (!download.error && download.status === 0 && fs.existsSync(cachedJar)) {
    return cachedJar;
  }

  const bundledJar = bundledJarPath();
  if (fs.existsSync(bundledJar)) {
    return bundledJar;
  }

  if (download.error) {
    console.error(`[codeperf] failed to run mvn: ${download.error.message}`);
  }
  console.error("[codeperf] codeperf-cli jar not found");
  console.error(`[codeperf] checked target jar: ${targetJar}`);
  console.error(`[codeperf] checked Maven artifact: ${artifact}`);
  console.error(`[codeperf] checked bundled jar: ${bundledJar}`);
  return null;
}

function javaCommand() {
  return process.env.CODEPERF_JAVA_CMD || "java";
}

function main() {
  const jar = resolveCliJar();
  if (!jar) {
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
