import com.siaa.core.algorithm.*
import com.siaa.core.model.*

fun main() {
    val now = System.currentTimeMillis()
    val components = listOf(
        KnowledgeComponent("G_BASE", "Present simple", "A1", KcDomain.GRAMMAR, importance = 0.9),
        KnowledgeComponent("G_3SG", "Third person s", "A1", KcDomain.GRAMMAR, importance = 0.9)
    )
    val edges = listOf(KnowledgeEdge("G_BASE", "G_3SG"))
    val masteredBase = LearnerKcState(
        kcId = "G_BASE",
        mastery = 0.90,
        recognition = 0.90,
        production = 0.85,
        automaticity = 0.70,
        halfLifeHours = 24.0 * 30.0,
        lastReviewedAtEpochMs = now,
        totalAttempts = 8,
        totalCorrect = 7,
        consecutiveSuccess = 3,
        transferSuccesses = 1,
        novelSuccesses = 2
    )
    val states = listOf(
        masteredBase,
        LearnerKcState("G_3SG", mastery = 0.45, halfLifeHours = 12.0, totalAttempts = 3)
    )
    val exercises = listOf(
        ExerciseDefinition("E1", ExerciseType.AB, listOf("G_3SG"), "A1", 0.45, "Which?", optionA="She work", optionB="She works", correctOption="B"),
        ExerciseDefinition("E2", ExerciseType.TEACH, listOf("G_3SG"), "A1", 0.25, "Rule")
    )
    val snapshot = LearningSnapshot(components, edges, states, exercises)
    val planner = PomdpLookaheadPlanner()
    val ranked = planner.rank(SessionMode.GRAMMAR, snapshot, emptyList(), now, 5)
    check(ranked.isNotEmpty())
    println("top=${ranked.first().exercise.id} utility=${ranked.first().utility} ${ranked.first().rationale}")
    check(BktUpdater.posterior(0.4, true) > 0.4)
    check(kotlin.math.abs(HalfLifeModel.recallProbability(10.0, 10.0) - 0.5) < 1e-9)

    // Checkpoint must require transfer + novelty, not only repeated mastery.
    val repeatedOnly = masteredBase.copy(transferSuccesses = 0, novelSuccesses = 0)
    check(!MasteryCheckpointEvaluator.evaluate(repeatedOnly, now).passed)
    check(MasteryCheckpointEvaluator.evaluate(masteredBase, now).passed)

    // Readiness is a hard eligibility constraint for new content, even when the prerequisite is soft.
    val lowParent = KnowledgeComponent("P", "Parent", "A1", KcDomain.GRAMMAR)
    val newChild = KnowledgeComponent("C", "Child", "A1", KcDomain.GRAMMAR)
    val lowSnapshot = LearningSnapshot(
        components = listOf(lowParent, newChild),
        edges = listOf(KnowledgeEdge("P", "C", hardPrerequisite = false)),
        states = listOf(LearnerKcState("P", mastery = 0.20, totalAttempts = 1)),
        exercises = listOf(ExerciseDefinition("C1", ExerciseType.AB, listOf("C"), "A1", .4, "q", optionA="a", optionB="b", correctOption="A"))
    )
    check(AdaptiveUtilityPlanner().rank(SessionMode.GRAMMAR, lowSnapshot, emptyList(), now, 5).isEmpty())

    // CEFR denominator only includes KCs with an assessment path, so unassessable KCs cannot block a level forever.
    val cefrComponents = listOf(
        KnowledgeComponent("A", "Assessable", "Pre-A1", KcDomain.VOCABULARY),
        KnowledgeComponent("U", "Unassessable", "Pre-A1", KcDomain.VOCABULARY)
    )
    val cefrState = masteredBase.copy(kcId = "A")
    val cefrSnapshot = LearningSnapshot(
        components = cefrComponents,
        edges = emptyList(),
        states = listOf(cefrState),
        exercises = listOf(ExerciseDefinition("AE", ExerciseType.AB, listOf("A"), "Pre-A1", .3, "q", optionA="a", optionB="b", correctOption="A"))
    )
    check(CefrProgressEstimator.estimate(cefrSnapshot, now).endsWith("Pre-A1"))

    // SELF_ASSESS positive evidence is intentionally weaker than objective evidence.
    val updater = StateUpdater()
    val prior = LearnerKcState("A", mastery=.40)
    val comp = cefrComponents.first()
    val objective = updater.update(comp, prior, cefrSnapshot.exercises.first(), true, null, 1000, now, 1.0)
    val selfEx = ExerciseDefinition("S", ExerciseType.SELF_ASSESS, listOf("A"), "Pre-A1", .3, "mental")
    val self = updater.update(comp, prior, selfEx, true, ResponseConfidence.CORRECT, 1000, now, SessionPolicy().selfAssessPositiveWeight)
    check(self.mastery - prior.mastery < objective.mastery - prior.mastery)

    println("SIAA algorithm smoke OK")
}
