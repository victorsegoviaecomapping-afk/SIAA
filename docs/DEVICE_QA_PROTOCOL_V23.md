# Protocolo de QA físico — SIAA Android v2.3

Fecha: 2026-09-23

Este protocolo convierte la validación de audífonos, pantalla bloqueada y rutas de audio en una prueba repetible con evidencia. No puede sustituirse íntegramente por un emulador porque el mapeo de botones, Bluetooth, llamadas y políticas OEM dependen del teléfono y del firmware de los audífonos.

## Matriz mínima

Ejecutar al menos en:

- un teléfono Android 13–14;
- un teléfono Android 15–16 si está disponible;
- dos modelos de audífonos Bluetooth con controles multimedia diferentes;
- un caso con audífonos cableados/USB-C si se pretende soportarlo públicamente.

Registrar fabricante, modelo, versión Android, build y modelo/firmware de los audífonos.

## Preparación

1. Instalar un APK debug generado por `Android CI` o por Android Studio.
2. Conceder notificaciones cuando Android lo solicite.
3. Emparejar los audífonos y comprobar que son la salida de audio activa.
4. En SIAA abrir **Audífonos**, calibrar Play/Pause, Next y Previous si el firmware no usa los keycodes estándar.
5. Iniciar una sesión corta de 10–15 ítems.
6. En un equipo con `adb`, ejecutar `./scripts/device_qa_collect.sh evidencia/inicial`.

## Casos obligatorios

| ID | Caso | Resultado esperado |
|---|---|---|
| D01 | Iniciar sin salida privada | La sesión no empieza; el servicio foreground se retira y no habla por altavoz. |
| D02 | Iniciar con audífonos | Se crea la MediaSession y el audio se oye sólo por la salida privada. |
| D03 | Play/Pause | Se interpreta una única acción por `ACTION_DOWN`; `ACTION_UP` se ignora. |
| D04 | Next | Ejecuta la acción secundaria correspondiente al estado pedagógico actual. |
| D05 | Previous | Ejecuta back/repeat cuando está disponible; no produce dobles acciones. |
| D06 | Perfil calibrado | Los keycodes crudos se traducen según el perfil guardado, incluso si difieren de los defaults. |
| D07 | Bloquear pantalla | La sesión continúa controlable desde audífonos/MediaSession. |
| D08 | App a background | No se pierde el turno ni se duplica audio. |
| D09 | Desconectar Bluetooth durante habla | El runtime se pausa; no continúa por altavoz. |
| D10 | Reconectar Bluetooth | La reanudación requiere una ruta privada segura. |
| D11 | Pérdida de AudioFocus | La sesión se pausa de forma segura. |
| D12 | Llamada/notificación de voz | SIAA cede focus; no superpone voz con la llamada. |
| D13 | Stop desde notificación | Finaliza runtime, ancla, focus, foreground service y notificación. |
| D14 | Stop desde media key | Finaliza sin servicio huérfano. |
| D15 | Calibración sin audífonos | Se rechaza y se cierra el servicio si estaba inactivo. |
| D16 | Desconexión durante calibración | Se cancela calibración y se elimina el foreground service. |
| D17 | Matar proceso durante turno | El restore no duplica respuesta ni avanza indebidamente. |
| D18 | Reinicio tras sesión terminada | No restaura una sesión ya cerrada. |

## Sonda reproducible de botones

Con una sesión activa:

```bash
./scripts/device_media_probe.sh
```

La sonda envía Play/Pause, Next, Previous y Stop mediante `adb shell input keyevent` y deja el estado final de `dumpsys media_session`. Esto valida el pipeline Android/MediaSession; **no valida los taps físicos del firmware**, que deben comprobarse aparte.

## Evidencia

Después de cada escenario relevante:

```bash
./scripts/device_qa_collect.sh evidencia/D09-bluetooth-loss
```

El directorio incluye `getprop`, `dumpsys media_session`, audio, notificaciones, servicios, power/device-idle y logcat. Conservarlo junto al modelo de teléfono/audífonos y marcar PASS/FAIL en la tabla anterior.

## Criterio de salida

Una release pública no debe marcar “physical audio QA complete” hasta que D01–D18 hayan pasado en la matriz mínima y no exista ningún caso en que voz pedagógica migre accidentalmente al altavoz.
