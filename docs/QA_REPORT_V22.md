# QA report — SIAA Android v2.2 Content Complete Candidate

Fecha: 2026-09-22

## Contenido validado en este artefacto

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
- 3,648 assets de audio offline.

## Gates de contenido

Se ejecutaron y pasaron dos niveles de control:

- `content_depth_audit.py`: umbrales mínimos por CEFR y cobertura de gramática/vocabulario/pragmática.
- `content_completeness_audit.py`: cobertura teach/objective/transfer para KCs no léxicos, alphabet completo, sanidad A/B, placeholders y fundación Pre-A1.

Profundidad validada: Pre-A1 25/8/28/8; A1 26/14/43/7; A2 23/14/48/8; B1 22/10/52/8; B2 22/10/50/10; C1 22/10/47/11; C2 22/7/47/10 para listening/pronunciation/chunks/pragmatics respectivamente.

## Audio

`audio_asset_audit.py` — **PASS**: 3,648 assets: vocab 2,746; listening 164; phonology 84; phrase 654; bytes indexados 35,304,019. Los 162 `LISTENING_AB` y las actividades con audio de frase resuelven a assets indexados.

## Smokes ejecutados

- `validate_project.py` — **PASS**.
- `content_depth_audit.py` — **PASS**.
- `content_completeness_audit.py` — **PASS**, 0 warnings.
- `audio_asset_audit.py` — **PASS**.
- `smoke_migration.py` — **PASS**.
- `smoke_pure_kotlin.sh` — **PASS** (algorithm smoke).
- `smoke_content.sh` — **PASS**.
- `smoke_runtime.sh` — **PASS**.
- `smoke_runtime_regressions.sh` — **PASS**: STOP, PAUSE, TIMEOUT, TEACH, turn gate, restore, finishing route y restore planning.
- `smoke_spelling_runtime.sh` — **PASS**.

`profile_audit.py` mantiene `found=2692/2746` y `exact_level_match=2599/2692`; las discrepancias se conservan para QA lingüístico y no se corrigen ciegamente porque una forma puede tener sentidos/funciones de distintos niveles. `verify_release.py` se ejecuta al cerrar el paquete contra `FILES_SHA256.txt`.

## Lo que este QA no sustituye

- build APK/AAB real si el entorno local no dispone de Android SDK/Gradle bootstrap;
- compatibilidad física con firmwares de audífonos;
- QA lingüístico/acústico humano exhaustivo;
- medición longitudinal de eficacia pedagógica/calibración del StudentModel.
