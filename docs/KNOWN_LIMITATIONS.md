# Limitaciones actuales v2.3

- Los 3,648 audios bundled son sintéticos pre-renderizados; el pipeline de audio humano existe, pero no debe presentarse como si ya se hubieran incorporado voces humanas.
- La cobertura Pre-A1–C2 es amplia y estructuralmente cerrada según gates, pero no pretende contener cada lexema, sentido o construcción posible del inglés.
- Los niveles CEFR de formas polisémicas pueden variar por sentido; las discrepancias de perfil deben revisarse por significado, no corregirse ciegamente.
- La presentación técnica de los 411 KCs CEFR-J fue humanizada automáticamente y pasa gates; todavía requiere muestreo humano de naturalidad/ambigüedad antes de una release pública.
- No se ejecutó `:app:assembleDebug` dentro de este entorno porque no hay Android SDK/adb. Los workflows incluyen instalación del SDK, tests, lint, build e instrumentación para ejecutarlos externamente.
- El emulador puede validar integración Android, pero no reproduce fielmente firmware Bluetooth, taps físicos, políticas OEM ni transiciones reales de audio.
- Media buttons, lockscreen, llamadas, pérdida de ruta y foreground service requieren la matriz física definida en `DEVICE_QA_PROTOCOL_V23.md`.
- Priors, pesos y umbrales adaptativos requieren calibración con interacciones longitudinales reales.
