# SIAA Android

[![Validación](https://github.com/segesc/siaa-android/actions/workflows/validate.yml/badge.svg)](https://github.com/segesc/siaa-android/actions/workflows/validate.yml)

Versión fuente **2.3.0 Release Candidate** · Kotlin · Jetpack Compose · Android

[Primeros pasos](START_HERE.md) · [Documentación](docs/README.md) · [Release readiness](docs/RELEASE_READINESS_V23.md) · [Cambios](CHANGELOG.md)

**SIAA — Sistema de Inglés Auditivo Adaptativo** es una app Android offline-first pensada para estudiar inglés con el teléfono guardado y la pantalla apagada. Su interacción principal usa audio y comandos multimedia de audífonos (`Play/Pause`, `Next`, `Previous`) mediante Media3/MediaSession.

## Estado v2.3

La capa curricular estructural está en estado **Content Complete Candidate** y la v2.3 añade hardening de release:

- **3,651 KCs**, **7,175 relaciones**, **9,473 ejercicios estáticos**;
- **2,746 lexemas** y ~**9,133 variantes** generables;
- **315 unidades formulaicas/multiword**;
- **162 listenings**, **73 discriminaciones de pronunciación** y **62 KCs pragmáticos**;
- **74 KCs Pre-A1**;
- **3,648 audios offline**;
- 411 KCs gramaticales CEFR-J y 1,233 actividades convertidos a presentación learner-facing;
- gates de completitud, profundidad, lingüística, audio, migraciones y runtime;
- CI Android reproducible con Gradle 9.6.0 + SDK instalado en Actions;
- workflow de emulador para pruebas instrumentadas;
- protocolo y scripts ADB para validación física de audífonos/lockscreen/rutas;
- pipeline de sustitución selectiva por audio humano con provenance obligatoria.

El audio bundled continúa siendo sintético. La validación con audífonos físicos, QA humano y eficacia longitudinal siguen siendo dependencias externas y no se presentan como completadas.

## Arquitectura

- `:app` — UI Compose, `MediaSessionService`, routing de botones, preferencias y composición de dependencias.
- `:core:model` — modelos de dominio.
- `:core:algorithm` — BKT, half-life/memoria, Knowledge Space, Q-matrix/CDM, MIRT, SMC, planner, lookahead y Thompson Sampling restringido.
- `:core:runtime` — máquina de estados pedagógica, help ladder, restore y protocolo de sesión.
- `:core:data` — Room, content packs versionados, repositorio local e historial.
- `:core:audio` — TTS, audio focus, salida privada y protección de rutas.
- `:core:content` — validación curricular y generación controlada de variantes.

## Build

Toolchain fijado:

- JDK 17
- compileSdk 37
- targetSdk 36
- Gradle 9.6.0
- AGP 9.4.0

Con wrapper disponible:

```bash
./gradlew clean :app:assembleDebug
```

Si el ZIP no contiene `gradle-wrapper.jar`, consulta `docs/BUILD_AND_RUN.md`. En CI el build no depende de ese binario: `gradle/actions/setup-gradle` provisiona Gradle 9.6.0 y el workflow instala el SDK requerido.

## QA local sin Android SDK

```bash
python scripts/validate_project.py
python scripts/profile_audit.py
python scripts/audio_asset_audit.py
python scripts/audio_signal_audit.py
python scripts/content_depth_audit.py
python scripts/content_completeness_audit.py
python scripts/linguistic_qa_audit.py
python scripts/release_readiness_audit.py
python scripts/smoke_migration.py
./scripts/smoke_pure_kotlin.sh
./scripts/smoke_content.sh
./scripts/smoke_runtime.sh
./scripts/smoke_runtime_regressions.sh
./scripts/smoke_spelling_runtime.sh
python scripts/verify_release.py
```

Para el teléfono físico, seguir `docs/DEVICE_QA_PROTOCOL_V23.md`. Para sustituir audio sintético, usar `docs/HUMAN_AUDIO_PIPELINE_V23.md`.

## Automatización

- `.github/workflows/validate.yml` — gates rápidos de contenido/integridad.
- `.github/workflows/android-ci.yml` — SDK + tests + lint + compilación de APK e instrumentation APKs.
- `.github/workflows/android-device-tests.yml` — pruebas instrumentadas en emulador Android.

## Estructura

```text
app/                Aplicación Android y assets
core/               Modelo, algoritmo, runtime, datos, audio y contenido
docs/               Arquitectura, QA, protocolos y estado de release
gradle/             Catálogo de versiones y configuración del wrapper
reference/          Documento maestro
research_sources/   Procedencia de fuentes
scripts/            QA, importación, validación y herramientas ADB
tools/              Smoke tests Kotlin
.github/            Workflows CI
```

## Licencia y referencias

El repositorio conserva los avisos de las referencias incluidas. No se asigna automáticamente una licencia nueva a material externo. Toda grabación humana debe conservar licencia, origen y atribución/release aplicable antes de activarse.
