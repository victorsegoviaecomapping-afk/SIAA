# Plan de audio humano — SIAA v1.2

## Estado actual

El banco incluido en v1.2 contiene **1,184 assets offline sintéticos** generados con eSpeak. Es suficiente para funcionamiento offline, vocabulario y buena parte de las pruebas de integración, pero no se considera patrón oro para fonética fina, acentos, connected speech ni listening auténtico avanzado.

## Regla de sustitución

El runtime usa una clave textual/ID de audio. Esto permite reemplazar un clip sintético por uno humano sin cambiar el algoritmo ni el ejercicio. La sustitución se hará sólo cuando cada archivo tenga:

1. licencia comprobada a nivel de archivo;
2. autor y URL de procedencia;
3. acento/variante documentados;
4. transcripción exacta;
5. revisión acústica y lingüística;
6. mapeo a KC/ejercicio;
7. checksum SHA-256.

## Primeros candidatos verificados

`app/src/main/assets/reference/human_audio/candidates.json` registra cuatro pronunciaciones de Wikimedia Commons: `this`, `think`, `ship` y `sheep`. `this` figura como dominio público; `think`, `ship` y `sheep` están multi-licenciados, incluyendo CC BY-SA 3.0/GFDL. **No están incluidos todavía como audio ejecutable**, porque el pipeline de esta sesión no pudo materializar de forma reproducible los binarios OGG; sólo se registra su procedencia verificada para una futura importación.

## Fuentes candidatas adicionales

- Wikimedia Commons / Lingua Libre: pronunciaciones aisladas y algunas frases, licencia por asset.
- Tatoeba: frases y audio, pero la licencia de audio depende del hablante/archivo y se debe comprobar individualmente.
- LibriSpeech / OpenSLR: útil para listening largo de lectura en inglés; no sustituye conversaciones espontáneas.
- Grabaciones SIAA propias: opción preferida para minimal pairs, reducciones y diálogos controlados, porque permite especificar acento, velocidad y target fonético con precisión.

## Prioridad de producción

La primera ola humana debe cubrir: /ɪ/–/iː/, /b/–/v/, /θ/–/ð/, terminaciones `-s`/`-ed`, schwa, linking, elisión, stress léxico, nuclear stress, entonación de stance y diálogos A1–B2. Después se amplía a acentos y discurso C1–C2.
