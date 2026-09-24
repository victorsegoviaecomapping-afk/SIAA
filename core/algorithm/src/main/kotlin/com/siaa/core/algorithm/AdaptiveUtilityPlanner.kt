package com.siaa.core.algorithm

import com.siaa.core.model.*
import kotlin.math.abs
import kotlin.math.exp

class AdaptiveUtilityPlanner(
    private val onlineModels: OnlineAdaptiveModels = OnlineAdaptiveModels(),
    private val diagnostic: QMatrixDiagnostic = QMatrixDiagnostic(),
    policy: SessionPolicy = SessionPolicy(),
    private val placement: AdaptivePlacementEngine = AdaptivePlacementEngine()
) : ExercisePlanner, ConfigurableExercisePlanner {
    @Volatile private var policy: SessionPolicy = policy

    override fun updatePolicy(policy: SessionPolicy) { this.policy = policy }
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
        if (mode == SessionMode.PLACEMENT) {
            return placement.rank(snapshot, recentInteractions, capabilities, limit)
        }
        val states = snapshot.states.associateBy { it.kcId }
        val graph = KnowledgeGraphEngine(snapshot.components, snapshot.edges)
        val recentIds = recentInteractions.take(8).map { it.exerciseId }.toSet()

        // Reconstruct the role at the time of each recent interaction instead of
        // reclassifying history with the learner's current state.
        val recentRoles = classifyRecentRoles(recentInteractions.take(16), snapshot, states)
        val totalRecent = recentRoles.size.coerceAtLeast(1)
        val newProportion = recentRoles.count { it == LearningRole.NEW }.toDouble() / totalRecent
        val reviewProportion = recentRoles.count { it == LearningRole.REVIEW }.toDouble() / totalRecent
        val transferProportion = recentRoles.count { it == LearningRole.TRANSFER }.toDouble() / totalRecent

        val eligible = snapshot.exercises.asSequence()
            .filter { modeAllows(mode, it, snapshot.componentById) }
            .filter { capabilities.canPresent(it.type) }
            .filter { isEligible(it, states, graph, nowEpochMs) }
            .filterNot { it.id in recentIds && snapshot.exercises.size > 12 }
            .map { exercise -> score(exercise, snapshot, states, graph, recentInteractions, nowEpochMs, newProportion, reviewProportion, transferProportion) }
            .sortedWith(compareByDescending<PlannerCandidate> { it.utility }.thenBy { it.exercise.id })
            .take(limit)
            .toList()

        if (eligible.isNotEmpty()) return eligible

        // Si todos los recientes agotaron elegibles, permitir repetir recientes pero NUNCA violar isEligible
        return snapshot.exercises.asSequence()
            .filter { modeAllows(mode, it, snapshot.componentById) }
            .filter { capabilities.canPresent(it.type) }
            .filter { isEligible(it, states, graph, nowEpochMs) }
            .map { exercise -> score(exercise, snapshot, states, graph, recentInteractions, nowEpochMs, newProportion, reviewProportion, transferProportion) }
            .sortedWith(compareByDescending<PlannerCandidate> { it.utility }.thenBy { it.exercise.id })
            .take(limit)
            .toList()
    }


    private fun classifyRecentRoles(
        recentInteractions: List<InteractionRecord>,
        snapshot: LearningSnapshot,
        states: Map<String, LearnerKcState>
    ): List<LearningRole> {
        if (recentInteractions.isEmpty()) return emptyList()

        val window = recentInteractions.asReversed() // oldest -> newest
        val evidenceInsideWindow = mutableMapOf<String, Int>()
        for (interaction in window) {
            val exercise = snapshot.exerciseById[interaction.exerciseId] ?: continue
            for (kcId in exercise.kcIds) {
                if (interaction.graded || interaction.kind == InteractionKind.TEACH_EXPOSURE) {
                    evidenceInsideWindow[kcId] = (evidenceInsideWindow[kcId] ?: 0) + 1
                }
            }
        }

        // Seed KCs that demonstrably had evidence before this recent window.
        val seen = states.values.filter { state ->
            val currentEvidence = state.totalAttempts + state.exposureCount
            currentEvidence > (evidenceInsideWindow[state.kcId] ?: 0)
        }.mapTo(mutableSetOf()) { it.kcId }

        val roles = mutableListOf<LearningRole>()
        for (interaction in window) {
            val exercise = snapshot.exerciseById[interaction.exerciseId] ?: continue
            val transfer = exercise.kcIds.size > 1 || exercise.type in setOf(
                ExerciseType.PRON_DISCRIMINATION,
                ExerciseType.SPELL_FROM_AUDIO
            )
            val role = when {
                exercise.kcIds.any { it !in seen } -> LearningRole.NEW
                transfer -> LearningRole.TRANSFER
                else -> LearningRole.REVIEW
            }
            roles += role
            if (interaction.graded || interaction.kind == InteractionKind.TEACH_EXPOSURE) {
                seen += exercise.kcIds
            }
        }
        return roles
    }

    fun classifyRole(exercise: ExerciseDefinition, states: Map<String, LearnerKcState>): LearningRole {
        val totalAttempts = exercise.kcIds.sumOf { (states[it]?.totalAttempts ?: 0) + (states[it]?.exposureCount ?: 0) }
        return when {
            totalAttempts == 0 -> LearningRole.NEW
            exercise.kcIds.size > 1 || exercise.type in setOf(ExerciseType.PRON_DISCRIMINATION, ExerciseType.SPELL_FROM_AUDIO) -> LearningRole.TRANSFER
            else -> LearningRole.REVIEW
        }
    }

    private fun isEligible(
        exercise: ExerciseDefinition,
        states: Map<String, LearnerKcState>,
        graph: KnowledgeGraphEngine,
        nowEpochMs: Long
    ): Boolean {
        if (exercise.kcIds.isEmpty()) return true
        // TODOS los KCs del ejercicio deben tener sus prerrequisitos duros cumplidos.
        // NINGÚN tipo de ejercicio (incluyendo TEACH) puede saltarse un prerrequisito duro.
        return exercise.kcIds.all { kcId ->
            val state = states[kcId]
            val hasEvidence = state != null && (state.totalAttempts + state.exposureCount) > 0
            val readinessOk = hasEvidence || graph.readiness(kcId, states) >= policy.readinessThreshold
            graph.isUnlocked(
                kcId = kcId,
                states = states,
                nowEpochMs = nowEpochMs,
                policy = policy
            ) && readinessOk && phaseAllows(exercise, pedagogicalPhase(state, nowEpochMs))
        }
    }

    private fun score(
        exercise: ExerciseDefinition,
        snapshot: LearningSnapshot,
        states: Map<String, LearnerKcState>,
        graph: KnowledgeGraphEngine,
        recentInteractions: List<InteractionRecord>,
        nowEpochMs: Long,
        newProportion: Double = 0.25,
        reviewProportion: Double = 0.50,
        transferProportion: Double = 0.25
    ): PlannerCandidate {
        val kcCount = exercise.kcIds.size.coerceAtLeast(1)
        val diagnosticWeights = diagnostic.diagnosticWeights(exercise, states)
        val sumDiag = exercise.kcIds.sumOf { diagnosticWeights[it] ?: (1.0 / kcCount) }.coerceAtLeast(1e-6)

        var weightedRetentionUrgency = 0.0
        var weightedMasteryGap = 0.0
        var weightedAutoGap = 0.0
        var weightedInfo = 0.0
        var weightedUnlock = 0.0
        var weightedImportance = 0.0
        var weightedReadiness = 0.0
        var weightedTargetDiff = 0.0
        var weightedStrategy = 0.0
        var weightedLatentMastery = 0.0
        var weightedLatentUncertainty = 0.0
        var totalAttemptsAll = 0

        for (kcId in exercise.kcIds) {
            val w = (diagnosticWeights[kcId] ?: (1.0 / kcCount)) / sumDiag
            val component = snapshot.componentById[kcId]
            val state = states[kcId] ?: LearnerKcState(kcId, mastery = component?.priorMastery ?: 0.15)
            totalAttemptsAll += state.totalAttempts

            val elapsedHours = state.lastReviewedAtEpochMs?.let { (nowEpochMs - it).coerceAtLeast(0L) / 3_600_000.0 } ?: 9999.0
            val recall = if (state.lastReviewedAtEpochMs == null) 0.35 else onlineModels.recallProbability(kcId, elapsedHours, state.halfLifeHours)
            val recallDeficit = (policy.targetRecall - recall).coerceAtLeast(0.0)
            val retentionUrgency = (recallDeficit / policy.targetRecall.coerceAtLeast(0.01)).coerceIn(0.0, 1.0)
            val masteryGap = (1.0 - state.mastery).coerceIn(0.0, 1.0)
            val autoGap = (1.0 - state.automaticity).coerceIn(0.0, 1.0)
            val info = MathUtils.binaryEntropy(state.mastery) * state.uncertainty.coerceAtLeast(0.15)
            val unlock = graph.unlockValue(kcId, states)
            val importance = component?.importance ?: 0.5
            val readiness = graph.readiness(kcId, states)
            val latent = onlineModels.latentPosterior(kcId, state.mastery, state.halfLifeHours)
            val effectiveMastery = (0.72 * state.mastery + 0.28 * latent.masteryMean).coerceIn(0.0, 1.0)
            val targetDiff = (0.25 + 0.65 * effectiveMastery).coerceIn(0.25, 0.90)
            val strategySample = onlineModels.strategySample(kcId, exercise.type)

            weightedRetentionUrgency += w * retentionUrgency
            weightedMasteryGap += w * masteryGap
            weightedAutoGap += w * autoGap
            weightedInfo += w * info
            weightedUnlock += w * unlock
            weightedImportance += w * importance
            weightedReadiness += w * readiness
            weightedTargetDiff += w * targetDiff
            weightedStrategy += w * strategySample
            weightedLatentMastery += w * latent.masteryMean
            weightedLatentUncertainty += w * (latent.masterySd * 2.5).coerceIn(0.04, 0.95)
        }

        val qSuccess = diagnostic.predictedCorrect(exercise, states)
        val mirtTheta = mapOf(
            "mastery" to MathUtils.logit(weightedLatentMastery.coerceIn(0.02, 0.98)),
            "automaticity" to MathUtils.logit((1.0 - weightedAutoGap).coerceIn(0.02, 0.98)),
            "readiness" to MathUtils.logit(weightedReadiness.coerceIn(0.02, 0.98))
        )
        val mirtSuccess = MirtModel.probability(
            mirtTheta,
            MirtItem(
                discrimination = mapOf("mastery" to 0.62, "automaticity" to 0.20, "readiness" to 0.18),
                difficulty = MathUtils.logit(exercise.difficulty.coerceIn(0.05, 0.95)),
                guessing = if (exercise.type == ExerciseType.TEACH) 0.0 else 0.12
            )
        )
        val successP = (0.68 * qSuccess + 0.32 * mirtSuccess).coerceIn(0.02, 0.98)
        val difficultyMatch = exp(-2.2 * abs(exercise.difficulty - weightedTargetDiff))
        val durationPenalty = (exercise.estimatedSeconds / 90.0).coerceIn(0.0, 1.0)
        val baseRiskPenalty = when {
            successP < 0.25 && exercise.type != ExerciseType.TEACH -> 0.9
            successP < 0.40 -> 0.35
            successP > 0.96 && weightedMasteryGap < 0.12 -> 0.25
            else -> 0.0
        }
        // Lower-tail uncertainty penalty: approximates CVaR behaviour without
        // running a costly Monte-Carlo tree at every mobile turn.
        val lowerTail = ((0.52 - successP).coerceAtLeast(0.0) / 0.52)
        val cvarPenalty = (lowerTail * (0.45 + 0.55 * weightedLatentUncertainty)).coerceIn(0.0, 1.0)
        val riskPenalty = (0.72 * baseRiskPenalty + 0.28 * cvarPenalty).coerceIn(0.0, 1.0)
        val teachingBoost = if (exercise.type == ExerciseType.TEACH && totalAttemptsAll == 0) 0.55 else 0.0
        val newContentReadinessPenalty = if (totalAttemptsAll == 0 && exercise.type != ExerciseType.TEACH && weightedReadiness < policy.readinessThreshold) 0.50 else 0.0

        val role = classifyRole(exercise, states)
        val quotaBoost = when (role) {
            LearningRole.NEW -> (policy.newContentFraction - newProportion).coerceIn(-0.15, 0.20)
            LearningRole.REVIEW -> (policy.reviewFraction - reviewProportion).coerceIn(-0.15, 0.20)
            LearningRole.TRANSFER -> (policy.transferFraction - transferProportion).coerceIn(-0.15, 0.20)
        }

        // Wheel spinning eval sobre todos los KCs involucrados
        val allExerciseIds = snapshot.exercises.filter { ex -> ex.kcIds.any { it in exercise.kcIds } }.map { it.id }.toSet()
        val wheel = WheelSpinningDetector.detect(allExerciseIds, recentInteractions)
        val remediationBoost = when {
            !wheel.detected -> 0.0
            exercise.type == ExerciseType.TEACH -> 0.70
            "remedial" in exercise.tags || "contrast" in exercise.tags -> 0.35
            else -> -0.45
        }
        val explorationBonus = 0.06 * (weightedStrategy - 0.5)

        val utility = (
            0.23 * weightedRetentionUrgency +
            0.19 * weightedMasteryGap +
            0.10 * weightedAutoGap +
            0.15 * weightedInfo +
            0.08 * weightedUnlock.coerceIn(0.0, 1.0) +
            0.10 * difficultyMatch +
            0.08 * weightedImportance +
            0.07 * weightedReadiness +
            teachingBoost +
            remediationBoost +
            explorationBonus +
            0.12 * quotaBoost -
            0.08 * durationPenalty -
            0.18 * riskPenalty -
            newContentReadinessPenalty
        )

        return PlannerCandidate(
            exercise = exercise,
            utility = utility,
            successProbability = successP,
            retentionUrgency = weightedRetentionUrgency,
            informationValue = weightedInfo,
            unlockValue = weightedUnlock,
            riskPenalty = riskPenalty,
            rationale = "gap=${fmt(weightedMasteryGap)}, info=${fmt(weightedInfo)}, readiness=${fmt(weightedReadiness)}, latent=${fmt(weightedLatentMastery)}, qP=${fmt(qSuccess)}, mirtP=${fmt(mirtSuccess)}, wheel=${wheel.detected}"
        )
    }

    private fun modeAllows(
        mode: SessionMode,
        exercise: ExerciseDefinition,
        components: Map<String, KnowledgeComponent>
    ): Boolean {
        if (mode == SessionMode.ADAPTIVE) return true
        val domains = exercise.kcIds.mapNotNull { components[it]?.domain }.toSet()
        return when (mode) {
            SessionMode.PLACEMENT -> "placement" in exercise.tags
            SessionMode.VOCABULARY -> domains.any { it == KcDomain.VOCABULARY || it == KcDomain.CHUNK }
            SessionMode.GRAMMAR -> KcDomain.GRAMMAR in domains
            SessionMode.LISTENING -> KcDomain.LISTENING in domains || exercise.type == ExerciseType.LISTENING_AB
            SessionMode.SPELLING -> domains.any { it == KcDomain.ORTHOGRAPHY || it == KcDomain.LETTER } || exercise.type == ExerciseType.SPELLING_AB || exercise.type == ExerciseType.SPELL_FROM_AUDIO
            SessionMode.PRONUNCIATION -> KcDomain.PHONOLOGY in domains || exercise.type == ExerciseType.PRON_DISCRIMINATION
            SessionMode.SPEAKING, SessionMode.READING, SessionMode.WRITING -> false
            SessionMode.ADAPTIVE -> true
        }
    }


    private fun pedagogicalPhase(state: LearnerKcState?, nowEpochMs: Long): PedagogicalPhase {
        if (state == null || (state.totalAttempts + state.exposureCount) == 0) return PedagogicalPhase.DIAGNOSIS
        if (state.totalAttempts == 0) return PedagogicalPhase.TEACHING
        val elapsed = state.lastReviewedAtEpochMs?.let { (nowEpochMs - it).coerceAtLeast(0L) / 3_600_000.0 }
        val recall = if (elapsed != null) HalfLifeModel.recallProbability(elapsed, state.halfLifeHours) else 0.0
        return when {
            recall < policy.targetRecall -> PedagogicalPhase.REVIEW
            state.mastery >= 0.72 && state.transferSuccesses < policy.checkpointMinTransferSuccesses -> PedagogicalPhase.TRANSFER
            else -> PedagogicalPhase.PRACTICE
        }
    }

    private fun phaseAllows(exercise: ExerciseDefinition, phase: PedagogicalPhase): Boolean = when (phase) {
        PedagogicalPhase.DIAGNOSIS -> exercise.type == ExerciseType.TEACH || exercise.type in setOf(
            ExerciseType.AB,
            ExerciseType.MEANING_AB,
            ExerciseType.LISTENING_AB,
            ExerciseType.SPELLING_AB,
            ExerciseType.PRON_DISCRIMINATION,
            ExerciseType.CHUNK_AB
        )
        PedagogicalPhase.TEACHING -> exercise.type == ExerciseType.TEACH || "remedial" in exercise.tags
        PedagogicalPhase.PRACTICE -> true
        PedagogicalPhase.TRANSFER -> exercise.kcIds.size > 1 || exercise.type in setOf(ExerciseType.SPELL_FROM_AUDIO, ExerciseType.PRON_DISCRIMINATION, ExerciseType.CHUNK_AB) || "transfer" in exercise.tags
        PedagogicalPhase.REVIEW -> exercise.type != ExerciseType.TEACH || "remedial" in exercise.tags
    }

    private fun fmt(v: Double) = "%.2f".format(java.util.Locale.US, v)
}

