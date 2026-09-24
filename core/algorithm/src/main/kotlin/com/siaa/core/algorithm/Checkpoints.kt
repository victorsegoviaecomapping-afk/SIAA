package com.siaa.core.algorithm

import com.siaa.core.model.LearnerKcState
import com.siaa.core.model.SessionPolicy

data class CheckpointResult(
    val passed: Boolean,
    val score: Double,
    val reasons: List<String>
)

object MasteryCheckpointEvaluator {
    fun evaluate(
        state: LearnerKcState,
        nowEpochMs: Long,
        policy: SessionPolicy = SessionPolicy()
    ): CheckpointResult {
        val elapsed = state.lastReviewedAtEpochMs?.let {
            (nowEpochMs - it).coerceAtLeast(0L) / 3_600_000.0
        } ?: Double.POSITIVE_INFINITY
        val retention = if (elapsed.isFinite()) {
            HalfLifeModel.recallProbability(elapsed, state.halfLifeHours)
        } else 0.0
        val score = 0.40 * state.mastery +
            0.18 * state.recognition +
            0.14 * state.production +
            0.12 * state.automaticity +
            0.10 * retention +
            0.03 * (state.transferSuccesses / 2.0).coerceIn(0.0, 1.0) +
            0.03 * (state.novelSuccesses / 3.0).coerceIn(0.0, 1.0)
        val reasons = buildList {
            if (state.mastery < policy.masteryThreshold) add("mastery<${policy.masteryThreshold}")
            if (retention < 0.75) add("retention<0.75")
            if (state.automaticity < 0.55) add("automaticity<0.55")
            if (state.totalAttempts < 3) add("insufficient_evidence")
            if (state.transferSuccesses < policy.checkpointMinTransferSuccesses) add("insufficient_transfer")
            if (state.novelSuccesses < policy.checkpointMinNovelSuccesses) add("insufficient_novelty")
        }
        return CheckpointResult(reasons.isEmpty() && score >= 0.78, score, reasons)
    }
}
