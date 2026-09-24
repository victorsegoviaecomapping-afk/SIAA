package com.siaa.core.runtime

import com.siaa.core.algorithm.ExercisePlanner
import com.siaa.core.algorithm.PlannerCandidate
import com.siaa.core.algorithm.StateUpdater
import com.siaa.core.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LessonRuntimeUnitTest {

    private class TestRepo(
        val exs: List<ExerciseDefinition> = defaultExercises(),
        initialState: LearnerKcState? = null
    ) : LearningRepository {
        val comps = listOf(KnowledgeComponent("G", "3sg", "A1", KcDomain.GRAMMAR, priorMastery = 0.2))
        val states = mutableMapOf<String, LearnerKcState>()
        val interactions = mutableListOf<InteractionRecord>()
        val events = mutableListOf<RuntimeEvent>()
        val misconceptions = mutableMapOf<String, Misconception>()
        var active: SessionRecord? = null
        var nextSessionId = 1L

        init {
            if (initialState != null) states[initialState.kcId] = initialState
        }

        override suspend fun loadSnapshot() = LearningSnapshot(comps, emptyList(), states.values.toList(), exs)
        override suspend fun createSession(mode: SessionMode, nowEpochMs: Long): Long {
            val id = nextSessionId++
            active = SessionRecord(id = id, mode = mode, startedAtEpochMs = nowEpochMs)
            return id
        }
        override suspend fun finishSession(sessionId: Long, nowEpochMs: Long) {
            if (active?.id == sessionId) active = active?.copy(endedAtEpochMs = nowEpochMs)
        }
        override suspend fun activeSession(): SessionRecord? = active?.takeIf { it.endedAtEpochMs == null }
        override suspend fun recordInteraction(record: InteractionRecord): Long {
            interactions += record
            return interactions.size.toLong()
        }
        override suspend fun updateLearnerState(state: LearnerKcState) { states[state.kcId] = state }
        override suspend fun commitTurn(
            interaction: InteractionRecord,
            updatedStates: List<LearnerKcState>,
            misconceptionUpdates: List<Misconception>
        ): Long {
            check(interactions.none { it.sessionId == interaction.sessionId && it.turnId == interaction.turnId })
            updatedStates.forEach { states[it.kcId] = it }
            misconceptionUpdates.forEach { misconceptions[it.id] = it }
            interactions += interaction
            return interactions.size.toLong()
        }
        override suspend fun recordRuntimeEvent(event: RuntimeEvent): Long {
            events += event
            return events.size.toLong()
        }
        override suspend fun recentRuntimeEvents(limit: Int) = events.asReversed().take(limit)
        override suspend fun recentInteractions(limit: Int) = interactions.asReversed().take(limit)
        override suspend fun hasInteraction(sessionId: Long, turnId: Long) =
            interactions.any { it.sessionId == sessionId && it.turnId == turnId }
        override suspend fun dashboardStats(nowEpochMs: Long) =
            DashboardStats(1, 0, 0, 0.0, 0.0, interactions.size, "A1")
        override suspend fun saveDeviceProfile(profile: DeviceProfile) = 1L
        override suspend fun latestDeviceProfile(): DeviceProfile? = null

        companion object {
            fun defaultExercises() = listOf(
                ExerciseDefinition(
                    "E1", ExerciseType.AB, listOf("G"), "A1", .4, "Cuál?",
                    optionA = "she work", optionB = "she works", correctOption = "B",
                    explanationEs = "Con she usamos works"
                ),
                ExerciseDefinition(
                    "E2", ExerciseType.SELF_ASSESS, listOf("G"), "A1", .5,
                    "Construye mentalmente: ella trabaja", explanationEs = "She works"
                )
            )
        }
    }

    private class TestSpeech(
        private val delayForText: (String) -> Long = { 0L }
    ) : SpeechPort {
        val spoken = mutableListOf<String>()
        override suspend fun speak(text: String, languageTag: String, rate: Float) {
            spoken += text
            val d = delayForText(text)
            if (d > 0) delay(d)
        }
        override fun stop() = Unit
        override fun shutdown() = Unit
    }

    private class TestEarcon : EarconPort {
        val played = mutableListOf<EarconKind>()
        override fun play(kind: EarconKind) { played += kind }
        override fun release() = Unit
    }

    private class TestPlanner : ExercisePlanner {
        override fun rank(
            mode: SessionMode,
            snapshot: LearningSnapshot,
            recentInteractions: List<InteractionRecord>,
            nowEpochMs: Long,
            limit: Int
        ): List<PlannerCandidate> {
            val seen = recentInteractions.map { it.exerciseId }.toSet()
            val e = snapshot.exercises.firstOrNull { it.id !in seen } ?: return emptyList()
            return listOf(PlannerCandidate(e, .5, .6, .5, .5, 0.0, 0.0, "unit_test"))
        }
    }

    private fun TestScope.advanceUntilState(runtime: LessonRuntime, target: LessonState, maxMs: Long = 10_000L) {
        var elapsed = 0L
        while (elapsed <= maxMs) {
            runCurrent()
            if (runtime.snapshot.value.state == target) return
            advanceTimeBy(10L)
            elapsed += 10L
        }
        error("Timeout waiting for $target. Actual snapshot: ${runtime.snapshot.value}")
    }

    private fun runtime(repo: TestRepo, speech: TestSpeech, ear: TestEarcon, scope: TestScope): LessonRuntime =
        LessonRuntime(
            repository = repo,
            planner = TestPlanner(),
            stateUpdater = StateUpdater(),
            speech = speech,
            earcon = ear,
            dispatcher = StandardTestDispatcher(scope.testScheduler)
        )

    @Test
    fun fullLessonTurnExecutionAndPersistence() = runTest {
        val repo = TestRepo()
        val runtime = runtime(repo, TestSpeech(), TestEarcon(), this)

        runtime.start(SessionConfig(SessionMode.GRAMMAR, maxItems = 2, announceControls = false))
        advanceUntilState(runtime, LessonState.WAITING_BINARY)
        assertTrue(repo.events.any { it.eventType == RuntimeEventType.SESSION_STARTED })

        assertTrue(runtime.onCommand(RuntimeCommand.SECONDARY))
        advanceUntilState(runtime, LessonState.WAITING_SELF_ASSESSMENT)

        assertTrue(runtime.onCommand(RuntimeCommand.PRIMARY))
        advanceUntilState(runtime, LessonState.SESSION_END)

        assertEquals(2, repo.interactions.size)
        assertEquals(2, runtime.snapshot.value.turnsCompleted)
        assertEquals(2, runtime.snapshot.value.gradedResponses)
        assertEquals(2, runtime.snapshot.value.correctResponses)
        assertTrue(repo.states.getValue("G").mastery > 0.2)
        runtime.shutdown()
    }

    @Test
    fun rapidCompetingAnswersCommitOnce() = runTest {
        val repo = TestRepo()
        val runtime = runtime(repo, TestSpeech(), TestEarcon(), this)
        runtime.start(SessionConfig(SessionMode.GRAMMAR, maxItems = 2, announceControls = false))
        advanceUntilState(runtime, LessonState.WAITING_BINARY)

        runtime.onCommand(RuntimeCommand.SECONDARY)
        runtime.onCommand(RuntimeCommand.PRIMARY)
        advanceUntilState(runtime, LessonState.WAITING_SELF_ASSESSMENT)

        assertEquals(1, repo.interactions.size)
        assertEquals(1L, repo.interactions.single().turnId)
        assertTrue(repo.events.any { it.eventType == RuntimeEventType.ANSWER_REJECTED_DUPLICATE })
        runtime.shutdown()
    }

    @Test
    fun backAndAnswerCannotRunTogether() = runTest {
        val repo = TestRepo()
        val runtime = runtime(repo, TestSpeech(), TestEarcon(), this)
        runtime.start(SessionConfig(SessionMode.GRAMMAR, maxItems = 2, announceControls = false))
        advanceUntilState(runtime, LessonState.WAITING_BINARY)

        assertTrue(runtime.onCommand(RuntimeCommand.BACK))
        assertTrue(runtime.onCommand(RuntimeCommand.SECONDARY)) // consumed/rejected by the turn gate
        runCurrent()
        assertTrue(repo.interactions.isEmpty())
        advanceUntilState(runtime, LessonState.WAITING_BINARY)
        runtime.shutdown()
    }

    @Test
    fun stopDuringPromptRemainsSessionEnd() = runTest {
        val repo = TestRepo()
        val speech = TestSpeech { text -> if (text == "Cuál?") 10_000L else 0L }
        val runtime = runtime(repo, speech, TestEarcon(), this)
        runtime.start(SessionConfig(SessionMode.GRAMMAR, maxItems = 1, announceControls = false))
        advanceUntilState(runtime, LessonState.SPEAKING)

        runtime.stop()
        runCurrent()
        advanceTimeBy(20_000L)
        runCurrent()

        assertEquals(LessonState.SESSION_END, runtime.snapshot.value.state)
        assertFalse(repo.events.any { it.eventType == RuntimeEventType.ERROR })
        assertTrue(repo.events.any { it.eventType == RuntimeEventType.TURN_CANCELLED })
        runtime.shutdown()
    }

    @Test
    fun routeLossDuringPromptRemainsPausedUntilExplicitResume() = runTest {
        val repo = TestRepo()
        val speech = TestSpeech { text -> if (text == "Cuál?") 10_000L else 0L }
        val runtime = runtime(repo, speech, TestEarcon(), this)
        runtime.start(SessionConfig(SessionMode.GRAMMAR, maxItems = 1, announceControls = false))
        advanceUntilState(runtime, LessonState.SPEAKING)

        runtime.pauseForRouteChange()
        runCurrent()
        advanceTimeBy(20_000L)
        runCurrent()
        assertEquals(LessonState.PAUSED, runtime.snapshot.value.state)

        assertTrue(runtime.onCommand(RuntimeCommand.PLAY))
        advanceUntilState(runtime, LessonState.WAITING_BINARY, 20_000L)
        runtime.shutdown()
    }

    @Test
    fun timeoutIsCensoredEvidenceAndEventuallyClosesTurn() = runTest {
        val initial = LearnerKcState(
            kcId = "G",
            mastery = 0.33,
            recognition = 0.40,
            halfLifeHours = 20.0,
            lastReviewedAtEpochMs = 1_234L,
            totalAttempts = 2,
            exposureCount = 1
        )
        val repo = TestRepo(initialState = initial)
        val ear = TestEarcon()
        val runtime = runtime(repo, TestSpeech(), ear, this)
        runtime.start(
            SessionConfig(
                mode = SessionMode.GRAMMAR,
                maxItems = 1,
                announceControls = false,
                policy = SessionPolicy(
                    binaryResponseTimeoutMs = 80L,
                    selfAssessmentTimeoutMs = 80L,
                    maxTimeoutRetries = 1
                )
            )
        )
        advanceUntilState(runtime, LessonState.SESSION_END, 5_000L)

        val interaction = repo.interactions.single()
        val after = repo.states.getValue("G")
        assertEquals(InteractionKind.TIMEOUT, interaction.kind)
        assertFalse(interaction.graded)
        assertEquals(initial.mastery, after.mastery)
        assertEquals(initial.recognition, after.recognition)
        assertEquals(initial.halfLifeHours, after.halfLifeHours)
        assertEquals(initial.lastReviewedAtEpochMs, after.lastReviewedAtEpochMs)
        assertEquals(initial.totalAttempts, after.totalAttempts)
        assertEquals(initial.exposureCount + 1, after.exposureCount)
        assertEquals(1, runtime.snapshot.value.turnsCompleted)
        assertEquals(0, runtime.snapshot.value.gradedResponses)
        assertTrue(EarconKind.INCORRECT !in ear.played)
        runtime.shutdown()
    }

    @Test
    fun teachIsExposureOnlyAndHonorsMaxItems() = runTest {
        val teach = ExerciseDefinition(
            id = "TEACH", type = ExerciseType.TEACH, kcIds = listOf("G"), cefr = "A1",
            difficulty = 0.2, promptEs = "Explicación", stimulusEn = "hello"
        )
        val initial = LearnerKcState(
            kcId = "G", mastery = 0.44, recognition = 0.51, production = 0.31,
            automaticity = 0.22, halfLifeHours = 20.0, lastReviewedAtEpochMs = 999L,
            totalAttempts = 4, exposureCount = 3
        )
        val repo = TestRepo(listOf(teach), initial)
        val runtime = runtime(repo, TestSpeech(), TestEarcon(), this)
        runtime.start(SessionConfig(SessionMode.GRAMMAR, maxItems = 1, announceControls = false))
        advanceUntilState(runtime, LessonState.SESSION_END)

        val after = repo.states.getValue("G")
        assertEquals(initial.mastery, after.mastery)
        assertEquals(initial.recognition, after.recognition)
        assertEquals(initial.production, after.production)
        assertEquals(initial.automaticity, after.automaticity)
        assertEquals(initial.halfLifeHours, after.halfLifeHours)
        assertEquals(initial.lastReviewedAtEpochMs, after.lastReviewedAtEpochMs)
        assertEquals(initial.totalAttempts, after.totalAttempts)
        assertEquals(initial.exposureCount + 1, after.exposureCount)
        val interaction = repo.interactions.single()
        assertEquals(InteractionKind.TEACH_EXPOSURE, interaction.kind)
        assertFalse(interaction.correct)
        assertFalse(interaction.graded)
        assertEquals(1, runtime.snapshot.value.turnsCompleted)
        runtime.shutdown()
    }

    @Test
    fun incorrectAnnotatedAnswerUpdatesMisconceptionOnlyWhenGraded() = runTest {
        val ex = ExerciseDefinition(
            "M", ExerciseType.AB, listOf("G"), "A1", .4, "q",
            optionA = "wrong", optionB = "right", correctOption = "B",
            misconceptionIds = listOf("M1")
        )
        val repo = TestRepo(listOf(ex))
        val runtime = runtime(repo, TestSpeech(), TestEarcon(), this)
        runtime.start(SessionConfig(SessionMode.GRAMMAR, maxItems = 1, announceControls = false))
        advanceUntilState(runtime, LessonState.WAITING_BINARY)
        runtime.onCommand(RuntimeCommand.PRIMARY)
        advanceUntilState(runtime, LessonState.SESSION_END)

        val interaction = repo.interactions.single()
        assertTrue(interaction.graded)
        assertFalse(interaction.correct)
        assertNotNull(interaction.stateAfterMastery)
        val misconception = repo.misconceptions["M1"]
        assertNotNull(misconception)
        assertTrue(misconception.probability > 0.10)
        runtime.shutdown()
    }
}
