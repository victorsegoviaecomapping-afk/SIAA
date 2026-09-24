# SIAA Android v2.3 — Release Candidate preparado para CI + QA físico/humano

La v2.3 conserva la cobertura curricular cerrada de v2.2 y se concentra en los puntos que impedían tratarla como candidata seria de producto: lenguaje learner-facing, ciclo de vida Android, instrumentación, build reproducible, evidencia física y transición controlada a audio humano.

## Incorporado

- Humanización de los 411 KCs gramaticales importados de CEFR-J.
- Regeneración de 1,233 actividades CEFR-J con modelos naturales en inglés y etiquetas comprensibles.
- Eliminación de los 4 distractores léxicos genéricos restantes.
- Gate `linguistic_qa_audit.py` integrado en CI.
- Hardening de `SiaaPlaybackService` ante salida insegura, AudioFocus denegado, token inválido y pérdida de ruta durante calibración.
- Test instrumentado del router de botones y perfiles calibrados.
- Android CI con Gradle 9.6.0 provisionado y SDK instalado en runner.
- Workflow de emulador para `connectedDebugAndroidTest` de app + Room.
- `device_media_probe.sh` y `device_qa_collect.sh` para reproducir/capturar pruebas físicas.
- Pipeline `import_human_audio.py` con provenance obligatoria, normalización FFmpeg y actualización de hashes.
- `audio_signal_audit.py` para fonología y cualquier audio humano activado.
- Protocolos v2.3 de QA físico, lingüístico, audio humano y release readiness.

## Conteos preservados

- 3,651 KCs.
- 7,175 relaciones.
- 9,473 ejercicios estáticos.
- 2,746 lexemas.
- 315 KCs multiword/formulaicos.
- ~9,133 variantes léxicas generables.
- ~18,606 actividades potenciales.
- 162 `LISTENING_AB`.
- 73 `PRON_DISCRIMINATION`.
- 62 KCs pragmáticos.
- 3,648 audios offline (35,304,019 bytes indexados).

## Lo que queda fuera del artefacto

No se declara completado lo que requiere evidencia externa: ejecución real del build Android en SDK, emulador/CI sobre la revisión publicada, matriz física teléfono × audífonos, revisión lingüística humana, grabaciones humanas materializadas/licenciadas y calibración longitudinal con usuarios.
