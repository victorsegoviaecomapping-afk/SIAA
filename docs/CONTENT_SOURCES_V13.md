# Fuentes y procedencia — v1.3

## Expansión A1 entrada por entrada

La ampliación A1 usa **CEFR-J / Open Language Profiles** para verificar que cada headword esté soportado en A1. Las traducciones españolas, chunks breves y prompts añadidos en esta pasada son authoring original SIAA y no copias de un diccionario o libro.

La rutina `scripts/expand_content_v13_a1.py` rechaza cualquier entrada que no aparezca como A1 en el perfil CEFR-J y evita duplicados.

## Audio

Después de añadir los lexemas, `scripts/generate_audio_bank.py` vuelve a renderizar sólo los assets faltantes y actualiza `audio_index.json`. El audio sigue siendo sintético pre-renderizado con eSpeak/Ogg; no se presenta como voz humana.

## Próximas pasadas

El mismo criterio se aplica secuencialmente a A2, B1, B2, C1 y C2: activar cada entrada sólo cuando el nivel, glosa/sentido y uso estén suficientemente defendidos.
