#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"; OUT="${TMPDIR:-/tmp}/siaa-v24-feature-smoke"; rm -rf "$OUT"; mkdir -p "$OUT"
mapfile -t SRC < <(find "$ROOT/core/model/src/main/kotlin" "$ROOT/core/algorithm/src/main/kotlin" -name '*.kt' -print | sort)
kotlinc "${SRC[@]}" "$ROOT/tools/production_placement_smoke.kt" -include-runtime -d "$OUT/v24.jar"
java -jar "$OUT/v24.jar"
