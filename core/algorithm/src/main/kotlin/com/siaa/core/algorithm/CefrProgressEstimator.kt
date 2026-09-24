package com.siaa.core.algorithm

import com.siaa.core.model.ExerciseDefinition
import com.siaa.core.model.ExerciseType
import com.siaa.core.model.LearningSnapshot
import com.siaa.core.model.SessionPolicy

/**
 * Estima progreso CEFR sobre D_l: KCs que cuentan con una vía completa para satisfacer
 * el checkpoint (evidencia graduada múltiple + al menos una actividad de transferencia).
 * Así, un KC meramente descriptivo o sin instrumento suficiente no hace matemáticamente
 * imposible superar un nivel.
 */
object CefrProgressEstimator {
    private val order = listOf("Pre-A1", "A1", "A2", "B1", "B2", "C1", "C2")

    fun checkpointEligibleKcIds(snapshot: LearningSnapshot): Set<String> {
        val gradedByKc = snapshot.exercises
            .filter { it.type != ExerciseType.TEACH }
            .flatMap { ex -> ex.kcIds.map { it to ex } }
            .groupBy({ it.first }, { it.second })
        return gradedByKc.mapNotNull { (kcId, exercises) ->
            val distinct = exercises.distinctBy { it.id }
            val hasNovelPath = distinct.size >= 2
            val hasTransferPath = distinct.any(::isTransferEvidence)
            kcId.takeIf { hasNovelPath && hasTransferPath }
        }.toSet()
    }

    fun estimate(snapshot: LearningSnapshot, nowEpochMs: Long, policy: SessionPolicy = SessionPolicy()): String {
        val essential = checkpointEligibleKcIds(snapshot)
        var best: String? = null
        var firstAvailable: String? = null
        for (level in order) {
            val kcs = snapshot.components.filter { it.cefr.equals(level, true) && it.id in essential }
            if (kcs.isEmpty()) continue
            if (firstAvailable == null) firstAvailable = level
            val passed = kcs.count { kc ->
                val state = snapshot.stateByKcId[kc.id] ?: return@count false
                MasteryCheckpointEvaluator.evaluate(state, nowEpochMs, policy).passed
            }
            val coverage = passed.toDouble() / kcs.size
            if (coverage >= policy.cefrCoverageThreshold) best = level else break
        }
        return if (best != null) {
            "Progreso CEFR estimado por SIAA: $best"
        } else {
            "Progreso CEFR estimado por SIAA: en progreso hacia ${firstAvailable ?: "Pre-A1"}"
        }
    }

    fun isTransferEvidence(exercise: ExerciseDefinition): Boolean =
        exercise.kcIds.size > 1 ||
            exercise.type in setOf(
                ExerciseType.SPELL_FROM_AUDIO,
                ExerciseType.PRON_DISCRIMINATION,
                ExerciseType.CHUNK_AB
            ) ||
            exercise.tags.any { tag ->
                tag.equals("transfer", true) ||
                    tag.equals("contrast", true) ||
                    tag.equals("context", true) ||
                    tag.equals("novel-context", true)
            }
}
