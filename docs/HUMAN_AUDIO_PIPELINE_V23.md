# Pipeline de audio humano v2.3

## Estado

El paquete mantiene 3,648 audios offline pre-renderizados y actualmente sintéticos. La v2.3 prepara una sustitución selectiva por grabaciones humanas **sin cambiar los IDs ni romper las referencias del contenido**.

Cuatro candidatos de Wikimedia Commons con procedencia/licencia documentada permanecen en `app/src/main/assets/reference/human_audio/candidates.json`. No se incluyen sus binarios en este artefacto. El manifest distingue ahora `importReady`: `think` y `sheep` tienen clave exacta actual; `this` y `ship` son candidatos de componente/referencia y **no deben forzarse** sobre un asset multi-token.

## Importación

Crear un CSV UTF-8 con:

```text
key,input_path,speaker_id,accent,license,source_url,release
```

`key` debe coincidir con la clave normalizada ya existente en `audio/audio_index.json`. `release` debe documentar la autorización aplicable del hablante/proveedor cuando corresponda.

Primero validar:

```bash
python scripts/import_human_audio.py recordings.csv --dry-run
```

Luego importar:

```bash
python scripts/import_human_audio.py recordings.csv
```

El importador:

- exige speaker, acento, licencia, URL de origen y release;
- normaliza con FFmpeg a mono 44.1 kHz Ogg Vorbis;
- aplica loudness objetivo `I=-20`, `TP=-2`, `LRA=7`;
- sustituye el asset en la misma ruta;
- recalcula SHA-256 y provenance en el índice;
- cambia el banco a `mixed` o `human` según corresponda;
- actualiza el hash del índice en el manifest de contenido.

## Gates

Después de cada lote:

```bash
python scripts/audio_asset_audit.py
python scripts/audio_signal_audit.py
python scripts/verify_release.py
```

`audio_signal_audit.py` inspecciona todos los assets fonológicos y todas las grabaciones humanas con `ffprobe`, controla duración/mono y exige provenance completa para audio no sintético.

## Criterio editorial

Priorizar primero:

1. pares mínimos/contrastes fonológicos;
2. listening donde el acento o reducción sea pedagógicamente relevante;
3. ejemplos de pragmática/entonación;
4. variedad de hablantes/acento en niveles altos.

No activar un archivo únicamente porque su licencia sea compatible: debe pasar también QA lingüístico y acústico.
