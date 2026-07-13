#!/usr/bin/env bash

set -euo pipefail

SERVER_URL="${SERVER_URL:-http://127.0.0.1:9090}"
PROJECT="${PROJECT:-codeperf-demo}"
BRANCH="${BRANCH:-local}"
COMMIT="${COMMIT:-$(git rev-parse --short HEAD 2>/dev/null || echo local)}"
TARGET_PACKAGE="${TARGET_PACKAGE:-com.codeperf.demo}"
CLASSES_DIR="${CLASSES_DIR:-codeperf-demo/target/classes}"
SOURCE_ROOT="${SOURCE_ROOT:-codeperf-demo/src/main/java}"
AGENT_CONFIG="${AGENT_CONFIG:-config/agent.yml}"

echo "[codeperf] package"
mvn -q package

echo "[codeperf] create task"
TASK_ID="$(java -jar codeperf-cli/target/codeperf-cli.jar task \
  --server "$SERVER_URL" \
  --project "$PROJECT" \
  --commit "$COMMIT" \
  --branch "$BRANCH" \
  --env preprod)"
echo "[codeperf] task=$TASK_ID"

echo "[codeperf] static scan and upload"
java -jar codeperf-cli/target/codeperf-cli.jar scan-diff \
  --base origin/main \
  --head HEAD \
  --target-package "$TARGET_PACKAGE" \
  --classes-dir "$CLASSES_DIR" \
  --source-root "$SOURCE_ROOT" \
  --output build/codeperf/perf-static.json \
  --server "$SERVER_URL" \
  --task-id "$TASK_ID" \
  --upload

echo "[codeperf] render $AGENT_CONFIG with analysisTaskId=$TASK_ID before starting preprod app"
echo "[codeperf] example: java -javaagent:codeperf-agent/target/codeperf-agent.jar=$AGENT_CONFIG -jar app.jar"

echo "[codeperf] gate"
java -jar codeperf-cli/target/codeperf-cli.jar gate \
  --server "$SERVER_URL" \
  --task-id "$TASK_ID" \
  --fail-on WARN
