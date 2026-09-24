#!/usr/bin/env bash
set -euo pipefail
PACKAGE="${SIAA_PACKAGE:-com.siaa.app}"
STAMP="$(date +%Y%m%d-%H%M%S)"
OUT="${1:-device-qa-$STAMP}"
command -v adb >/dev/null || { echo "adb no está disponible" >&2; exit 2; }
adb get-state >/dev/null
mkdir -p "$OUT"
adb shell getprop > "$OUT/getprop.txt"
adb shell dumpsys package "$PACKAGE" > "$OUT/package.txt" || true
adb shell dumpsys media_session > "$OUT/media_session.txt" || true
adb shell dumpsys audio > "$OUT/audio.txt" || true
adb shell dumpsys notification --noredact > "$OUT/notification.txt" || true
adb shell dumpsys activity services "$PACKAGE" > "$OUT/services.txt" || true
adb shell dumpsys power > "$OUT/power.txt" || true
adb shell dumpsys deviceidle > "$OUT/deviceidle.txt" || true
adb logcat -d -v threadtime > "$OUT/logcat.txt" || true
{
  echo "package=$PACKAGE"
  echo "captured_at=$(date -Iseconds)"
  echo "serial=$(adb get-serialno)"
  echo "model=$(adb shell getprop ro.product.manufacturer | tr -d '\r') $(adb shell getprop ro.product.model | tr -d '\r')"
  echo "android=$(adb shell getprop ro.build.version.release | tr -d '\r')"
  echo "sdk=$(adb shell getprop ro.build.version.sdk | tr -d '\r')"
  echo "build=$(adb shell getprop ro.build.fingerprint | tr -d '\r')"
} > "$OUT/summary.txt"
echo "Evidencia guardada en $OUT"
