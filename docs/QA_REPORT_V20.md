# QA report — SIAA Android v2.0 Content Complete Candidate

Fecha: 2026-09-21

## Contenido validado

- 3,520 KCs.
- 6,952 relaciones.
- 8,934 ejercicios estáticos.
- 2,746 lexemas.
- 202 KCs multiword/formulaicos.
- ~8,944 variantes léxicas generables.
- ~17,878 actividades potenciales.
- 138 ejercicios de listening.
- 65 `PRON_DISCRIMINATION`.
- 54 KCs pragmáticos.
- 3,351 assets de audio offline indexados.

## Gates de profundidad

`python3 scripts/content_depth_audit.py` — PASS.

- Listening: A1 26 · A2 23 · B1 22 · B2 22 · C1 22 · C2 22.
- Pronunciación: A1 14 · A2 14 · B1 10 · B2 10 · C1 10 · C2 7.
- Multiword KCs: A1 27 · A2 32 · B1 36 · B2 36 · C1 36 · C2 35.
- Pragmática: A1 7 · A2 8 · B1 8 · B2 10 · C1 11 · C2 10.
- Transferencia léxica: 2,746 / 2,746.
- Reconocimiento pragmático objetivo: 54 / 54.
- Gramática objective/teach/transfer: 425 / 425 en las tres capas.

## Audio

`python3 scripts/audio_asset_audit.py` — PASS.

- vocab: 2,746;
- listening: 138;
- phonology: 77 estímulos únicos;
- phrase: 390;
- total: 3,351;
- bytes indexados: 31,850,889.

Todos los assets están marcados como audio sintético pre-renderizado. Los checksums individuales y el hash del índice se validan.

## Smokes ejecutados

- `python3 scripts/validate_project.py` — PASS.
- `bash scripts/smoke_pure_kotlin.sh` — PASS.
- `bash scripts/smoke_content.sh` — PASS.
- `bash scripts/smoke_runtime.sh` — PASS.
- `bash scripts/smoke_spelling_runtime.sh` — PASS.
- `bash scripts/smoke_runtime_regressions.sh` — PASS.
- `python3 scripts/smoke_migration.py` — PASS.

Regresiones runtime verificadas: STOP, PAUSE/RESUME, TIMEOUT, TEACH, turn gate, restore, pérdida de ruta durante finishing y restore durante planning.

## Perfil CEFR-J

`found=2692/2746 exact_level_match=2599/2692`.

Las discrepancias no se corrigen de forma ciega porque una misma forma puede corresponder a sentidos/usos de distintos niveles. Quedan registradas para QA lingüístico.

## Lo que este QA no prueba

1. `assembleDebug`/AAB real: Android SDK no está instalado en este entorno.
2. Compatibilidad Bluetooth/MediaSession con cada firmware de audífonos.
3. Naturalidad acústica humana: el banco actual es sintético.
4. Eficacia pedagógica longitudinal: necesita usuarios y datos reales.
5. Revisión humana exhaustiva de todos los sentidos/glosas B2–C2.
