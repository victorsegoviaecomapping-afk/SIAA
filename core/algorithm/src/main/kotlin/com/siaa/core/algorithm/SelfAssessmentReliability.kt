package com.siaa.core.algorithm

import com.siaa.core.model.*

data class SelfAssessmentReliability(
    val positive: Double,
    val unsure: Double,
    val negative: Double
)

/**
 * Lightweight per-learner calibration for noisy self-report observations.
 * It contrasts recent objective graded accuracy with the learner's self-reported
 * positive rate on the same KC. Optimistic self-reporting is down-weighted.
 */
object SelfAssessmentReliabilityEstimator {
    fun estimate(
        kcId: String,
        snapshot: LearningSnapshot,
        interactions: List<InteractionRecord>,
        policy: SessionPolicy = SessionPolicy()
    ): SelfAssessmentReliability {
        val relevant = interactions.filter { record ->
            snapshot.exerciseById[record.exerciseId]?.kcIds?.contains(kcId) == true && record.graded
        }
        val objective = relevant.filter { record ->
            val type = snapshot.exerciseById[record.exerciseId]?.type
            type != ExerciseType.SELF_ASSESS && type != ExerciseType.SPELL_FROM_AUDIO
        }
        val self = relevant.filter { record ->
            val type = snapshot.exerciseById[record.exerciseId]?.type
            type == ExerciseType.SELF_ASSESS || type == ExerciseType.SPELL_FROM_AUDIO
        }
        // Beta(2,2) priors avoid extreme reliability from tiny samples.
        val objectiveAccuracy = (2.0 + objective.count { it.correct }) / (4.0 + objective.size)
        val selfPositiveRate = (2.0 + self.count { it.confidence == ResponseConfidence.CORRECT }) / (4.0 + self.size)
        val optimism = (selfPositiveRate - objectiveAccuracy).coerceAtLeast(0.0)
        val pessimism = (objectiveAccuracy - selfPositiveRate).coerceAtLeast(0.0)
        return SelfAssessmentReliability(
            positive = (policy.selfAssessPositiveWeight * (1.0 - 0.75 * optimism)).coerceIn(0.12, 0.55),
            unsure = (policy.selfAssessUnsureWeight * (1.0 - 0.35 * optimism)).coerceIn(0.10, 0.40),
            negative = (policy.selfAssessNegativeWeight * (1.0 - 0.35 * pessimism)).coerceIn(0.30, 0.75)
        )
    }
}
