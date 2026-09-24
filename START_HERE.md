# START HERE — SIAA Android v2.3 Release Candidate

## 1. Verifica el paquete

Sin Android SDK puedes ejecutar todos los gates offline:

```bash
python scripts/validate_project.py
python scripts/content_depth_audit.py
python scripts/content_completeness_audit.py
python scripts/linguistic_qa_audit.py
python scripts/audio_asset_audit.py
python scripts/audio_signal_audit.py
python scripts/release_readiness_audit.py
python scripts/smoke_migration.py
./scripts/smoke_pure_kotlin.sh
./scripts/smoke_content.sh
./scripts/smoke_runtime.sh
./scripts/smoke_runtime_regressions.sh
./scripts/smoke_spelling_runtime.sh
python scripts/verify_release.py
```

## 2. Compila Android

Usa JDK 17, Platform 37 y Gradle 9.6.0. El workflow `Android CI` instala el SDK/Gradle y compila APK + APKs instrumentados. Localmente consulta `docs/BUILD_AND_RUN.md`.

## 3. Ejecuta instrumentación

`android-device-tests.yml` ejecuta en emulador los tests de la app y las migraciones Room. Incluye el test de `EarbudCommandRouter` para defaults, perfil calibrado y eventos ignorados.

## 4. Prueba física

La primera prueba física debe durar 20–30 minutos con pantalla bloqueada y teléfono guardado. Debe comprobarse que ninguna pérdida de Bluetooth/AudioFocus provoca voz por altavoz ni deja un foreground service huérfano.

Sigue exactamente `docs/DEVICE_QA_PROTOCOL_V23.md` y captura evidencia con:

```bash
./scripts/device_qa_collect.sh evidencia/inicial
./scripts/device_media_probe.sh
```

## 5. Qué probar del contenido

- Pre-A1: saludos, identidad, supervivencia, instrucciones, números y spelling.
- Alphabet/spelling: 26/26 letras con enseñanza, evaluación y transferencia.
- Vocabulary: significado, sonido, ortografía y transferencia.
- Grammar: comprobar que ya no aparecen etiquetas como `NP`, `AUX`, `CLAUSE`, `TENSE/ASPECT`; el alumno debe oír ejemplos naturales.
- Listening: muestrear Pre-A1, A1, B1, C1 y C2.
- Pronunciation: contrastes perceptivos sin mirar la pantalla.
- Formulaic language y pragmatics: naturalidad y adecuación contextual.

## 6. Audio humano

Los **3,648 assets offline bundled siguen siendo sintéticos**. La v2.3 incluye el pipeline para reemplazar selectivamente archivos manteniendo sus IDs. Ver `docs/HUMAN_AUDIO_PIPELINE_V23.md`.

## 7. Criterio de release pública

No marcar como completadas la matriz de hardware, QA lingüístico humano ni audio humano mientras no exista evidencia real. El detalle está en `docs/RELEASE_READINESS_V23.md` y la corrida automática final en `docs/QA_REPORT_V23.md`.
