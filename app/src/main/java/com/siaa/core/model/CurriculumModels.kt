package com.siaa.core.model

enum class CefrLevel(val displayName: String, val description: String) {
    PRE_A1("Pre-A1", "Fundamentos y supervivencia: letras, números, saludos"),
    A1("A1", "Interacciones cotidianas inmediatas y frases básicas"),
    A2("A2", "Rutinas, transacciones simples e instrucciones"),
    B1("B1", "Comprensión de viajes, anécdotas e ideas principales"),
    B2("B2", "Discursos técnicos, matices y argumentación estructurada"),
    C1("C1", "Lenguaje idiomático, causal, ironía y discurso académico"),
    C2("C2", "Resumen denso, paradojas y registros de alta precisión")
}

enum class ExerciseKind(val displayName: String, val iconName: String) {
    LISTENING("Listening", "headphones"),
    PHONOLOGY("Fonología", "graphic_eq"),
    PHRASE("Frases y Colocaciones", "record_voice_over"),
    ALPHABET_SPELLING("Deletreo y Alfabeto", "spellcheck")
}

data class ExerciseOption(
    val id: String,
    val text: String,
    val isCorrect: Boolean,
    val earbudCommand: String = "" // e.g. "Next" or "Prev"
)

data class ExerciseItem(
    val id: String,
    val title: String,
    val level: String,
    val kind: ExerciseKind,
    val promptText: String,
    val audioPath: String? = null,
    val audioVoice: String = "en-us",
    val speedWpm: Int = 140,
    val options: List<ExerciseOption>,
    val explanation: String,
    val hint: String,
    val phoneticPair: String? = null,
    val category: String = ""
)
