# QA report — SIAA Android v1.6

Fecha: 2026-09-21

## Contenido

- 3,281 KCs.
- 6,652 relaciones.
- 7,751 ejercicios estáticos.
- 2,746 lexemas.
- ~8,944 variantes generables.
- 2,901 audios offline indexados.

## Checks ejecutados

- `python3 scripts/validate_project.py` — PASS.
- `python3 scripts/content_depth_audit.py` — PASS.
- `python3 scripts/audio_asset_audit.py` — PASS.
- `bash scripts/smoke_pure_kotlin.sh` — PASS.
- `bash scripts/smoke_content.sh` — PASS.
- `bash scripts/smoke_runtime.sh` — PASS.
- `bash scripts/smoke_spelling_runtime.sh` — PASS.
- `bash scripts/smoke_runtime_regressions.sh` — PASS: STOP, PAUSE, TIMEOUT, TEACH, turn gate, restore, route-loss finishing y restore planning.

## Auditoría CEFR-J

`found=2692/2746 exact_level_match=2599/2692`

Las discrepancias de nivel no se corrigen automáticamente: se mantienen para revisión porque un headword puede tener sentidos/usos distintos por nivel.

## Limitaciones

- El audio sigue siendo sintético pre-renderizado en esta versión.
- No se ejecutó `:app:assembleDebug` en este entorno por ausencia de Android SDK.
- Falta QA humano sistemático de sentidos, collocations y audio humano C1/C2.
