# SIAA bundled audio v1.2

Este directorio contiene audio pre-renderizado offline para vocabulario, listening y algunos objetivos fonológicos.

- Motor de esta generación: eSpeak.
- Codec: Ogg Vorbis mono.
- El contenido se marca como **synthetic=true** en `audio_index.json`.
- No debe describirse como grabación humana ni usarse como gold standard para evaluar acentos finos.
- `BundledAudioSpeechPort` reproduce el asset exacto cuando existe y usa Android TTS como fallback.

El banco puede sustituirse clip por clip por audio humano licenciado manteniendo la misma clave de texto y el mismo `rel` o actualizando el índice.


El plan para incorporar grabaciones humanas verificadas está en `docs/HUMAN_AUDIO_PLAN_V12.md`; los candidatos aún no bundled viven en `app/src/main/assets/reference/human_audio/candidates.json`.
