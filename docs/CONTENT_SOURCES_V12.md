# Fuentes y procedencia del contenido v1.2

SIAA distingue entre **fuentes de diseño** y **material redistribuido**. Un PDF disponible gratuitamente no implica permiso automático para copiarlo dentro de la app.

## Fuentes de diseño/currículo

- Council of Europe — CEFR Companion Volume.
- CEFR-J / Open Language Profiles — perfiles de vocabulario y gramática.
- British Council / EAQUALS — Core Inventory for General English, usado como referencia de funciones, temas y progresión, no como banco copiado de ejercicios.
- Nation — vocabulario, frecuencia, profundidad de conocimiento y spacing.
- Lightbown & Spada — adquisición e instrucción de L2.
- Vandergrift & Goh / Field — procesos y metacognición de listening.
- Celce-Murcia et al. — pronunciación, stress, rhythm y connected speech.
- Larsen-Freeman / Celce-Murcia — gramática como form–meaning–use.
- U.S. Department of State American English — Teaching Pragmatics, como referencia abierta de funciones pragmáticas.

Los guiones de listening y los ejercicios añadidos en v1.2 son **contenido original SIAA** inspirado en esas categorías pedagógicas, no reproducciones de ejercicios de los libros.

## Léxico abierto

La expansión bilingüe activa usa un overlay conservador derivado de CEFR-J + un subconjunto filtrado del language pack español de `Jason-Latz/contexto`. El código del repositorio Contexto es MIT; su `THIRD_PARTY_NOTICES.md` documenta que el Spanish pack incorpora FreeDict y que los datos afectados se distribuyen bajo CC BY-SA 3.0, además de otras atribuciones upstream. Las entradas dudosas por polisemia no se activan automáticamente.

## Audio

El banco bundled v1.2 fue generado localmente con eSpeak y codificado como Ogg Vorbis. Cada asset tiene SHA-256 y procedencia en `audio/audio_index.json`.

Fuentes humanas abiertas evaluadas para fases posteriores incluyen Wikimedia Commons/Lingua Libre, Tatoeba (licencia por hablante/archivo) y LibriSpeech/OpenSLR. No se marca un clip como reutilizable hasta verificar la licencia del archivo concreto.

## Regla de authoring

Una entrada nueva no se activa únicamente porque aparezca en una lista: debe tener sentido objetivo, nivel defendible, glosa/definición, evidencia de uso, distractores razonables y provenance. Para audio humano se añade además licencia y atribución por asset.
