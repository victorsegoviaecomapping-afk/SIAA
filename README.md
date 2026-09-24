# SIAA — Sistema de Inglés Auditivo Adaptativo

SIAA es una aplicación Android nativa construida con **Kotlin**, **Jetpack Compose** y **Material Design 3**, diseñada para el aprendizaje auditivo adaptativo de inglés mediante control por audífonos manos libres y reproducción de audio offline.

## Características Principales

1. **Modo Manos Libres y Control por Audífonos (`EarbudCommandRouter`)**:
   - Interacción completa mediante botones multimedia de audífonos (`Play/Pause`, `Next`, `Previous`).
   - Perfiles preconfigurados para AirPods, Galaxy Buds, Sony Serie 1000X y mapeo personalizado.
   - Simulador interactivo en pantalla para probar eventos de hardware.
   - Enrutamiento calibrable para responder a opciones de ejercicio y escaleras de ayuda.

2. **Servicio Foreground con Pantalla Bloqueada (`SiaaPlaybackService`)**:
   - Servicio en primer plano para mantener la sesión de audio activa con la pantalla apagada o en el bolsillo.
   - Controles de medios integrados en la notificación persistente y pantalla de bloqueo.
   - Gestión adecuada de foco de audio (`AudioFocusRequest`).

3. **Currículo CEFR Extensivo (Pre-A1 a C2)**:
   - **Comprensión Auditiva (Listening)**: Audios de conversaciones cotidianas, instrucciones y discursos diplomáticos/académicos.
   - **Discriminación Fonológica**: Contrastes acústicos mínimos (/b/ vs /v/, /i:/ vs /ɪ/, flap-t, entonación y acento léxico).
   - **Frases y Colocaciones**: Expresiones fijadas, phrasal verbs y conectores de discurso contextualizados.
   - **Deletreo y Alfabeto**: Fonética elemental, números y supervivencia (Pre-A1).
   - Catálogo offline indexado en `audio_index.json` con cientos de clips de audio Vorbis (`.ogg`) integrados en assets.
   - Motor de Text-to-Speech (TTS) integrado como respaldo.

4. **Motor Pedagógico Adaptativo BKT (Bayesian Knowledge Tracing)**:
   - Estimación bayesiana continua de la probabilidad latente de dominio $P(L_t)$.
   - Ajustes según probabilidades de Guess ($P_G$), Slip ($P_S$) y Transición ($P_T$).
   - Modelo de retención de memoria espaciada con decaimiento exponencial (Half-life $R(t) = 2^{-\Delta t / S}$).

5. **Escalera de Ayuda Pedagógica (Help Ladder)**:
   - **Nivel 1**: Pista contextual sin revelar la respuesta.
   - **Nivel 2**: Modelo auditivo a velocidad reducida (0.75x).
   - **Nivel 3**: Transcripción completa y explicación lingüística.

## Estructura de la Aplicación

- `com.siaa.core.model`: Modelos de datos para perfiles de dispositivos (`DeviceProfile`), ítems curriculares (`ExerciseItem`), opciones y progreso del usuario.
- `com.siaa.core.algorithm`: Motor de estimación bayesiana (`BktEngine`) y cálculo de estabilidad de memoria.
- `com.siaa.core.runtime`: Máquina de estados pedagógica (`SessionStateMachine`), escalera de ayuda y eventos multimedia.
- `com.siaa.core.data`: Repositorio curricular (`CurriculumRepository`) y preferencias persistentes (`UserPreferencesRepository`).
- `com.siaa.app.media`: Enrutador de eventos de hardware (`EarbudCommandRouter`), controlador de audio (`AudioPlayerController`) y servicio en segundo plano (`SiaaPlaybackService`).
- `com.siaa.app.ui`: Pantallas en Jetpack Compose (`StudySessionScreen`, `CurriculumScreen`, `EarbudCalibrationScreen`, `ProgressStatsScreen`) y sistema de diseño Material 3 (`Theme.kt`, `Color.kt`, `Type.kt`).

## Requisitos de Compilación

- **Android SDK:** Compile SDK 36, Min SDK 26, Target SDK 36
- **JDK:** Java 17 / 21
- **Gradle:** 9.3.1
- **Android Gradle Plugin (AGP):** 9.1.1 con soporte nativo de Kotlin
- **Jetpack Compose:** Compose BOM 2024.09.00
