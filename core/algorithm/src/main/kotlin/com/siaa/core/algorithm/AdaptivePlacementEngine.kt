package com.siaa.core.algorithm

import com.siaa.core.model.*
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln

/**
 * Lightweight CAT/placement estimator for the first-run diagnostic.
 * It intentionally uses only objective items tagged `placement` and never marks
 * a KC mastered merely because a neighbouring level was passed.
 */
class AdaptivePlacementEngine {
    private val levelAbility = mapOf(
        "PRE-A1" to -2.4,
        "PREA1" to -2.4,
        "A1" to -1.6,
        "A2" to -0.8,
        "B1" to 0.0,
        "B2" to 0.8,
        "C1" to 1.6,
        "C2" to 2.4
    )
    private val ordered = listOf("Pre-A1", "A1", "A2", "B1", "B2", "C1", "C2")

    fun rank(
        snapshot: LearningSnapshot,
        recentInteractions: List<InteractionRecord>,
        capabilities: SessionCapabilities,
        limit: Int
    ): List<PlannerCandidate> {
        val placementItems = snapshot.exercises.filter {
            "placement" in it.tags && it.type != ExerciseType.TEACH && capabilities.canPresent(it.type)
        }
        if (placementItems.isEmpty()) return emptyList()
        val used = recentInteractions.filter { it.graded }.map { it.exerciseId }.toSet()
        val estimates = estimateAbilities(snapshot, recentInteractions)
        val globalTheta = estimates.values.takeIf { it.isNotEmpty() }?.average() ?: -0.8

        return placementItems.asSequence()
            .filterNot { it.id in used }
            .map { item ->
                val domains = item.kcIds.mapNotNull { snapshot.componentById[it]?.domain }.distinct()
                val theta = domains.mapNotNull { estimates[it] }.takeIf { it.isNotEmpty() }?.average() ?: globalTheta
                val b = difficultyToTheta(item.difficulty)
                val p = logistic(theta - b)
                val information = (p * (1.0 - p) * 4.0).coerceIn(0.0, 1.0)
                val domainCoverage = domains.sumOf { domain ->
                    val n = recentInteractions.count { r ->
                        val ex = snapshot.exerciseById[r.exerciseId] ?: return@count false
                        ex.kcIds.any { snapshot.componentById[it]?.domain == domain }
                    }
                    1.0 / (1.0 + n)
                } / domains.size.coerceAtLeast(1)
                val levelDistance = abs(theta - levelTheta(item.cefr))
                val utility = 0.62 * information + 0.28 * domainCoverage + 0.10 * exp(-levelDistance)
                PlannerCandidate(
                    exercise = item,
                    utility = utility,
                    successProbability = p,
                    retentionUrgency = 0.0,
                    informationValue = information,
                    unlockValue = domainCoverage,
                    riskPenalty = 0.0,
                    rationale = "placement theta=${fmt(theta)}, b=${fmt(b)}, info=${fmt(information)}, coverage=${fmt(domainCoverage)}"
                )
            }
            .sortedWith(compareByDescending<PlannerCandidate> { it.utility }.thenBy { it.exercise.id })
            .take(limit)
            .toList()
    }

    fun estimate(snapshot: LearningSnapshot, interactions: List<InteractionRecord>): PlacementProfile {
        val objective = interactions.filter { it.graded && snapshot.exerciseById[it.exerciseId]?.tags?.contains("placement") == true }
        val byDomain = KcDomain.entries.associateWith { domain ->
            val items = objective.mapNotNull { r ->
                val ex = snapshot.exerciseById[r.exerciseId] ?: return@mapNotNull null
                if (ex.kcIds.none { snapshot.componentById[it]?.domain == domain }) return@mapNotNull null
                ex to r
            }
            estimateTheta(items)
        }.filterValues { it != null }.mapValues { it.value!! }
        val overallTheta = byDomain.values.takeIf { it.isNotEmpty() }?.average()
            ?: estimateTheta(objective.mapNotNull { r -> snapshot.exerciseById[r.exerciseId]?.let { it to r } })
            ?: -1.6
        val domainCefr = byDomain.mapValues { (_, theta) -> thetaToCefr(theta) }
        val reliability = (1.0 - exp(-objective.size / 18.0)).coerceIn(0.0, 0.98)
        return PlacementProfile(
            overallCefr = thetaToCefr(overallTheta),
            domainCefr = domainCefr,
            domainAbility = byDomain,
            answeredItems = objective.size,
            reliability = reliability
        )
    }

    fun seedMastery(kc: KnowledgeComponent, profile: PlacementProfile): Double {
        val theta = profile.domainAbility[kc.domain] ?: levelTheta(profile.overallCefr)
        val delta = theta - levelTheta(kc.cefr)
        // Conservative prior only: placement can initialize but not certify mastery.
        return (0.05 + 0.67 * logistic(delta / 0.72)).coerceIn(0.05, 0.72)
    }

    private fun estimateAbilities(snapshot: LearningSnapshot, interactions: List<InteractionRecord>): Map<KcDomain, Double> {
        val objective = interactions.filter { it.graded && snapshot.exerciseById[it.exerciseId]?.tags?.contains("placement") == true }
        return KcDomain.entries.mapNotNull { domain ->
            val items = objective.mapNotNull { r ->
                val ex = snapshot.exerciseById[r.exerciseId] ?: return@mapNotNull null
                if (ex.kcIds.none { snapshot.componentById[it]?.domain == domain }) return@mapNotNull null
                ex to r
            }
            estimateTheta(items)?.let { domain to it }
        }.toMap()
    }

    private fun estimateTheta(items: List<Pair<ExerciseDefinition, InteractionRecord>>): Double? {
        if (items.isEmpty()) return null
        // Small Newton-Raphson 1PL update with N(0, 1.7^2) prior to keep sparse domains stable.
        var theta = 0.0
        repeat(10) {
            var grad = -theta / (1.7 * 1.7)
            var hess = -1.0 / (1.7 * 1.7)
            for ((ex, r) in items) {
                val p = logistic(theta - difficultyToTheta(ex.difficulty))
                val y = if (r.correct) 1.0 else 0.0
                grad += y - p
                hess -= p * (1.0 - p)
            }
            if (hess == 0.0) return@repeat
            theta = (theta - grad / hess).coerceIn(-3.0, 3.0)
        }
        return theta
    }

    private fun difficultyToTheta(difficulty: Double): Double = ((difficulty.coerceIn(0.03, 0.97) - 0.5) * 5.2)
    private fun levelTheta(level: String): Double = levelAbility[level.uppercase()] ?: levelAbility[level] ?: -1.6
    private fun thetaToCefr(theta: Double): String = ordered.minBy { abs(levelTheta(it) - theta) }
    private fun logistic(x: Double): Double = 1.0 / (1.0 + exp(-x))
    private fun fmt(v: Double) = "%.2f".format(java.util.Locale.US, v)
}
