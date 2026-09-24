package com.siaa.core.algorithm

import com.siaa.core.model.*
import kotlin.test.Test
import kotlin.test.assertTrue

class AlgorithmTest {
    @Test fun bktCorrectRaisesMastery() {
        val before = 0.35
        val after = BktUpdater.posterior(before, true)
        assertTrue(after > before)
    }

    @Test fun halfLifeDecaysToHalf() {
        val p = HalfLifeModel.recallProbability(12.0, 12.0)
        assertTrue(kotlin.math.abs(p - 0.5) < 1e-9)
    }

    @Test fun prerequisiteLocksChild() {
        val comps = listOf(
            KnowledgeComponent("A", "A", "A1", KcDomain.GRAMMAR),
            KnowledgeComponent("B", "B", "A1", KcDomain.GRAMMAR)
        )
        val graph = KnowledgeGraphEngine(comps, listOf(KnowledgeEdge("A", "B")))
        val states = mapOf("A" to LearnerKcState("A", mastery = 0.20))
        assertTrue(!graph.isUnlocked("B", states))
    }

    @Test fun plannerRespectsSessionCapabilities() {
        val comps = listOf(KnowledgeComponent("G", "Grammar", "A1", KcDomain.GRAMMAR))
        val exercises = listOf(
            ExerciseDefinition("E_BINARY", ExerciseType.AB, listOf("G"), "A1", 0.4, "Pregunta A/B"),
            ExerciseDefinition("E_SELF", ExerciseType.SELF_ASSESS, listOf("G"), "A1", 0.5, "Autoevaluación")
        )
        val snapshot = LearningSnapshot(comps, emptyList(), listOf(LearnerKcState("G", mastery = 0.5)), exercises)
        val planner = AdaptiveUtilityPlanner()

        // With default capabilities (3-way available), self-assess is allowed
        val fullRank = planner.rank(SessionMode.GRAMMAR, snapshot, emptyList(), 0L, 10, SessionCapabilities(hasBack = true))
        assertTrue(fullRank.any { it.exercise.id == "E_SELF" })

        // Without back button, 3-way self-assessment cannot be presented
        val restrictedRank = planner.rank(SessionMode.GRAMMAR, snapshot, emptyList(), 0L, 10, SessionCapabilities(hasBack = false))
        assertTrue(restrictedRank.none { it.exercise.id == "E_SELF" })
        assertTrue(restrictedRank.any { it.exercise.id == "E_BINARY" })
    }

    @Test fun pomdpIsInvariantToKcOrder() {
        val kc1 = KnowledgeComponent("KC1", "KC 1", "A1", KcDomain.VOCABULARY)
        val kc2 = KnowledgeComponent("KC2", "KC 2", "A1", KcDomain.VOCABULARY)
        val ex1 = ExerciseDefinition("E1", ExerciseType.AB, listOf("KC1", "KC2"), "A1", 0.4, "Exercise 1")
        val ex2 = ExerciseDefinition("E2", ExerciseType.AB, listOf("KC2", "KC1"), "A1", 0.4, "Exercise 1 reversed")

        val snapshot1 = LearningSnapshot(
            components = listOf(kc1, kc2),
            edges = emptyList(),
            states = listOf(
                LearnerKcState("KC1", mastery = 0.4),
                LearnerKcState("KC2", mastery = 0.6)
            ),
            exercises = listOf(ex1)
        )
        val snapshot2 = LearningSnapshot(
            components = listOf(kc1, kc2),
            edges = emptyList(),
            states = listOf(
                LearnerKcState("KC1", mastery = 0.4),
                LearnerKcState("KC2", mastery = 0.6)
            ),
            exercises = listOf(ex2)
        )

        val fixedBase = object : ExercisePlanner {
            override fun rank(
                mode: SessionMode,
                snapshot: LearningSnapshot,
                recentInteractions: List<InteractionRecord>,
                nowEpochMs: Long,
                limit: Int,
                capabilities: SessionCapabilities
            ): List<PlannerCandidate> = snapshot.exercises.map {
                PlannerCandidate(
                    exercise = it,
                    utility = 1.0,
                    successProbability = 0.7,
                    retentionUrgency = 0.3,
                    informationValue = 0.4,
                    unlockValue = 0.2,
                    riskPenalty = 0.0,
                    rationale = "test"
                )
            }
        }
        val planner = PomdpLookaheadPlanner(base = fixedBase)
        val rank1 = planner.rank(SessionMode.ADAPTIVE, snapshot1, emptyList(), 1000L, 5)
        val rank2 = planner.rank(SessionMode.ADAPTIVE, snapshot2, emptyList(), 1000L, 5)

        assertTrue(rank1.isNotEmpty())
        assertTrue(rank2.isNotEmpty())
        assertTrue(kotlin.math.abs(rank1[0].utility - rank2[0].utility) < 1e-6)
    }
}
