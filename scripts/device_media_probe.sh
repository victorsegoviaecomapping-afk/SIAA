#!/usr/bin/env bash
set -euo pipefail
command -v adb >/dev/null || { echo "adb no está disponible" >&2; exit 2; }
adb get-state >/dev/null
DELAY="${SIAA_MEDIA_PROBE_DELAY:-2}"
echo "SIAA debe estar ya en una sesión activa antes de ejecutar esta sonda."
for key in KEYCODE_MEDIA_PLAY_PAUSE KEYCODE_MEDIA_NEXT KEYCODE_MEDIA_PREVIOUS KEYCODE_MEDIA_STOP; do
  echo "-> $key"
  adb shell input keyevent "$key"
  sleep "$DELAY"
done
echo "Estado final de MediaSession:"
adb shell dumpsys media_session | sed -n '/com.siaa.app/,+80p' || true
