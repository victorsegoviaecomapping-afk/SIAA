# Estado de implementación v2.3

## Plataforma

Implementados: Android multimódulo, Compose, MediaSessionService, controles multimedia, calibración de audífonos, audio/TTS, Room, restore de sesión, learner model y planner adaptativo.

La v2.3 endurece el ciclo de vida del servicio: inicio sin salida privada, token inválido, denegación de AudioFocus y calibración fallida ya no deben dejar un foreground service activo. La pérdida de salida privada durante calibración la cancela y libera ancla/focus/servicio.

## Contenido

Estado **Content Complete Candidate**:

- 3,651 KCs y 7,175 relaciones;
- 9,473 ejercicios estáticos;
- 2,746 lexemas activos;
- 315 unidades formulaicas;
- 425 KCs gramaticales, incluidos 411 importados de CEFR-J ahora learner-facing;
- 1,233 ejercicios CEFR-J regenerados con modelos naturales;
- 162 listenings, 73 discriminaciones y 62 KCs pragmáticos;
- 74 KCs Pre-A1;
- 3,648 assets offline.

Todos los KCs no léxicos cumplen teach/objective/transfer según los gates actuales. Las 26 letras son enseñables/evaluables y no quedan distractores léxicos genéricos detectados.

## QA/Release engineering

Incluidos:

- `linguistic_qa_audit.py`;
- `audio_signal_audit.py`;
- importador de audio humano con provenance;
- test instrumentado del router de botones;
- CI Android con Gradle 9.6.0 provisionado;
- workflow de emulador;
- sondas ADB y protocolo físico.

## Pendiente externo

- ejecución efectiva del build/emulador en un entorno con Android SDK;
- matriz física teléfono × audífonos × OEM;
- QA lingüístico/acústico humano;
- materialización de grabaciones humanas licenciadas;
- calibración longitudinal del algoritmo con usuarios reales.
