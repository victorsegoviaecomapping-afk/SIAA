# Changelog

## 2.3.0 - 2026-09-23

- 411 KCs gramaticales CEFR-J convertidos a nombres/modelos learner-facing; 1,233 actividades regeneradas.
- Nuevo gate `linguistic_qa_audit.py` para impedir filtración de notación técnica, placeholders y distractores inválidos.
- Corregidos los últimos 4 distractores léxicos genéricos.
- `SiaaPlaybackService` endurecido ante inicio inseguro, token inválido, AudioFocus fallido y pérdida de ruta durante calibración.
- Test instrumentado de `EarbudCommandRouter`.
- CI Android independiente del `gradle-wrapper.jar` local y workflow de emulador para instrumentación.
- Scripts ADB para sonda de botones y captura de evidencia física.
- Pipeline `import_human_audio.py` + `audio_signal_audit.py` con provenance obligatoria y normalización FFmpeg.
- Protocolos de QA físico, lingüístico y audio humano, más matriz explícita de release readiness.

## 2.2.0 - 2026-09-22

- Fundación Pre-A1 ampliada: supervivencia, pragmática, listening y pronunciación.
- 26/26 letras con teach + evaluación objetiva + transferencia.
- Cierre de KCs huérfanos de ortografía/fonología y transferencias legacy.
- Distractores léxicos reforzados por nivel CEFR y categoría gramatical.
- 3,651 KCs, 7,175 edges, 9,473 ejercicios estáticos y 315 unidades formulaicas.
- 3,648 audios offline regenerados.
- Nuevo gate `content_completeness_audit.py` integrado en CI.
- Metadatos, documentación y release manifest actualizados a v2.2.0.

## 0.8.0

- Runtime con estado `FINISHING`, cancelación segura y protección por turno/sesión.
- Preflight de salida de audio privada y cierre seguro ante pérdida de ruta/foco.
- Restore idempotente basado en `EXERCISE_SELECTED`; se corrige muerte de proceso durante `PLANNING_NEXT`.
- Progreso CEFR sobre conjunto evaluable D_l y checkpoints con transferencia/novedad.
- Readiness como restricción de elegibilidad y fase pedagógica explícita.
- Autoevaluación tratada como evidencia ruidosa/personalizada.
- Room schema v3, migración 2→3, snapshots de schema y test instrumentado de migración.
- Calibración de keycodes raw no estándar, hardening de acciones internas del servicio y UI sin truncado silencioso.
- Content pack 0.8.0: 138 ejercicios estáticos y fingerprint de versión de generador.
- Documento maestro actualizado mediante addendum técnico v0.8.


## 0.7.0
- Nuevo módulo `:core:content` con validador y generador controlado de variantes.
- Content pack versionado y reemplazable sin borrar el estado del alumno.
- 30 lexemas curados generan ~156 actividades adicionales sobre 134 estáticas.
- Integración efectiva de Q-matrix en la atribución de evidencia por KC.
- Memory ensemble usado por el planificador y actualizado online.
- Particle filter/SMC activado como segunda estimación de mastery y half-life.
- Thompson Sampling restringido activado para explorar tipos de actividad ya válidos.
- Wheel-spinning conectado a boost de remediación y penalización de práctica repetitiva.
- Help ladder de cuatro niveles: repetir, lento, segmentar, spelling.
- `SpeechPort` soporta velocidad relativa por intervención.
- Preferencias persistentes: máximo de actividades, anuncios de controles y explicaciones.
- Modo de calibración de audífonos independiente de una lección.
- Ancla silenciosa Media3 en loop para mantener la sesión multimedia activa durante TTS.
- Historial de sesiones y mapa curricular con readiness/unlock en Compose.
- Datos CEFR-J abiertos incorporados como referencia y script de auditoría.
- `smoke_content.sh` y validación de ciclos duros en prerrequisitos.

## 0.6.0
- Reestructuración multimódulo completa.
- Room y seeding de 87 KCs / 197 edges / 134 ejercicios.
- Runtime adaptativo genérico.
- MediaSessionService y media button router.
- Modos Adaptive/Vocabulary/Spelling/Grammar/Listening/Pronunciation.
- BKT, HLR, memory ensemble, Q-matrix, MIRT, particle filter, POMDP lookahead.
- UI Compose para inicio/progreso/audífonos/ajustes.
- Vocabulario con ortografía y deletreo.
- Scripts de validación y smoke test.

## v0.7-final
- Corregida la firma de `HomeScreen` para compilar con su llamada actual.
- Referencia actualizada al Documento Maestro v1.5.
- Añadidos `TECH_STACK.md`, `IMPLEMENTATION_STATUS.md` y verificación de release.
- Añadido workflow CI para SDK Android, tests y `assembleDebug`.
- Añadida política de sesión y política explícita de migraciones de datos.

## 1.2.0
- 72 nuevos scripts de listening originales A1–C2; total 102 LISTENING_AB.
- 39 nuevas discriminaciones fonológicas same/different; total 41 PRON_DISCRIMINATION.
- Reconocimiento pragmático objetivo para todos los KCs pragmáticos.
- Transferencia mental añadida a los 1,029 lexemas activos.
- Banco de audio offline regenerado: 1,184 assets, 11.5 MB aprox.
- Spelling A/B ahora deletrea las opciones letra por letra y el feedback revela la forma correcta por nombres ingleses de letras.
- Evento PREDICTION_RECORDED para evaluación prequential.
- Planner: Q-matrix + MIRT + posterior de partículas + penalización de riesgo de cola inferior.
- POMDP/MPC depth 2.
- Toolchain actualizado a AGP 9.4.0 / Gradle 9.6.0 / JDK 17.

## 1.3.0
- Expansión léxica A1 revisada elemento por elemento.
- +269 lexemas A1; total A1 = 530, total global = 1,298.
- +538 ejercicios estáticos (enseñanza + transferencia para nuevas entradas).
- Banco de audio regenerado a 1,453 assets offline.
- Fuente/nivel validado con CEFR-J; glosas y chunks nuevos son authoring original SIAA.

## 1.4.0
- Expansión A2 revisada entrada por entrada: +294 lexemas.
- A2 activo: 493 lexemas.
- Audio local regenerado para cada nueva entrada.

## 1.5.0
- Expansión B1 revisada entrada por entrada: +430 lexemas.
- B1 activo: 664 lexemas.
- Total global: 2,022 lexemas, 2,557 KCs, 6,303 ejercicios estáticos.
- Banco de audio: 2,177 assets offline.

## 1.6.0 - 2026-09-21
- 261 lexemas nuevos activados, principalmente C1/C2.
- 2,283 lexemas activos totales.
- 2,818 KCs y 6,825 ejercicios estáticos.
- Contexto Spanish language pack (MIT) añadido como fuente abierta de glosas de alta confianza.
- Cada entrada nueva se valida contra CEFR-J/Open Language Profiles antes de activarse.
- Banco de audio offline regenerado a 2,438 assets.
- Añadidos provenance, licencia y documentación de cobertura v1.6.
- Segunda y tercera tanda v1.6: B2/C1/C2 ampliados hasta 373/344/342 lexemas respectivamente.
- Total v1.6 final de esta iteración: 2,746 lexemas, 3,281 KCs, 7,751 ejercicios estáticos y 2,901 audios offline.

## 2.0.0 - 2026-09-21
- Elevado el content pack a 3,520 KCs, 6,952 relaciones y 8,934 ejercicios estáticos.
- Añadidos 202 KCs multiword/formulaicos A1–C2 con phrasal verbs, collocations, discourse markers y fórmulas académicas/pragmáticas.
- Listening aumentado a 138 tareas, con textos extendidos B1–C2 y prompts metacognitivos de planificación, monitoreo y evaluación.
- Pronunciación ampliada a 65 discriminaciones y 40 KCs fonológicos.
- Pragmática ampliada a 54 KCs con reconocimiento objetivo y transferencia.
- Todos los 425 KCs gramaticales pasan a tener TEACH + discriminación objetiva + SELF_ASSESS de transferencia.
- Banco offline regenerado a 3,351 assets: vocabulario, listening, fonología y frases formulaicas.
- Endurecido `content_depth_audit.py` para impedir regresiones de densidad por nivel.
- Corregida la documentación de licencia del Spanish language pack de Contexto: se preservan las obligaciones share-alike documentadas por su upstream.

## 2.1.0 - 2026-09-22
- Segunda capa formulaica A1–C2.
- +85 KCs multiword no duplicados.
- +255 ejercicios estáticos: enseñanza, discriminación y transferencia.
- Nuevos phrasal verbs, collocations, verb/adjective patterns, discourse markers y fórmulas académicas C1/C2.
- Banco de audio actualizado a 3,521 assets, incluidos 560 estímulos/frases formulaicas.
- Auditoría final deliberadamente aplazada a la siguiente interacción.
