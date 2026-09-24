# Política de expansión léxica abierta — v1.2

## Por qué no se activan automáticamente decenas de miles de traducciones

La búsqueda de fuentes abiertas encontró un pack inglés→español con decenas de miles de entradas y procedencia abierta (Contexto/FreeDict). Sin embargo, una auditoría de muestras comunes mostró un problema pedagógico importante: incluso entradas marcadas con confianza alta pueden seleccionar una acepción que no coincide con la que necesita el nivel, por ejemplo una palabra polisémica puede devolver un sustantivo cuando el currículo quiere el verbo.

Por eso SIAA separa **referencia abierta** de **contenido enseñable**. El objetivo no es inflar el contador de vocabulario, sino activar una acepción correcta y útil.

## Gate mínimo para activar un lexema

- el headword aparece en un perfil CEFR/frecuencia defendible;
- la glosa española corresponde a la acepción enseñada;
- POS y sentido están alineados;
- no es nombre propio/entrada enciclopédica salvo objetivo explícito;
- ejemplo/chunk son compatibles con el nivel;
- distractores son plausibles pero no ambiguos;
- provenance/licencia quedan registradas;
- pasa revisión automática y, para casos ambiguos, revisión humana.

## Fuentes abiertas ya usadas

- CEFR-J / Open Language Profiles para niveles y candidaturas.
- `Jason-Latz/contexto` como overlay conservador.
- FreeDict English–Spanish como una de las fuentes documentadas aguas arriba del pack Contexto.

La cola de authoring permanece deliberadamente más grande que el conjunto activo. Esta separación evita convertir un diccionario bilingüe general en un curso defectuoso.
