# Expansión y cierre de contenido v2.2

## Objetivo

Cerrar los huecos detectados después de v2.1 sin inflar artificialmente el banco. La intervención prioriza KCs que no tenían evidencia evaluable, la debilidad de Pre-A1 y la calidad de distractores léxicos.

## 1. Fundación Pre-A1

Se añadieron **28 KCs CHUNK** orientados a supervivencia comunicativa: saludos/despedidas, cortesía, identidad, origen/residencia, estado personal, pedir ayuda, pedir repetición o menor velocidad, comprender significado/precio, localizar el baño y cierres temporales.

Cada nueva unidad dispone de enseñanza, discriminación de significado y transferencia auditiva. También se añadieron **8 KCs de pragmática Pre-A1**, **6 KCs de listening** con tareas originales y **4 KCs fonológicos** centrados en contrastes perceptivos básicos.

El resultado del gate por actividad es: 25 listenings, 8 discriminaciones fonológicas, 28 chunks y 8 KCs pragmáticos en Pre-A1.

## 2. Alphabet y spelling

La v2.1 tenía varias letras sin actividad evaluable. La v2.2 exige para cada `L_A`…`L_Z`:

1. `TEACH` con nombre/sonido de letra;
2. tarea `AB` evaluable;
3. transferencia/autoevaluación.

`O_ALPHABET` también queda conectado a la capa de letras y evaluable.

## 3. Ortografía y fonología

Se cerraron KCs legacy que estaban definidos pero no tenían evidencia suficiente. Entre ellos: límites de palabra, silent-e, terminación -ed y word stress; además se completó transferencia/enseñanza en `O_IGH`, `O_OUGH`, `PH_TH` y `PH_SHORT_LONG_I` donde faltaba.

## 4. Listening y chunks legacy

Se añadieron transferencias a unidades legacy que sólo tenían reconocimiento y enseñanza estratégica a KCs de listening que no explicitaban la estrategia antes de evaluar.

## 5. Distractores léxicos

Para **2,735 lexemas** se recalcularon distractores en español de forma determinista. La selección prioriza candidatos del **mismo nivel CEFR**, la **misma categoría gramatical** y frecuencia cercana, evitando duplicar el significado correcto. Esto reduce contrastes triviales del tipo “mujer vs casa”.

## 6. Enriquecimiento léxico

Los lexemas se enlazan con chunks existentes/nuevos cuando su lema aparece en una unidad formulaica. El banco actual deja **767 lexemas con al menos un chunk** y **143 con example frames enriquecidos** desde la capa formulaica.

## 7. Gate nuevo

`scripts/content_completeness_audit.py` comprueba:

- teach/objective/transfer de todos los KCs no léxicos;
- consistencia KC↔lexeme;
- distractores léxicos válidos;
- opciones A/B no vacías ni idénticas;
- ausencia de placeholders conocidos;
- umbral mínimo Pre-A1–C2 para listening/pronunciation/chunks/pragmatics;
- cobertura evaluable de las 26 letras.

El gate se ejecuta tanto en `validate.yml` como en `android-ci.yml`.
