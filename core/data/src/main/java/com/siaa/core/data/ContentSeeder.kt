package com.siaa.core.data

import android.content.Context
import androidx.room.withTransaction
import com.siaa.core.content.ContentPackValidator
import com.siaa.core.content.ContentValidationIssue
import com.siaa.core.content.ControlledVariantGenerator
import com.siaa.core.content.LexemeSpec
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.io.File

class ContentSeeder(
    private val context: Context,
    private val db: SiaaDatabase
) {
    companion object {
        const val SUPPORTED_SCHEMA_VERSION = "1.1"
        private const val GENERATOR_VERSION = "4"
    }

    suspend fun seedIfNeeded(force: Boolean = false) {
        val manifestObj = readObject("content/manifest.json")
        val schemaVersion = manifestObj.optString("schemaVersion", "")
        check(schemaVersion == SUPPORTED_SCHEMA_VERSION) {
            "Unsupported schema version: '$schemaVersion' (expected $SUPPORTED_SCHEMA_VERSION)"
        }

        val contentVersion = manifestObj.optString("contentVersion", manifestObj.optString("version", "0.7.0"))
        val effectiveVersion = "$contentVersion|generator=$GENERATOR_VERSION"
        val installedVersion = db.metaDao().get("content_version")
        if (!force && installedVersion == effectiveVersion && db.contentDao().kcCount() > 0) return

        val checksumsObj = manifestObj.optJSONObject("checksums")
        check(checksumsObj != null) {
            "Manifest no contiene el bloque obligatorio 'checksums'"
        }

        val filesToCheck = listOf(
            "content/kcs.json" to "kcs.json",
            "content/edges.json" to "edges.json",
            "content/exercises.json" to "exercises.json",
            "content/lexemes.json" to "lexemes.json"
        )
        for ((path, key) in filesToCheck) {
            val expectedHash = checksumsObj.optString(key, "")
            check(expectedHash.isNotEmpty()) {
                "Checksum obligatorio ausente para $key"
            }
            val actualHash = sha256Asset(path)
            check(actualHash.equals(expectedHash, ignoreCase = true)) {
                "Checksum mismatch para $path: esperado $expectedHash pero fue $actualHash"
            }
        }

        val kcs = readArray("content/kcs.json").map { obj ->
            KnowledgeComponentEntity(
                id = obj.getString("id"),
                name = obj.getString("name"),
                cefr = obj.optString("cefr", "A1"),
                domain = obj.getString("domain"),
                form = obj.optString("form"),
                meaning = obj.optString("meaning"),
                useText = obj.optString("use"),
                importance = obj.optDouble("importance", 0.5),
                priorMastery = obj.optDouble("priorMastery", 0.15),
                tagsCsv = jsonArrayToCsv(obj.optJSONArray("tags"))
            )
        }
        val edges = readArray("content/edges.json").map { obj ->
            KnowledgeEdgeEntity(
                fromId = obj.getString("fromId"),
                toId = obj.getString("toId"),
                weight = obj.optDouble("weight", 1.0),
                hardPrerequisite = obj.optBoolean("hardPrerequisite", true)
            )
        }
        val staticExercises = readArray("content/exercises.json").map { obj ->
            ExerciseEntity(
                id = obj.getString("id"),
                type = obj.getString("type"),
                kcIdsCsv = jsonArrayToCsv(obj.getJSONArray("kcIds")),
                cefr = obj.optString("cefr", "A1"),
                difficulty = obj.optDouble("difficulty", 0.4),
                promptEs = obj.optString("promptEs"),
                stimulusEn = obj.optString("stimulusEn"),
                optionA = obj.optString("optionA"),
                optionB = obj.optString("optionB"),
                correctOption = obj.optString("correctOption"),
                explanationEs = obj.optString("explanationEs"),
                spellTarget = obj.optString("spellTarget"),
                estimatedSeconds = obj.optInt("estimatedSeconds", 20),
                tagsCsv = jsonArrayToCsv(obj.optJSONArray("tags")),
                misconceptionIdsCsv = jsonArrayToCsv(obj.optJSONArray("misconceptionIds"))
            )
        }
        val generator = ControlledVariantGenerator()
        val generatedExercises = readArray("content/lexemes.json")
            .map { obj ->
                LexemeSpec(
                    id = obj.getString("id"),
                    lemma = obj.getString("lemma"),
                    meaningEs = obj.getString("meaningEs"),
                    cefr = obj.optString("cefr", "A1"),
                    spellingDifficulty = obj.optDouble("spellingDifficulty", 0.25),
                    distractorEs = obj.optString("distractorEs", "otra opción"),
                    hardDistractorsEs = jsonArrayToList(obj.optJSONArray("hardDistractorsEs")),
                    chunks = jsonArrayToList(obj.optJSONArray("chunks")),
                    exampleFrames = jsonArrayToList(obj.optJSONArray("exampleFrames")),
                    senseId = obj.optString("senseId"),
                    pos = obj.optString("pos"),
                    semanticDomain = obj.optString("semanticDomain", "general"),
                    register = obj.optString("register", "neutral"),
                    audioForm = obj.optString("audioForm", obj.getString("lemma")),
                    tags = jsonArrayToList(obj.optJSONArray("tags")).toSet()
                )
            }
            .flatMap(generator::vocabularyVariants)
            .map { gen ->
                ExerciseEntity(
                    id = gen.id,
                    type = gen.kind,
                    kcIdsCsv = gen.kcId,
                    cefr = kcs.firstOrNull { it.id == gen.kcId }?.cefr ?: "A1",
                    difficulty = if (gen.kind == "SPELLING_AB" || gen.kind == "SPELL_FROM_AUDIO") 0.48 else 0.35,
                    promptEs = gen.promptEs,
                    stimulusEn = gen.stimulusEn,
                    optionA = gen.optionA,
                    optionB = gen.optionB,
                    correctOption = gen.correctOption,
                    explanationEs = gen.explanationEs,
                    spellTarget = gen.spellTarget,
                    estimatedSeconds = if (gen.kind == "TEACH") 18 else 22,
                    tagsCsv = "generated,vocabulary"
                )
            }

        val allExercises = staticExercises + generatedExercises
        val duplicates = allExercises.groupBy { it.id }.filter { it.value.size > 1 }.keys
        check(duplicates.isEmpty()) {
            "Colisión de IDs detectada en ejercicios del content pack: ${duplicates.take(5)}"
        }
        val exercises = allExercises

        val validation = ContentPackValidator.validate(
            components = kcs.map { it.toModel() },
            edges = edges.map { it.toModel() },
            exercises = exercises.map { it.toModel() }
        )
        val errors = validation.filter { it.severity == ContentValidationIssue.Severity.ERROR }
        check(errors.isEmpty()) {
            "El content pack local es inválido: " + errors.take(8).joinToString(" | ") { "${it.code}: ${it.message}" }
        }

        db.withTransaction {
            db.contentDao().clearEdges()
            db.contentDao().clearExercises()
            db.contentDao().clearKcs()
            db.contentDao().insertKcs(kcs)
            db.contentDao().insertEdges(edges)
            db.contentDao().insertExercises(exercises)

            val validKcIds = kcs.map { it.id }
            db.learnerDao().deleteOrphanStates(validKcIds)

            db.metaDao().put(AppMetaEntity("content_version", effectiveVersion))
        }
    }

    private fun readObject(path: String): JSONObject = JSONObject(readText(path))

    private fun readArray(path: String): List<JSONObject> {
        val arr = JSONArray(readText(path))
        return List(arr.length()) { arr.getJSONObject(it) }
    }

    private fun readText(path: String): String = openContent(path).bufferedReader().use { it.readText() }

    private fun openContent(path: String): java.io.InputStream {
        val active = File(context.filesDir, ContentPackManager.ACTIVE_DIR)
        val name = path.removePrefix("content/")
        val external = File(active, name)
        return if (external.isFile) external.inputStream() else context.assets.open(path)
    }

    private fun sha256Asset(path: String): String {
        val bytes = openContent(path).use { it.readBytes() }
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun jsonArrayToList(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { arr.optString(it) }
    }

    private fun jsonArrayToCsv(arr: JSONArray?): String {
        if (arr == null) return ""
        return (0 until arr.length()).joinToString(",") { arr.optString(it) }
    }


}
