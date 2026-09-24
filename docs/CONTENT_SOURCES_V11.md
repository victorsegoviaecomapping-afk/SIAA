# Fuentes de contenido y procedencia — v1.1

SIAA separa **fuentes de referencia** de **contenido redistribuido**. Un recurso accesible en Internet no se copia automáticamente dentro de la app: se redistribuye sólo cuando sus términos lo permiten; de lo contrario se usa como referencia para producir ejercicios originales.

## Datos incorporados

### CEFR-J / Open Language Profiles
- https://github.com/openlanguageprofiles/olp-en-cefrj
- Se incluyen el Vocabulary Profile, Grammar Profile y el perfil C1/C2 ya presentes en `assets/reference/cefrj/`.
- El README de origen indica que los perfiles CEFR-J pueden usarse sin cargo para investigación y uso comercial con cita; el perfil Octanove C1/C2 está bajo CC BY-SA 4.0.
- Uso en SIAA: nivelación léxica, activación de 411 items gramaticales con nivel defendible y trazabilidad curricular.

### Contexto Spanish language pack
- https://github.com/Jason-Latz/contexto
- Licencia del repositorio: MIT.
- Uso en SIAA: overlay derivado y conservador de traducciones inglés→español cruzadas con CEFR-J. El proyecto no copia el paquete completo; sólo conserva las entradas seleccionadas en `reference/open_lexicon/contexto_cefr_overlay.json`.

## Fuentes abiertas/oficiales usadas como referencia de authoring

### CEFR Companion Volume
- Documento ya aportado al proyecto.
- Uso: descriptores globales y por actividad, especialmente listening, mediación, pragmática y niveles C.

### British Council / EAQUALS — Core Inventory for General English
- https://www.teachingenglish.org.uk/article/british-council-eaquals-core-inventory-general-english-0
- Uso: mapa de funciones, gramática, discurso y temas A1–C1. Los prompts/frases de SIAA son redacción original.

### American English / U.S. Department of State — Teaching Pragmatics
- https://americanenglish.state.gov/resources/teaching-pragmatics
- Uso: diseño de tareas de requests, openings/closings, disagreement, politeness, backchannels y discourse markers. No se copian bancos de ejercicios completos.

### Paul Nation — recursos de vocabulario / BNC-COCA
- https://www.wgtn.ac.nz/lals/resources/paul-nations-resources/vocabulary-lists
- Uso: principios de frecuencia, cobertura y priorización; además del libro de Nation proporcionado por el usuario.

### English Grammar: Notes for Spanish Speakers
- https://ria.asturias.es/RIA/handle/123456789/14576
- Recurso sujeto a Creative Commons según el repositorio institucional.
- Uso: contraste L1→L2 y diseño de futuras misconceptions específicas de hispanohablantes.

## Audio

La v1.1 contiene un banco **pre-renderizado y offline de voz sintética** generado con eSpeak y comprimido en Ogg Vorbis. Esto resuelve reproducibilidad y disponibilidad sin red, pero **no se etiqueta como audio humano**.

Para evaluación fonética sensible a acento/prosodia se mantiene como siguiente capa una colección humana con licencia explícita (Wikimedia Commons/Lingua Libre u otras fuentes compatibles), conservando autor, licencia y URL por clip.
