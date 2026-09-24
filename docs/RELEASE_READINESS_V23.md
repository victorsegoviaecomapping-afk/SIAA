# Release readiness — SIAA Android v2.3

Fecha: 2026-09-23

## Cerrado dentro del artefacto

- Cobertura estructural Pre-A1–C2 de v2.2 preservada.
- 411 KCs gramaticales CEFR-J convertidos a presentación learner-facing.
- 1,233 actividades CEFR-J regeneradas con ejemplos naturales y etiquetas comprensibles.
- Gate lingüístico automático integrado a CI.
- Gate de señal/provenance para fonología y futuro audio humano.
- Pipeline de importación/normalización de audio humano sin cambiar IDs.
- Foreground service endurecido ante token inválido, falta/pérdida de ruta privada, denegación de AudioFocus y calibración fallida.
- Test instrumentado del `EarbudCommandRouter` para defaults, perfil calibrado y eventos ignorados.
- Workflow Android que instala SDK y Gradle 9.6.0 de forma independiente del `gradle-wrapper.jar` local.
- Workflow de emulador para tests instrumentados de app y migraciones Room.
- Scripts ADB para sonda de botones y captura de evidencia física.

## Verificable automáticamente en este entorno

Se ejecutan los gates Python, smokes Kotlin puros/runtime/contenido, migración SQLite, spelling, integridad de audio y manifest reproducible. El resultado exacto de la corrida final se registra en `docs/QA_REPORT_V23.md`.

## Dependencias externas que no deben falsearse como completadas

1. **Build Android local**: este entorno no tiene Android SDK/adb. El proyecto deja CI preparada para instalar el SDK, ejecutar `gradle test`, `lintDebug`, compilar APKs e instrumentation APKs.
2. **Emulador Android**: el workflow está preparado, pero necesita ejecutarse en GitHub Actions sobre la versión subida del proyecto.
3. **Audífonos físicos/OEM**: Bluetooth, taps reales, lockscreen, llamadas y route changes requieren hardware. El protocolo y la captura ADB están incluidos.
4. **Audio humano**: no se inventan ni redistribuyen grabaciones que no estén materializadas/licenciadas; se entrega pipeline, provenance y candidatos, pero el banco bundled sigue siendo sintético.
5. **QA lingüístico humano**: la limpieza automática no sustituye evaluación de hablantes/revisores. Se entrega protocolo estratificado y gates para impedir regresiones.
6. **Eficacia pedagógica longitudinal**: mastery/readiness/retention requieren datos de usuarios reales y no pueden certificarse con fixtures.

Por tanto, v2.3 es un **Release Candidate preparado para CI + validación física/humana**, no una afirmación de certificación física o lingüística externa.
