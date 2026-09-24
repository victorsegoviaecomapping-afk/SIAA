# QA report — SIAA Android v1.2

Fecha: 2026-09-21

## Contenido validado

- 1,564 KCs.
- 6,652 relaciones.
- 4,317 ejercicios estáticos.
- 1,029 lexemas activos.
- ~3,153 variantes léxicas generables.
- ~7,470 actividades potenciales.
- 102 ejercicios `LISTENING_AB`.
- 41 ejercicios `PRON_DISCRIMINATION`.
- 36 KCs pragmáticos con reconocimiento objetivo.
- 1,029/1,029 lexemas con tarea de transferencia.
- 1,184 assets de audio offline indexados.

## Checks ejecutados

- `python scripts/validate_project.py` — PASS.
- `python scripts/audio_asset_audit.py` — PASS.
- `python scripts/content_depth_audit.py` — PASS.
- `python scripts/smoke_migration.py` — PASS.
- `bash scripts/smoke_pure_kotlin.sh` — PASS.
- `bash scripts/smoke_content.sh` — PASS.
- `bash scripts/smoke_runtime.sh` — PASS.
- `bash scripts/smoke_runtime_regressions.sh` — PASS.
- `bash scripts/smoke_spelling_runtime.sh` — PASS.

Las regresiones cubren STOP, PAUSE/RESUME, timeout, TEACH, turn-gate, restore, pérdida de ruta durante finishing y restore durante planning.

## Spelling auditivo

La prueba específica confirma que `SPELLING_AB` verbaliza A/B mediante **nombres ingleses de letras**, no como dos palabras pronunciadas, y que `PREDICTION_RECORDED` se guarda antes de conocer la respuesta.

## Audio

El audio bundled actual es sintético. El auditor comprueba cobertura exacta de 102/102 actividades de listening y 1,029/1,029 lexemas. El plan de sustitución por audio humano se documenta en `HUMAN_AUDIO_PLAN_V12.md`.

## Validación pendiente fuera de este entorno

No se ejecutó `:app:assembleDebug` porque el entorno no tiene Android SDK. Tampoco se puede declarar validado el comportamiento con audífonos Bluetooth reales, lock screen/OEM, llamadas/interrupciones ni la calidad perceptiva de clips humanos hasta usar hardware y material humano definitivo.
