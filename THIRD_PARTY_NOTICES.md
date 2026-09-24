# Third-party data and notices

## CEFR-J / Open Language Profiles

The repository includes copies of CEFR-J/Open Language Profiles CSV datasets in `app/src/main/assets/reference/cefrj/`.

- CEFR-J vocabulary and grammar profiles: research/commercial use is permitted without charge provided the dataset is cited; copyright remains with Tono Laboratory, TUFS.
- Octanove C1/C2 Vocabulary Profile: CC BY-SA 4.0.

See `app/src/main/assets/reference/cefrj/SOURCE_README.md` for the bundled attribution text.

## Contexto Spanish language-pack data

SIAA uses a selected subset of `Jason-Latz/contexto` Spanish language-pack entries, cross-checked against CEFR-J before activation.

Important licensing distinction:

- Contexto's own source code is MIT licensed.
- Contexto's upstream `THIRD_PARTY_NOTICES.md` states that its shipped Spanish pack is a derivative work incorporating FreeDict and that affected pack data is distributed under **CC BY-SA 3.0**.
- Therefore SIAA treats the selected Contexto-derived lexical data conservatively as share-alike data and preserves attribution/provenance rather than labelling the whole language pack as MIT.

Upstream documented data sources include FreeDict English-Spanish (CC BY-SA 3.0), Kaikki/Wiktionary (CC BY-SA/GFDL components), Open Multilingual Wordnet (mixed component licences), and optional Apertium corroboration (GPL-2.0). SIAA does not bundle those complete upstream corpora.

See `app/src/main/assets/reference/contexto/README.md`.

## SIAA original authored content

The multiword expressions, Spanish pedagogical explanations, transfer prompts, listening scripts and new pragmatics exercises introduced in v2.0 are original SIAA authoring. Common short English expressions themselves are language facts; the examples and instructional framing were authored for this project.

## SIAA synthetic audio

Audio files under `app/src/main/assets/audio/` are generated project artifacts created with eSpeak and encoded as Ogg Vorbis. They are labelled `synthetic=true`. They are not recordings copied from commercial language-learning materials.

## Reference-only resources

The British Council/EAQUALS Core Inventory, CEFR Companion Volume and books/papers supplied by the user inform original SIAA authoring. Their complete copyrighted prose, exercise banks and audio are not redistributed unless the relevant licence independently permits it.

## Human-audio candidates

`app/src/main/assets/reference/human_audio/candidates.json` contains metadata only for selected open pronunciation candidates. Binary human recordings are not silently imported: every future asset must keep per-file licence/attribution and pass acoustic/linguistic QA.
