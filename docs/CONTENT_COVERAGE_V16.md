# Cobertura de contenido v1.6

## Léxico activo

| Nivel | Lexemas activos |
|---|---:|
| A1 | 530 |
| A2 | 493 |
| B1 | 664 |
| B2 | 373 |
| C1 | 344 |
| C2 | 342 |
| **Total** | **2,746** |

## Estado general

- **3,281 KCs**.
- **6,652 relaciones curriculares**.
- **7,751 ejercicios estáticos**.
- **~8,944 variantes deterministas generables**.
- **~16,695 actividades potenciales** antes de futuros bancos humanos adicionales.
- **2,901 audios offline pre-renderizados**: 2,746 vocabulario, 102 listening y 53 fonología.

## Expansión v1.6

La pasada v1.6 activó **724 lexemas adicionales** respecto a v1.5, concentrados sobre todo en B2, C1 y C2. Se utilizó un subconjunto de alta confianza del language pack español de Contexto (con obligaciones de datos upstream preservadas; véase THIRD_PARTY_NOTICES.md) y se validó el nivel CEFR de cada headword contra CEFR-J/Open Language Profiles.

Las entradas ya existentes no se duplicaron: se enriquecieron con tags/provenance y, cuando correspondía, frecuencia.

## Regla de calidad

Una entrada sólo entra al pack activo cuando:

1. tiene headword y glosa española;
2. su nivel está respaldado por CEFR-J/Open Language Profiles;
3. la glosa procede de una fuente permisiva o authoring original SIAA;
4. existe un KC léxico;
5. existe enseñanza y transferencia;
6. el generador produce tareas de significado, spelling y recuperación;
7. el banco de audio contiene la forma inglesa pre-renderizada.

## Lo que aún falta para llamar al contenido “final”

- más cobertura de **sentidos** por palabra, no sólo headwords;
- **collocations**, phrasal verbs y multiword expressions graduados;
- mayor densidad de pragmática fina C1/C2;
- listening auténtico largo con voces humanas/acento variado;
- banco humano para contrastes fonológicos críticos;
- QA humano lingüístico y acústico antes del lanzamiento.
