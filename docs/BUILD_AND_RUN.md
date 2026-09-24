# Build & Run — v2.3

## Toolchain fijado

- compileSdk 37
- targetSdk 36
- minSdk 26
- JDK 17
- AGP 9.4.0
- Gradle 9.6.0
- Kotlin/Compose compiler 2.2.10
- Media3 1.11.1
- Room 2.8.5

## Build local

Con Android SDK Platform 37 y Build Tools 36.0.0 instalados:

```bash
./gradlew clean test lintDebug :app:assembleDebug
```

APK esperado:

`app/build/outputs/apk/debug/app-debug.apk`

Si falta el binario `gradle-wrapper.jar`, puedes ejecutar el bootstrap incluido o usar una instalación local de Gradle 9.6.0. La ruta CI no depende de ese binario.

## CI reproducible

`.github/workflows/android-ci.yml`:

1. fija JDK 17;
2. provisiona Gradle 9.6.0;
3. instala Platform 37, Build Tools 36.0.0 y platform-tools;
4. ejecuta todos los gates/smokes offline;
5. ejecuta `gradle test lintDebug`;
6. compila APK de app e instrumentation APKs;
7. publica los APKs como artefactos.

`.github/workflows/android-device-tests.yml` ejecuta los tests instrumentados en emulador API 36.

## Prueba física

Requisitos mínimos:

- teléfono Android compatible;
- TTS inglés y español si se usa fallback TTS;
- audífonos Bluetooth o salida privada soportada;
- permiso de notificaciones en Android 13+;
- `adb` opcional pero recomendado para capturar evidencia.

Seguir `DEVICE_QA_PROTOCOL_V23.md`.
