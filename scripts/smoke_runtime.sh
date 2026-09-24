#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

# Prefer the real Gradle test task when the wrapper is already usable.
if [ -f "$ROOT/gradle/wrapper/gradle-wrapper.jar" ]; then
  exec "$ROOT/gradlew" :core:runtime:test
fi

# Offline fallback: compile the pure Kotlin runtime smoke directly. This makes
# the ZIP auditable even when the wrapper JAR is absent and networking is blocked.
if command -v kotlinc >/dev/null && command -v kotlin >/dev/null; then
  KOTLINC_BIN="$(command -v kotlinc)"
  KOTLIN_HOME="$(cd "$(dirname "$KOTLINC_BIN")/.." && pwd)"
  COROUTINES_JAR="$KOTLIN_HOME/lib/kotlinx-coroutines-core-jvm.jar"
  if [ -f "$COROUTINES_JAR" ]; then
    TMP_DIR="$(mktemp -d)"
    trap 'rm -rf "$TMP_DIR"' EXIT
    mapfile -t SOURCES < <(find \
      "$ROOT/core/model/src/main/kotlin" \
      "$ROOT/core/algorithm/src/main/kotlin" \
      "$ROOT/core/runtime/src/main/kotlin" \
      -name '*.kt' -print | sort)
    kotlinc "${SOURCES[@]}" "$ROOT/tools/runtime_smoke.kt" \
      -cp "$COROUTINES_JAR" \
      -d "$TMP_DIR/runtime-smoke.jar"
    exec kotlin -cp "$TMP_DIR/runtime-smoke.jar:$COROUTINES_JAR" Runtime_smokeKt
  fi
fi

# Last resort for normal developer/CI environments: bootstrap the official wrapper.
if [ -x "$ROOT/scripts/bootstrap_gradle.sh" ]; then
  exec "$ROOT/scripts/bootstrap_gradle.sh" :core:runtime:test
fi

if command -v gradle >/dev/null; then
  exec gradle :core:runtime:test
fi

printf '%s\n' 'Unable to run runtime smoke: no usable Gradle wrapper and no Kotlin offline toolchain.' >&2
exit 1
