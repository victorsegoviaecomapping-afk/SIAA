# Registro de fuentes / trazabilidad

La especificación científica completa está en `reference/SIAA_documento_maestro_v1_5_android_v0_7.pdf`.

## Capas implementadas y fundamento conceptual

| Capa | Implementación | Familia de fuente usada en diseño |
|---|---|---|
| Niveles/competencias | KC.cefr + checkpoints | CEFR Companion Volume / English Profile |
| Gramática | Form-Meaning-Use + grafo | English Grammar Profile; The Grammar Book; Processing Instruction |
| Vocabulario | estados multidimensionales + spelling | Nation, *Learning Vocabulary in Another Language* |
| Listening | KCs perceptivos + modos | Vandergrift & Goh; Field |
| Pronunciación | discriminación perceptiva | Celce-Murcia et al. |
| Estado de conocimiento | mastery + prereqs | Knowledge Tracing; Knowledge/Learning Spaces |
| Retención | half-life + ensemble | Settles & Meeder HLR y literatura de spacing |
| Diagnóstico | Q-matrix/CDM/MIRT | modelos cognitivo-diagnósticos e IRT |
| Secuenciación | utility + lookahead | POMDP tutoring / decision theory |
| Adaptación experimental | Thompson bandit | contextual bandit / N-of-1 design |

Los archivos de libros y papers proporcionados por el usuario no se redistribuyen en este ZIP.

## Fuentes de contenido incorporadas en v1.2

| Fuente | Uso | Redistribución dentro del ZIP |
|---|---|---|
| CEFR-J / Open Language Profiles | nivelación léxica/gramatical y candidaturas | datasets ya incluidos con su notice |
| Contexto (`Jason-Latz/contexto`) | overlay bilingüe conservador | sólo subconjunto filtrado/provenance; upstream MIT y datos derivados con licencias propias documentadas |
| FreeDict English–Spanish | corroboración aguas arriba del pack bilingüe | no se copia el diccionario completo; licencia CC BY-SA según provenance upstream |
| British Council / EAQUALS Core Inventory | diseño curricular | referencia, no copia íntegra |
| Wikimedia Commons | candidatos de pronunciación humana | por asset; aún no bundled en v1.2 |
| eSpeak | generación del banco sintético local | audio generado como artefacto SIAA |

`docs/OPEN_LEXICON_POLICY_V12.md` documenta por qué un diccionario abierto no se activa ciegamente como currículo.
