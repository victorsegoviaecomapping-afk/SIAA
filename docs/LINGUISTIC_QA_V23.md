# QA lingüístico v2.3

## Cambio principal

Los 411 KCs gramaticales importados desde el perfil CEFR-J conservan su `form` técnico para trazabilidad, pero ya no exponen esa notación al alumno. Cada KC dispone ahora de:

- nombre pedagógico en español;
- modelo natural en inglés;
- actividad TEACH;
- reconocimiento A/B con distractor del mismo nivel;
- transferencia SELF_ASSESS.

En total se regeneraron 1,233 actividades CEFR-J learner-facing. Ejemplos de transformación:

- `TENSE/ASPECT: PAST ...` → **pasado simple** + “She worked yesterday.”;
- `PASSIVE: PRESENT PERFECT` → **voz pasiva: presente perfecto** + “The report has been checked.”;
- `IMPERATIVE ... NEG` → **imperativo negativo** + “Do not open the door.”;
- patrones con `NP`, `OBJECT`, `CLAUSE`, etc. mantienen la forma fuente sólo en metadatos, no en prompts/opciones.

## Gate automático

`scripts/linguistic_qa_audit.py` falla si detecta:

- notación técnica CEFR-J en campos learner-facing;
- notas de fuente/CJK o placeholders de autoría;
- tareas CEFR-J sin modelo en inglés;
- A/B sin clave o con opciones duplicadas;
- distractores léxicos genéricos o idénticos a la respuesta.

El gate se ejecuta en `validate.yml` y `android-ci.yml`.

## Revisión humana todavía necesaria

La automatización puede detectar contaminación técnica y muchas incoherencias estructurales, pero no demuestra por sí sola naturalidad nativa, adecuación pragmática o dificultad perceptiva. Para release pública se recomienda una revisión estratificada, no una lectura casual:

1. extraer muestras por CEFR × dominio × tipo de actividad;
2. doble revisión independiente de naturalidad, precisión, ambigüedad y nivel;
3. adjudicar desacuerdos;
4. corregir el ítem fuente y volver a ejecutar todos los gates;
5. registrar tasa de defectos por estrato para decidir si ampliar la muestra.

Los ítems de fonología y listening con contrastes finos deben revisarse junto con su audio, no sólo como texto.
