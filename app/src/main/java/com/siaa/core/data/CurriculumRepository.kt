package com.siaa.core.data

import android.content.Context
import com.siaa.core.model.CefrLevel
import com.siaa.core.model.ExerciseItem
import com.siaa.core.model.ExerciseKind
import com.siaa.core.model.ExerciseOption
import org.json.JSONObject
import java.io.InputStreamReader

class CurriculumRepository(private val context: Context) {

    private val cachedExercises = mutableListOf<ExerciseItem>()
    private var isLoaded = false

    fun getExercises(level: String? = null, kind: ExerciseKind? = null): List<ExerciseItem> {
        if (!isLoaded) {
            loadExercises()
        }
        return cachedExercises.filter { item ->
            (level == null || item.level.equals(level, ignoreCase = true)) &&
            (kind == null || item.kind == kind)
        }
    }

    fun getAllExercises(): List<ExerciseItem> {
        if (!isLoaded) {
            loadExercises()
        }
        return cachedExercises
    }

    @Synchronized
    private fun loadExercises() {
        if (isLoaded) return

        try {
            context.assets.open("audio/audio_index.json").use { inputStream ->
                val reader = InputStreamReader(inputStream, Charsets.UTF_8)
                val jsonString = reader.readText()
                val jsonObject = JSONObject(jsonString)
                val entries = jsonObject.optJSONObject("entries")

                if (entries != null) {
                    val keys = entries.keys()
                    var count = 0
                    while (keys.hasNext() && count < 250) {
                        val key = keys.next()
                        val entry = entries.getJSONObject(key)
                        val text = entry.optString("text", key)
                        val kindStr = entry.optString("kind", "listening")
                        val level = entry.optString("level", "A1")
                        val rel = entry.optString("rel")
                        val voice = entry.optString("voice", "en-us")
                        val speedWpm = entry.optInt("speedWpm", 140)

                        val kind = when (kindStr.lowercase()) {
                            "phonology" -> ExerciseKind.PHONOLOGY
                            "phrase" -> ExerciseKind.PHRASE
                            "alphabet", "spelling" -> ExerciseKind.ALPHABET_SPELLING
                            else -> if (level.equals("Pre-A1", ignoreCase = true) && text.length < 15) {
                                ExerciseKind.ALPHABET_SPELLING
                            } else {
                                ExerciseKind.LISTENING
                            }
                        }

                        val item = createExerciseFromEntry(
                            id = "ex_${count + 1}",
                            rawText = text,
                            level = level,
                            kind = kind,
                            audioPath = rel,
                            voice = voice,
                            speedWpm = speedWpm
                        )
                        cachedExercises.add(item)
                        count++
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback to embedded seed exercises if index fails
            cachedExercises.addAll(getSeedExercises())
        }

        if (cachedExercises.isEmpty()) {
            cachedExercises.addAll(getSeedExercises())
        }

        isLoaded = true
    }

    private fun createExerciseFromEntry(
        id: String,
        rawText: String,
        level: String,
        kind: ExerciseKind,
        audioPath: String,
        voice: String,
        speedWpm: Int
    ): ExerciseItem {
        return when (kind) {
            ExerciseKind.PHONOLOGY -> {
                val title = "Discriminación Fonológica ($level)"
                val options = listOf(
                    ExerciseOption("1", "Sonido A: Forma estándar clara", isCorrect = true, earbudCommand = "Next"),
                    ExerciseOption("2", "Sonido B: Variante contrastiva / reducida", isCorrect = false, earbudCommand = "Prev")
                )
                ExerciseItem(
                    id = id,
                    title = title,
                    level = level,
                    kind = kind,
                    promptText = rawText,
                    audioPath = audioPath,
                    audioVoice = voice,
                    speedWpm = speedWpm,
                    options = options,
                    explanation = "En este ejercicio fonológico debes identificar el rasgo acústico y la entonación natural de: \"$rawText\".",
                    hint = "Escucha con atención la vocal tónica y la consonante final.",
                    category = "Fonología y Pronunciación"
                )
            }
            ExerciseKind.PHRASE -> {
                val cleanPhrase = rawText.replace("_", " ")
                val title = "Frase Clave: $cleanPhrase"
                val options = listOf(
                    ExerciseOption("1", "Uso correcto en contexto comunicativo", isCorrect = true, earbudCommand = "Next"),
                    ExerciseOption("2", "Traducción literal incorrecta o uso no idiomático", isCorrect = false, earbudCommand = "Prev")
                )
                ExerciseItem(
                    id = id,
                    title = title,
                    level = level,
                    kind = kind,
                    promptText = "Expresión: $cleanPhrase",
                    audioPath = audioPath,
                    audioVoice = voice,
                    speedWpm = speedWpm,
                    options = options,
                    explanation = "\"$cleanPhrase\" es una frase idiomática fijada en inglés de nivel $level.",
                    hint = "Piensa en el significado funcional de la colocación en una conversación real.",
                    category = "Frases y Colocaciones"
                )
            }
            ExerciseKind.ALPHABET_SPELLING -> {
                val title = "Deletreo y Reconocimiento ($level)"
                val options = listOf(
                    ExerciseOption("1", rawText, isCorrect = true, earbudCommand = "Next"),
                    ExerciseOption("2", "Opción alternativa de pronunciación", isCorrect = false, earbudCommand = "Prev")
                )
                ExerciseItem(
                    id = id,
                    title = title,
                    level = level,
                    kind = kind,
                    promptText = "¿Qué palabra o letra deletreada escuchas?",
                    audioPath = audioPath,
                    audioVoice = voice,
                    speedWpm = speedWpm,
                    options = options,
                    explanation = "La pronunciación correcta corresponde exactamente a \"$rawText\".",
                    hint = "Presta atención a cada fonema pronunciado individualmente.",
                    category = "Alfabeto y Supervivencia"
                )
            }
            else -> {
                val title = "Comprensión Auditiva ($level)"
                val options = listOf(
                    ExerciseOption("1", "La afirmación refleja fielmente lo expresado en el audio", isCorrect = true, earbudCommand = "Next"),
                    ExerciseOption("2", "La afirmación contradice o distorsiona el sentido del audio", isCorrect = false, earbudCommand = "Prev")
                )
                ExerciseItem(
                    id = id,
                    title = title,
                    level = level,
                    kind = kind,
                    promptText = rawText,
                    audioPath = audioPath,
                    audioVoice = voice,
                    speedWpm = speedWpm,
                    options = options,
                    explanation = "El audio expresa: \"$rawText\".",
                    hint = "Enfócate en la idea central y la actitud del hablante sin leer la pantalla.",
                    category = "Comprensión Auditiva"
                )
            }
        }
    }

    private fun getSeedExercises(): List<ExerciseItem> {
        return listOf(
            ExerciseItem(
                id = "seed_1",
                title = "Pedido en Cafetería (A1)",
                level = "A1",
                kind = ExerciseKind.LISTENING,
                promptText = "Can I have a cheese sandwich and a glass of water, please?",
                audioPath = "audio/listening/l12_cafe_order.ogg",
                audioVoice = "en-gb",
                speedWpm = 125,
                options = listOf(
                    ExerciseOption("1", "Pide un sándwich de queso y agua", isCorrect = true, earbudCommand = "Next"),
                    ExerciseOption("2", "Pide un café caliente y la cuenta", isCorrect = false, earbudCommand = "Prev")
                ),
                explanation = "El cliente dice: 'Can I have a cheese sandwich and a glass of water, please?' (¿Me da un sándwich de queso y un vaso de agua, por favor?).",
                hint = "Escucha las palabras 'cheese sandwich' y 'water'.",
                category = "Supervivencia y Comida"
            ),
            ExerciseItem(
                id = "seed_2",
                title = "Cambio de Cita Médica (A2)",
                level = "A2",
                kind = ExerciseKind.LISTENING,
                promptText = "The dentist moved my appointment from Tuesday morning to Wednesday afternoon.",
                audioPath = "audio/listening/l12_appointment.ogg",
                audioVoice = "en-us",
                speedWpm = 135,
                options = listOf(
                    ExerciseOption("1", "La cita cambió para el miércoles por la tarde", isCorrect = true, earbudCommand = "Next"),
                    ExerciseOption("2", "La cita se canceló definitivamente para el martes", isCorrect = false, earbudCommand = "Prev")
                ),
                explanation = "La recepcionista explica que la cita pasó de Tuesday morning a Wednesday afternoon.",
                hint = "Fíjate en el día final: 'to Wednesday afternoon'.",
                category = "Citas y Rutinas"
            ),
            ExerciseItem(
                id = "seed_3",
                title = "Contraste Fonológico: Flap-T (B1)",
                level = "B1",
                kind = ExerciseKind.PHONOLOGY,
                promptText = "Escucha la articulación del sonido 't' entre vocales (Flap-T /ɾ/ vs /t/ aspirada).",
                audioPath = "audio/phonology/q20_p20_b1_flap_t.ogg",
                audioVoice = "en-us",
                speedWpm = 130,
                options = listOf(
                    ExerciseOption("1", "Usa pronunciación con Flap-T americano suave (/ɾ/)", isCorrect = true, earbudCommand = "Next"),
                    ExerciseOption("2", "Usa articulación británica con oclusión alveolar fuerte (/t/)", isCorrect = false, earbudCommand = "Prev")
                ),
                explanation = "En inglés norteamericano común, la /t/ intervocálica átona se pronuncia como un 'tap' o 'flap' semejante a la 'r' suave en español.",
                hint = "Escucha la suavidad del sonido central.",
                category = "Fonología y Pronunciación"
            ),
            ExerciseItem(
                id = "seed_4",
                title = "Colocación: Come up with (B1)",
                level = "B1",
                kind = ExerciseKind.PHRASE,
                promptText = "We need to come up with a better solution.",
                audioPath = "audio/phrase/q20_c20_come_up_with.ogg",
                audioVoice = "en-us",
                speedWpm = 140,
                options = listOf(
                    ExerciseOption("1", "Idear, proponer o inventar una solución", isCorrect = true, earbudCommand = "Next"),
                    ExerciseOption("2", "Subir físicamente con un objeto", isCorrect = false, earbudCommand = "Prev")
                ),
                explanation = "'Come up with' es un phrasal verb esencial que significa producir, sugerir o concebir una idea o solución.",
                hint = "Es sinónimo de 'invent' o 'propose'.",
                category = "Phrasal Verbs"
            ),
            ExerciseItem(
                id = "seed_5",
                title = "Discurso Diplomático: Qualified Support (C1)",
                level = "C1",
                kind = ExerciseKind.LISTENING,
                promptText = "At first the savings look impressive. Once deferred maintenance is included, however, much of the apparent efficiency disappears.",
                audioPath = "audio/listening/l12_budget_reframe.ogg",
                audioVoice = "en-gb",
                speedWpm = 165,
                options = listOf(
                    ExerciseOption("1", "El ahorro inicial es engañoso cuando se contemplan costes diferidos", isCorrect = true, earbudCommand = "Next"),
                    ExerciseOption("2", "El plan generó ahorros sostenibles y eficientes en todo momento", isCorrect = false, earbudCommand = "Prev")
                ),
                explanation = "El hablante matiza con 'however': una vez se incluye el mantenimiento diferido, la supuesta eficiencia se desvanece.",
                hint = "Presta atención al conector 'however' y su cambio de tono.",
                category = "Discurso Avanzado"
            )
        )
    }
}
