# Fuentes de contenido — v1.6

## Contexto Spanish language pack

- Proyecto: `Jason-Latz/contexto`.
- Archivo de origen: `public/language-packs/es.json`.
- Licencia del código Contexto: MIT; el Spanish pack documenta datos derivados bajo CC BY-SA 3.0 y otras atribuciones upstream.
- Uso SIAA: glosas inglés→español de entradas `eligible=true`, `confidence=high`.
- Selección incluida: `app/src/main/assets/reference/contexto/contexto_cefr_v16_selected.json`.
- Aviso/licencia: `app/src/main/assets/reference/contexto/README.md` y `LICENSE`.

## CEFR-J / Open Language Profiles

- Uso: validación independiente del nivel CEFR de cada headword.
- Archivos: `cefrj-vocabulary-profile-1.5.csv` y `octanove-vocabulary-profile-c1c2-1.0.csv`.
- CEFR-J determina el nivel; Contexto no se usa para asignar CEFR.

## Fuentes metodológicas

Las fuentes del documento maestro —CEFR, Nation, Vandergrift & Goh, Celce-Murcia, Larsen-Freeman/Celce-Murcia, BKT/HLR/POMDP/KST, entre otras— siguen guiando qué dimensión del conocimiento se entrena y cómo se secuencia. No se redistribuyen libros comerciales.

## Audio

El banco offline de esta versión contiene audio sintético pre-renderizado mediante eSpeak/Vorbis. Se conserva explícitamente el campo `synthetic=true` y no se presenta como habla humana.
