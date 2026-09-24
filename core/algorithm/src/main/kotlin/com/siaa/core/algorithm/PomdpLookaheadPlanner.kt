package com.siaa.core.algorithm

import com.siaa.core.model.*

/**
 * POMDP/MPC aproximado para móvil. El belief state se resume en LearnerKcState,
 * se simulan observaciones correcto/incorrecto y se vuelve a rankear desde el
 * estado hipotético. Sólo se ejecuta la primera acción; después de cada evidencia
 * real se replantea, como en model predictive control.
 */
class PomdpLookaheadPlanner(
    private val base: ExercisePlanner = AdaptiveUtilityPlanner(),
    private val gamma: Double = 0.55,
    private val branchWidth: Int = 5,
    private val depth: Int = 2,
    private val diagnostic: QMatrixDiagnostic = QMatrixDiagnostic(),
    private val bktParams: BktParams = BktParams()
) : ExercisePlanner, ConfigurableExercisePlanner {
    override fun updatePolicy(policy: SessionPolicy) {
        (base as? ConfigurableExercisePlanner)?.updatePolicy(policy)
    }

    override fun rank(
        mode: SessionMode,
        snapshot: LearningSnapshot,
        recentInteractions: List<InteractionRecord>,
        nowEpochMs: Long,
        limit: Int
    ): List<PlannerCandidate> = rank(mode, snapshot, recentInteractions, nowEpochMs, limit, SessionCapabilities())

    override fun rank(
        mode: SessionMode,
        snapshot: LearningSnapshot,
        recentInteractions: List<InteractionRecord>,
        nowEpochMs: Long,
        limit: Int,
        capabilities: SessionCapabilities
    ): List<PlannerCandidate> {
        val immediate = base.rank(mode, snapshot, recentInteractions, nowEpochMs, branchWidth, capabilities)
        return immediate.map { candidate ->
            val future = expectedFutureValue(mode, snapshot, recentInteractions, candidate, nowEpochMs, capabilities, depth)
            candidate.copy(
                utility = candidate.utility + gamma * future,
                rationale = candidate.rationale + ", mpcFuture=${fmt(future)}, depth=$depth"
            )
        }.sortedWith(compareByDescending<PlannerCandidate> { it.utility }.thenBy { it.exercise.id }).take(limit)
    }

    private fun expectedFutureValue(
        mode: SessionMode,
        snapshot: LearningSnapshot,
        recent: List<InteractionRecord>,
        candidate: PlannerCandidate,
        nowEpochMs: Long,
        capabilities: SessionCapabilities,
        remainingDepth: Int
    ): Double {
        if (remainingDepth <= 0 || candidate.exercise.kcIds.isEmpty()) return 0.0
        val pCorrect = candidate.successProbability.coerceIn(0.02, 0.98)
        val correctSnapshot = hypotheticalSnapshot(snapshot, candidate.exercise, true)
        val wrongSnapshot = hypotheticalSnapshot(snapshot, candidate.exercise, false)
        val dt = candidate.exercise.estimatedSeconds * 1000L
        val correctRecent = listOf(syntheticInteraction(candidate.exercise, true, nowEpochMs)) + recent
        val wrongRecent = listOf(syntheticInteraction(candidate.exercise, false, nowEpochMs)) + recent
        val correctValue = continuationValue(mode, correctSnapshot, correctRecent, nowEpochMs + dt, capabilities, remainingDepth - 1)
        val wrongValue = continuationValue(mode, wrongSnapshot, wrongRecent, nowEpochMs + dt, capabilities, remainingDepth - 1)
        return pCorrect * correctValue + (1.0 - pCorrect) * wrongValue
    }

    private fun continuationValue(
        mode: SessionMode,
        snapshot: LearningSnapshot,
        recent: List<InteractionRecord>,
        nowEpochMs: Long,
        capabilities: SessionCapabilities,
        remainingDepth: Int
    ): Double {
        val next = base.rank(mode, snapshot, recent, nowEpochMs, branchWidth, capabilities)
        if (next.isEmpty()) return 0.0
        if (remainingDepth <= 0) return next.first().utility
        return next.take(2).maxOf { c ->
            c.utility + gamma * expectedFutureValue(mode, snapshot, recent, c, nowEpochMs, capabilities, remainingDepth)
        }
    }

    private fun hypotheticalSnapshot(snapshot: LearningSnapshot, exercise: ExerciseDefinition, correct: Boolean): LearningSnapshot {
        val weights = diagnostic.diagnosticWeights(exercise, snapshot.stateByKcId)
        val total = weights.values.sum().coerceAtLeast(1e-6)
        val updated = snapshot.states.associateBy { it.kcId }.toMutableMap()
        for (kcId in exercise.kcIds) {
            val component = snapshot.componentById[kcId]
            val prior = updated[kcId] ?: LearnerKcState(kcId, mastery = component?.priorMastery ?: 0.15)
            val w = ((weights[kcId] ?: 1.0 / exercise.kcIds.size.coerceAtLeast(1)) / total).coerceIn(0.10, 1.0)
            val posterior = BktUpdater.posterior(prior.mastery, correct, bktParams)
            val blended = prior.mastery + (posterior - prior.mastery) * w
            updated[kcId] = prior.copy(
                mastery = blended.coerceIn(0.0, 1.0),
                uncertainty = (prior.uncertainty * if (correct) 0.93 else 0.96).coerceIn(0.04, 0.95),
                consecutiveSuccess = if (correct) prior.consecutiveSuccess + 1 else 0,
                consecutiveFailure = if (correct) 0 else prior.consecutiveFailure + 1,
                totalAttempts = prior.totalAttempts + 1,
                totalCorrect = prior.totalCorrect + if (correct) 1 else 0
            )
        }
        return snapshot.copy(states = updated.values.toList())
    }

    private fun syntheticInteraction(exercise: ExerciseDefinition, correct: Boolean, now: Long) = InteractionRecord(
        sessionId = -1L,
        turnId = -1L,
        exerciseId = exercise.id,
        timestampEpochMs = now,
        response = if (correct) "SIM_CORRECT" else "SIM_WRONG",
        correct = correct,
        graded = true,
        kind = InteractionKind.GRADED_RESPONSE,
        latencyMs = exercise.estimatedSeconds * 250L
    )

    private fun fmt(v: Double) = "%.3f".format(java.util.Locale.US, v)
}
