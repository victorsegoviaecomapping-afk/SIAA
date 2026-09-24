#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
KOTLINC_BIN="$(command -v kotlinc)"
KOTLIN_HOME="$(cd "$(dirname "$KOTLINC_BIN")/.." && pwd)"
COROUTINES_JAR="$KOTLIN_HOME/lib/kotlinx-coroutines-core-jvm.jar"
TMP_DIR="$(mktemp -d)"; trap 'rm -rf "$TMP_DIR"' EXIT
mapfile -t SOURCES < <(find "$ROOT/core/model/src/main/kotlin" "$ROOT/core/algorithm/src/main/kotlin" "$ROOT/core/runtime/src/main/kotlin" -name '*.kt' -print | sort)
kotlinc "${SOURCES[@]}" "$ROOT/tools/spelling_runtime_smoke.kt" -cp "$COROUTINES_JAR" -d "$TMP_DIR/spell-smoke.jar"
kotlin -cp "$TMP_DIR/spell-smoke.jar:$COROUTINES_JAR" Spelling_runtime_smokeKt
