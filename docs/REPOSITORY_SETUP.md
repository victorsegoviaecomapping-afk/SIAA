# Repository setup / baseline

Baseline activa: **v1.2.0**.

## Contenido

- 1,564 KCs.
- 6,652 relaciones.
- 4,317 ejercicios estáticos.
- 1,029 lexemas activos.
- ~3,153 variantes generadas al sembrar Room.
- 1,184 assets de audio offline.

## Validación local esperada

```bash
python -X utf8 scripts/validate_project.py
python -X utf8 scripts/audio_asset_audit.py
python -X utf8 scripts/content_depth_audit.py
python -X utf8 scripts/profile_audit.py
bash scripts/smoke_pure_kotlin.sh
bash scripts/smoke_content.sh
bash scripts/smoke_runtime.sh
bash scripts/smoke_runtime_regressions.sh
bash scripts/smoke_spelling_runtime.sh
python -X utf8 scripts/smoke_migration.py
```

`profile_audit.py` es informativo: una discrepancia con CEFR-J no demuestra por sí sola que el nivel SIAA sea incorrecto, porque una misma forma puede tener sentidos/usos en distintos niveles.

## Android

El CI instala Android API 37, usa JDK 17 y construye `:app:assembleDebug` con AGP 9.4.0 / Gradle 9.6.0. La build física y la compatibilidad con audífonos deben verificarse en hardware real.
