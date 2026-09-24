# Cobertura de contenido — SIAA v1.1

La v1.1 cambia el objetivo de “tener un KC por nivel” a tener un banco utilizable de enseñanza, recuperación y audio offline.

## Cobertura activa

Los recuentos exactos se leen desde `app/src/main/assets/content/manifest.json`. La generación v1.1 incorpora:

- más de mil lexemas bilingües activos;
- más de 400 puntos gramaticales adicionales derivados del Grammar Profile CEFR-J;
- funciones comunicativas A1–C2;
- listening por procesos con scripts originales graduados;
- fonología y connected speech;
- ejercicios estáticos de enseñanza/recuperación más variantes de significado, spelling y chunks generadas en instalación;
- audio offline exacto para **todos los lexemas activos** y todos los ejercicios `LISTENING_AB`.

## Qué significa “completo” aquí

La estructura A1–C2 está poblada y ejecutable, pero no se considera cerrada para siempre. Un curso C2 puede ampliarse indefinidamente en léxico, registro, dominios especializados y variedad de acentos. El criterio de release es cobertura suficiente y validada por competencia, no un número mágico de palabras.

## Audio

`assets/audio/audio_index.json` distingue explícitamente audio sintético. La app usa primero el clip pre-renderizado cuando existe y cae a Android TTS para texto dinámico. La sustitución progresiva por grabaciones humanas se puede hacer sin cambiar `LessonRuntime`.
