# QA report — SIAA Android v2.3 Release Candidate

Fecha: 2026-09-23

## Inventario validado

- 3,651 KCs.
- 7,175 relaciones.
- 9,473 ejercicios estáticos.
- 2,746 lexemas.
- ~9,133 variantes generables; ~18,606 actividades potenciales.
- 315 KCs formulaicos/multiword.
- 162 `LISTENING_AB`.
- 73 `PRON_DISCRIMINATION`.
- 62 KCs pragmáticos.
- 74 KCs Pre-A1.
- 3,648 assets de audio offline, 35,304,019 bytes indexados.

## QA de contenido

**PASS** `validate_project.py`: 3,651 KCs / 7,175 edges / 9,473 ejercicios / 2,746 lexemas; hashes de content pack coherentes y grafo duro sin ciclos detectados.

**PASS** `content_depth_audit.py`:

- listening por nivel: Pre-A1 25, A1 26, A2 23, B1 22, B2 22, C1 22, C2 22;
- pronunciación: 8 / 14 / 14 / 10 / 10 / 10 / 7;
- chunks: 28 / 43 / 48 / 52 / 50 / 47 / 47;
- pragmática: 8 / 7 / 8 / 8 / 10 / 11 / 10;
- transferencia léxica 2,746/2,746;
- pragmática objetiva 62/62;
- gramática teach/objective/transfer 425/425.

**PASS** `content_completeness_audit.py`: 0 warnings. Todos los KCs no léxicos cumplen la cobertura exigida por el gate y las 26 letras permanecen cubiertas.

## QA lingüístico v2.3

**PASS** `linguistic_qa_audit.py`: 411 KCs CEFR-J, exactamente 411 TEACH + 411 AB + 411 SELF_ASSESS = **1,233 actividades learner-facing**, 0 warnings.

Se eliminaron de los campos visibles al alumno tokens técnicos como `NP`, `AUX`, `CLAUSE`, `TENSE/ASPECT`, `PASSIVE:` y equivalentes. Se corrigieron además los cuatro distractores léxicos genéricos restantes. Los 411 targets usan 401 modelos ingleses distintos; las repeticiones restantes corresponden a patrones que comparten legítimamente el mismo ejemplo superficial.

## Audio

**PASS** `audio_asset_audit.py`:

- 3,648 assets indexados;
- vocab 2,746; listening 164; phonology 84; phrase 654;
- cobertura léxica 2,746/2,746;
- cobertura de assets para listening 162/162;
- cobertura de ejercicios phrase-audio 1,014/1,014.

**PASS** `audio_signal_audit.py`: 84 assets fonológicos/críticos inspeccionados con `ffprobe`; sin errores de duración/canales/provenance aplicable.

El banco bundled continúa con **0 grabaciones humanas activadas**. El smoke del pipeline de importación se ejecutó en una copia temporal: importó un asset de prueba, lo normalizó y cambió correctamente el índice a modo `mixed` sin alterar el proyecto real.

Los cuatro candidatos Wikimedia fueron revisados nuevamente en v2.3. El manifest distingue dos coincidencias directas actuales (`think`, `sheep`) de dos candidatos de componente (`this`, `ship`) para impedir reemplazos semánticamente incorrectos.

## Runtime, algoritmo y persistencia

- **PASS** `smoke_migration.py` — 15 statements.
- **PASS** `smoke_pure_kotlin.sh` — planner/algorithm smoke.
- **PASS** `smoke_content.sh` — content generator smoke.
- **PASS** `smoke_runtime.sh` — sesión de gramática completa y actualización de mastery.
- **PASS** `smoke_runtime_regressions.sh` — STOP, PAUSE, TIMEOUT, TEACH, turn gate, restore, finishing route y restore planning.
- **PASS** `smoke_spelling_runtime.sh`.

`profile_audit.py`: `found=2692/2746`, `exact_level_match=2599/2692`. Los no encontrados/discrepantes siguen siendo señal de revisión por sentido/función y no se reclasifican automáticamente.

## Release engineering v2.3

**PASS** `release_readiness_audit.py`:

- versionado 2.3.0 consistente y app `versionCode=23`;
- 411 KCs / 1,233 tareas learner-facing verificadas;
- test instrumentado de `EarbudCommandRouter` presente;
- hardening del servicio presente;
- workflows Android CI + emulador presentes;
- scripts/protocolos físicos presentes;
- pipeline y gate de audio humano presentes;
- 3,648 audios, 0 humanos bundled.

Los scripts Python compilan con `py_compile`, los scripts shell pasan `bash -n` y los tres workflows YAML parsean correctamente.

## Android que no puede certificarse en este entorno

Este entorno no contiene Android SDK ni `adb`; por tanto **no se afirma** que aquí se haya ejecutado `assembleDebug`, lint Android ni `connectedDebugAndroidTest`. La v2.3 deja esos pasos ejecutables en CI: Gradle 9.6.0 provisionado, Platform 37 / Build Tools 36.0.0 instalados en runner, build de app + instrumentation APKs y workflow de emulador.

Tampoco se marca como PASS la matriz teléfono × audífonos, llamadas, lockscreen/Bluetooth real, QA lingüístico humano ni grabaciones humanas: para ello se entrega `DEVICE_QA_PROTOCOL_V23.md`, captura ADB, protocolo lingüístico y pipeline de audio con provenance.

## Conclusión de QA

El artefacto pasa sus verificaciones **offline y automatizables**. El estado correcto es **Release Candidate preparado para CI + validación física/humana**, no “release pública certificada”.
