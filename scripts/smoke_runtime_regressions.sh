#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
command -v kotlinc >/dev/null || { echo 'kotlinc is required for offline runtime regression smoke' >&2; exit 1; }
command -v kotlin >/dev/null || { echo 'kotlin runner is required for offline runtime regression smoke' >&2; exit 1; }
KOTLINC_BIN="$(command -v kotlinc)"
KOTLIN_HOME="$(cd "$(dirname "$KOTLINC_BIN")/.." && pwd)"
COROUTINES_JAR="$KOTLIN_HOME/lib/kotlinx-coroutines-core-jvm.jar"
[ -f "$COROUTINES_JAR" ] || { echo "Missing $COROUTINES_JAR" >&2; exit 1; }
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT
mapfile -t SOURCES < <(find \
  "$ROOT/core/model/src/main/kotlin" \
  "$ROOT/core/algorithm/src/main/kotlin" \
  "$ROOT/core/runtime/src/main/kotlin" \
  -name '*.kt' -print | sort)
kotlinc "${SOURCES[@]}" "$ROOT/tools/runtime_regression_smoke.kt" \
  -cp "$COROUTINES_JAR" \
  -d "$TMP_DIR/runtime-regression.jar"
kotlin -cp "$TMP_DIR/runtime-regression.jar:$COROUTINES_JAR" Runtime_regression_smokeKt
