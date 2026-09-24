package com.siaa.core.runtime

import com.siaa.core.algorithm.ExercisePlanner
import com.siaa.core.algorithm.ConfigurableExercisePlanner
import com.siaa.core.algorithm.OnlineAdaptiveModels
import com.siaa.core.algorithm.QMatrixDiagnostic
import com.siaa.core.algorithm.StateUpdater
import com.siaa.core.algorithm.SelfAssessmentReliabilityEstimator
import com.siaa.core.model.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LessonRuntime(
    private val repository: LearningRepository,
    private val planner: ExercisePlanner,
    private val stateUpdater: StateUpdater,
    private val speech: SpeechPort,
    private val earcon: EarconPort,
    private val diagnostic: QMatrixDiagnostic = QMatrixDiagnostic(),
    private val onlineModels: OnlineAdaptiveModels = OnlineAdaptiveModels(),
    private val clock: ClockPort = SystemClockPort,
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val _snapshot = MutableStateFlow(RuntimeSnapshot())
    val snapshot: StateFlow<RuntimeSnapshot> = _snapshot.asStateFlow()

    private var config = SessionConfig()
    private var activeExercise: ExerciseDefinition? = null
    private var activePlannerScore: Double? = null
    private var currentTurn = TurnContext()

    private val answerAccepted = AtomicBoolean(true)
    private val turnAction = AtomicReference(TurnActionState.CLOSED)
    private val sessionClosing = AtomicBoolean(false)
    private val executionGeneration = AtomicLong(0L)

    private var sessionJob: Job? = null
    private var turnJob: Job? = null
    private var timeoutJob: Job? = null

    private suspend fun speakText(text: String, languageTag: String = "es-PE", rate: Float = 1.0f) {
        speech.speak(text, languageTag, (config.speechRate * rate).coerceIn(0.5f, 2.0f))
    }

    fun start(config: SessionConfig = SessionConfig()) {
        if (_snapshot.value.state !in setOf(LessonState.IDLE, LessonState.SESSION_END, LessonState.ERROR)) return

        this.config = config.copy(
            maxItems = if (config.mode == SessionMode.PLACEMENT) config.maxItems.coerceIn(18, 42) else config.maxItems,
            targetDurationMinutes = config.targetDurationMinutes.coerceIn(5, 180),
            policy = config.policy.forIntensity(config.intensity)
        )
        (planner as? ConfigurableExercisePlanner)?.updatePolicy(this.config.policy)
        val generation = executionGeneration.incrementAndGet()
        sessionClosing.set(false)
        turnAction.set(TurnActionState.CLOSED)
        answerAccepted.set(true)

        timeoutJob?.cancel()
        turnJob?.cancel()
        sessionJob?.cancel()
        speech.stop()

        sessionJob = scope.launch {
            try {
                if (!isCurrent(generation)) return@launch
                startOrRestoreSession(generation)
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                if (isCurrent(generation) && !sessionClosing.get()) fail(t)
            }
        }
    }

    fun stop() {
        val snap = _snapshot.value
        if (snap.state in setOf(LessonState.IDLE, LessonState.SESSION_END)) return
        if (snap.state == LessonState.FINISHING) {
            speech.stop()
            return
        }
        if (!sessionClosing.compareAndSet(false, true)) return

        val sessionId = snap.sessionId
        val turnId = currentTurn.turnId
        executionGeneration.incrementAndGet()
        turnAction.set(TurnActionState.CLOSED)
        answerAccepted.set(true)

        timeoutJob?.cancel()
        turnJob?.cancel()
        sessionJob?.cancel()
        speech.stop()

        // Make the terminal state visible immediately, before any slow persistence operation.
        _snapshot.value = snap.copy(
            state = LessonState.SESSION_END,
            message = "Sesión finalizada",
            promptFinishedAtMs = null,
            pausedFrom = null,
            pauseReason = null
        )

        scope.launch {
            recordEvent(
                eventType = RuntimeEventType.TURN_CANCELLED,
                stateBefore = snap.state.name,
                stateAfter = LessonState.SESSION_END.name,
                payload = "STOP",
                sessionIdOverride = sessionId,
                turnIdOverride = turnId
            )
            closeSessionInternal(sessionId, turnId, snap.state, "Sesión finalizada", announce = false)
        }
    }

    fun shutdown() {
        executionGeneration.incrementAndGet()
        sessionClosing.set(true)
        turnAction.set(TurnActionState.CLOSED)
        answerAccepted.set(true)
        timeoutJob?.cancel()
        turnJob?.cancel()
        sessionJob?.cancel()
        speech.stop()
        speech.shutdown()
        earcon.release()
        scope.cancel()
    }

    fun pauseForRouteChange() {
        pauseInternal(PauseReason.AUDIO_ROUTE_LOST, "Pausado: se desconectó la salida de audio", emitRouteLost = true)
    }

    fun pauseForFocusLoss() {
        pauseInternal(PauseReason.AUDIO_FOCUS_LOST, "Pausado: se perdió el foco de audio", emitRouteLost = false)
    }

    private fun pauseInternal(reason: PauseReason, message: String, emitRouteLost: Boolean = false) {
        val current = _snapshot.value
        if (current.state in setOf(LessonState.IDLE, LessonState.PAUSED, LessonState.SESSION_END, LessonState.ERROR)) return
        if (current.state == LessonState.FINISHING) {
            // Safety first: never let the final announcement migrate to a newly exposed route.
            turnJob?.cancel()
            speech.stop()
            scope.launch {
                recordEvent(
                    eventType = RuntimeEventType.TURN_CANCELLED,
                    stateBefore = LessonState.FINISHING.name,
                    stateAfter = LessonState.SESSION_END.name,
                    payload = reason.name,
                    sessionIdOverride = current.sessionId,
                    turnIdOverride = current.turnId
                )
            }
            return
        }
        // Before a session id exists there is nothing safe to resume; ignore an ultra-early media pause.
        if (current.sessionId == null) return

        val sessionId = current.sessionId
        val turnId = currentTurn.turnId
        executionGeneration.incrementAndGet() // invalidates any coroutine that may finish late
        turnAction.set(TurnActionState.PAUSED)
        answerAccepted.set(true)
        timeoutJob?.cancel()
        turnJob?.cancel()
        speech.stop()

        _snapshot.value = current.copy(
            state = LessonState.PAUSED,
            message = message,
            pausedFrom = current.state,
            pauseReason = reason,
            promptFinishedAtMs = null
        )

        scope.launch {
            if (emitRouteLost) {
                recordEvent(
                    eventType = RuntimeEventType.ROUTE_LOST,
                    stateBefore = current.state.name,
                    stateAfter = LessonState.PAUSED.name,
                    payload = reason.name,
                    sessionIdOverride = sessionId,
                    turnIdOverride = turnId
                )
            }
            recordEvent(
                eventType = RuntimeEventType.TURN_CANCELLED,
                stateBefore = current.state.name,
                stateAfter = LessonState.PAUSED.name,
                payload = reason.name,
                sessionIdOverride = sessionId,
                turnIdOverride = turnId
            )
            recordEvent(
                eventType = RuntimeEventType.PAUSED,
                stateBefore = current.state.name,
                stateAfter = LessonState.PAUSED.name,
                payload = reason.name,
                sessionIdOverride = sessionId,
                turnIdOverride = turnId
            )
        }
    }

    fun onCommand(command: RuntimeCommand): Boolean {
        val snap = _snapshot.value
        scope.launch {
            recordEvent(
                eventType = RuntimeEventType.COMMAND_RECEIVED,
                stateBefore = snap.state.name,
                stateAfter = snap.state.name,
                runtimeCommand = command.name,
                sessionIdOverride = snap.sessionId,
                turnIdOverride = snap.turnId
            )
        }

        return when (snap.state) {
            LessonState.WAITING_BINARY -> handleBinary(command)
            LessonState.WAITING_SELF_ASSESSMENT -> handleSelfAssessment(command)
            LessonState.SPEAKING, LessonState.HELPING, LessonState.FEEDBACK, LessonState.PREPARING -> handleDuringSpeech(command)
            LessonState.PAUSED -> handlePaused(command)
            LessonState.EVALUATING, LessonState.PLANNING_NEXT, LessonState.FINISHING -> if (command == RuntimeCommand.STOP) { stop(); true } else false
            LessonState.IDLE, LessonState.SESSION_END, LessonState.ERROR -> false
        }
    }

    private fun handleBinary(command: RuntimeCommand): Boolean = when (command) {
        RuntimeCommand.PRIMARY -> acceptBinaryAnswer(command, "A")
        RuntimeCommand.SECONDARY -> acceptBinaryAnswer(command, "B")
        RuntimeCommand.BACK -> requestHelp()
        RuntimeCommand.PAUSE -> { pauseManually(); true }
        RuntimeCommand.STOP -> { stop(); true }
        else -> false
    }

    private fun handleSelfAssessment(command: RuntimeCommand): Boolean = when (command) {
        RuntimeCommand.PRIMARY -> acceptSelfAnswer(command, ResponseConfidence.CORRECT)
        RuntimeCommand.SECONDARY -> acceptSelfAnswer(command, ResponseConfidence.UNSURE)
        RuntimeCommand.BACK -> acceptSelfAnswer(command, ResponseConfidence.WRONG)
        RuntimeCommand.PAUSE -> { pauseManually(); true }
        RuntimeCommand.STOP -> { stop(); true }
        else -> false
    }

    private fun handleDuringSpeech(command: RuntimeCommand): Boolean = when (command) {
        RuntimeCommand.PAUSE, RuntimeCommand.PRIMARY -> { pauseManually(); true }
        RuntimeCommand.STOP -> { stop(); true }
        // BACK during speech/feedback used to enter help with an invalid timeout state.
        RuntimeCommand.BACK, RuntimeCommand.SECONDARY, RuntimeCommand.PLAY -> false
    }

    private fun handlePaused(command: RuntimeCommand): Boolean = when (command) {
        RuntimeCommand.PRIMARY, RuntimeCommand.PLAY -> { resumeFromPause(); true }
        RuntimeCommand.STOP -> { stop(); true }
        else -> false
    }

    private fun acceptBinaryAnswer(command: RuntimeCommand, response: String): Boolean {
        if (!claimAnswer(command, response)) return true
        val exercise = activeExercise ?: run {
            turnAction.set(TurnActionState.CLOSED)
            return false
        }
        val turnId = currentTurn.turnId
        val generation = executionGeneration.get()
        val correct = response.equals(exercise.correctOption, ignoreCase = true)
        timeoutJob?.cancel()
        launchTurnWork(generation) {
            processAnswer(exercise, response, correct, null, true, InteractionKind.GRADED_RESPONSE, turnId, generation)
        }
        return true
    }

    private fun acceptSelfAnswer(command: RuntimeCommand, confidence: ResponseConfidence): Boolean {
        if (!claimAnswer(command, confidence.name)) return true
        val exercise = activeExercise ?: run {
            turnAction.set(TurnActionState.CLOSED)
            return false
        }
        val turnId = currentTurn.turnId
        val generation = executionGeneration.get()
        val correct = confidence == ResponseConfidence.CORRECT
        timeoutJob?.cancel()
        launchTurnWork(generation) {
            processAnswer(exercise, confidence.name, correct, confidence, true, InteractionKind.GRADED_RESPONSE, turnId, generation)
        }
        return true
    }

    private fun claimAnswer(command: RuntimeCommand, payload: String): Boolean {
        if (!turnAction.compareAndSet(TurnActionState.OPEN, TurnActionState.ANSWERING)) {
            recordDuplicateAsync(command, payload)
            return false
        }
        if (!answerAccepted.compareAndSet(false, true)) {
            turnAction.compareAndSet(TurnActionState.ANSWERING, TurnActionState.OPEN)
            recordDuplicateAsync(command, payload)
            return false
        }
        val snap = _snapshot.value
        scope.launch {
            recordEvent(
                eventType = RuntimeEventType.ANSWER_ACCEPTED,
                stateBefore = snap.state.name,
                stateAfter = snap.state.name,
                runtimeCommand = command.name,
                payload = payload,
                sessionIdOverride = snap.sessionId,
                turnIdOverride = snap.turnId
            )
        }
        return true
    }

    private fun recordDuplicateAsync(command: RuntimeCommand, payload: String) {
        val snap = _snapshot.value
        scope.launch {
            recordEvent(
                eventType = RuntimeEventType.ANSWER_REJECTED_DUPLICATE,
                stateBefore = snap.state.name,
                stateAfter = snap.state.name,
                runtimeCommand = command.name,
                payload = payload,
                sessionIdOverride = snap.sessionId,
                turnIdOverride = snap.turnId
            )
        }
    }

    private fun pauseManually() {
        pauseInternal(PauseReason.USER_REQUESTED, "Pausado")
    }

    private fun resumeFromPause() {
        val paused = _snapshot.value
        if (paused.state != LessonState.PAUSED || paused.sessionId == null || sessionClosing.get()) return

        val generation = executionGeneration.get()
        turnAction.set(TurnActionState.CLOSED)
        answerAccepted.set(true)
        _snapshot.value = paused.copy(
            state = LessonState.PREPARING,
            message = "Reanudando",
            pausedFrom = null,
            pauseReason = null,
            promptFinishedAtMs = null
        )

        scope.launch {
            recordEvent(
                eventType = RuntimeEventType.RESUMED,
                stateBefore = LessonState.PAUSED.name,
                stateAfter = LessonState.PREPARING.name,
                sessionIdOverride = paused.sessionId,
                turnIdOverride = currentTurn.turnId
            )
        }

        launchTurnWork(generation) {
            replayCurrentTurn(currentTurn.turnId, generation)
        }
    }

    private fun requestHelp(): Boolean {
        val snap = _snapshot.value
        if (snap.state != LessonState.WAITING_BINARY) return false
        val exercise = activeExercise ?: return false
        if (!turnAction.compareAndSet(TurnActionState.OPEN, TurnActionState.HELPING)) return true

        val expectedTurnId = currentTurn.turnId
        val generation = executionGeneration.get()
        timeoutJob?.cancel()
        answerAccepted.set(true)

        launchTurnWork(generation) {
            try {
                if (!isCurrent(generation, expectedTurnId)) return@launchTurnWork
                val depth = (currentTurn.helpDepth + 1).coerceAtMost(config.policy.maxHelpDepth)
                currentTurn = currentTurn.copy(helpDepth = depth)
                recordEvent(
                    eventType = RuntimeEventType.HELP_REQUESTED,
                    stateBefore = LessonState.WAITING_BINARY.name,
                    stateAfter = LessonState.HELPING.name,
                    payload = "Depth: $depth",
                    turnIdOverride = expectedTurnId
                )
                transitionTo(LessonState.HELPING, "Ayuda nivel $depth")
                speakHelp(exercise, depth)
                if (!isCurrent(generation, expectedTurnId)) return@launchTurnWork

                val finishedAt = clock.nowEpochMs()
                currentTurn = currentTurn.copy(
                    promptFinishedAtEpochMs = finishedAt,
                    expectedState = LessonState.WAITING_BINARY
                )
                answerAccepted.set(false)
                turnAction.set(TurnActionState.OPEN)
                transitionTo(
                    LessonState.WAITING_BINARY,
                    "Esperando A o B",
                    updateSnapshot = { it.copy(promptFinishedAtMs = finishedAt, helpDepth = depth) }
                )
                scheduleTimeout(LessonState.WAITING_BINARY, expectedTurnId, generation)
            } catch (ce: CancellationException) {
                throw ce
            }
        }
        return true
    }

    private suspend fun speakHelp(exercise: ExerciseDefinition, depth: Int) {
        when (depth) {
            0 -> Unit
            1 -> {
                speakText("Repetimos.", "es-PE")
                if (exercise.promptEs.isNotBlank()) speakText(exercise.promptEs, "es-PE")
                if (exercise.stimulusEn.isNotBlank()) speakText(exercise.stimulusEn, "en-US")
            }
            2 -> {
                speakText("Escucha más despacio.", "es-PE")
                if (exercise.stimulusEn.isNotBlank()) speakText(exercise.stimulusEn, "en-US", 0.72f)
                else speakText(exercise.promptEs, "es-PE", 0.82f)
            }
            3 -> {
                speakText("Descomposición por palabras.", "es-PE")
                val words = exercise.stimulusEn.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
                if (words.isEmpty()) speakText(exercise.promptEs, "es-PE", 0.82f)
                else words.forEach { speakText(it, "en-US", 0.78f) }
            }
            else -> {
                if (exercise.spellTarget.isNotBlank()) {
                    speakText("Deletreo de la palabra objetivo.", "es-PE")
                    speakText(EnglishAlphabet.spellForSpeech(exercise.spellTarget), "en-US", 0.80f)
                } else {
                    speakText("Repetimos la frase por palabras.", "es-PE")
                    exercise.stimulusEn.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
                        .forEach { speakText(it, "en-US", 0.75f) }
                }
            }
        }
        if (exercise.optionA.isNotBlank() && exercise.optionB.isNotBlank()) {
            speakText(
                "Opción A. ${exercise.optionA}. Opción B. ${exercise.optionB}.",
                optionLanguage(exercise),
                if (depth >= 2) 0.82f else 1.0f
            )
        }
    }

    private fun scheduleTimeout(waitingState: LessonState, expectedTurnId: Long, generation: Long) {
        require(waitingState == LessonState.WAITING_BINARY || waitingState == LessonState.WAITING_SELF_ASSESSMENT) {
            "Timeout can only be scheduled for a response-waiting state: $waitingState"
        }
        timeoutJob?.cancel()
        val timeoutMs = if (waitingState == LessonState.WAITING_BINARY) {
            config.policy.binaryResponseTimeoutMs
        } else {
            config.policy.selfAssessmentTimeoutMs
        }

        timeoutJob = scope.launch {
            delay(timeoutMs)
            if (
                isCurrent(generation, expectedTurnId) &&
                _snapshot.value.state == waitingState &&
                turnAction.compareAndSet(TurnActionState.OPEN, TurnActionState.HELPING)
            ) {
                timeoutJob = null
                onTimeoutDetected(waitingState, expectedTurnId, generation)
            }
        }
    }

    private fun onTimeoutDetected(waitingState: LessonState, expectedTurnId: Long, generation: Long) {
        val exercise = activeExercise ?: run {
            turnAction.set(TurnActionState.CLOSED)
            return
        }
        val retries = currentTurn.timeoutRetries + 1
        currentTurn = currentTurn.copy(timeoutRetries = retries)

        launchTurnWork(generation) {
            if (!isCurrent(generation, expectedTurnId)) return@launchTurnWork
            recordEvent(
                eventType = RuntimeEventType.TIMEOUT,
                stateBefore = waitingState.name,
                stateAfter = waitingState.name,
                payload = if (retries <= config.policy.maxTimeoutRetries) "Retry $retries" else "Max retries exceeded",
                turnIdOverride = expectedTurnId
            )

            if (retries <= config.policy.maxTimeoutRetries) {
                earcon.play(EarconKind.ATTENTION)
                delay(140L)
                speakText("¿Sigues ahí? Repetimos la actividad.", "es-PE")
                if (!isCurrent(generation, expectedTurnId)) return@launchTurnWork
                replayPromptOnly(exercise, expectedTurnId, generation)
            } else {
                turnAction.set(TurnActionState.ANSWERING)
                answerAccepted.set(true)
                processAnswer(
                    exercise = exercise,
                    response = "TIMEOUT",
                    correct = false,
                    confidence = null,
                    graded = false,
                    kind = InteractionKind.TIMEOUT,
                    expectedTurnId = expectedTurnId,
                    generation = generation
                )
            }
        }
    }

    private suspend fun processAnswer(
        exercise: ExerciseDefinition,
        response: String,
        correct: Boolean,
        confidence: ResponseConfidence?,
        graded: Boolean,
        kind: InteractionKind,
        expectedTurnId: Long,
        generation: Long
    ) {
        if (!isCurrent(generation, expectedTurnId)) return
        timeoutJob?.cancel()

        val snap = _snapshot.value
        val sessionId = snap.sessionId ?: return
        if (repository.hasInteraction(sessionId, expectedTurnId)) {
            syncCountersFromRepository(sessionId)
            if (isCurrent(generation, expectedTurnId)) nextExercise(generation)
            return
        }

        val now = clock.nowEpochMs()
        val latency = snap.promptFinishedAtMs?.let { (now - it).coerceAtLeast(0L) }
        val beforeSnapshot = repository.loadSnapshot()
        if (!isCurrent(generation, expectedTurnId)) return

        val primaryKcId = exercise.kcIds.firstOrNull()
        val beforeMastery = primaryKcId?.let { beforeSnapshot.stateByKcId[it]?.mastery }
        transitionTo(LessonState.EVALUATING, "Evaluando respuesta")

        val diagnosticWeights = diagnostic.diagnosticWeights(exercise, beforeSnapshot.stateByKcId)
        val priorInteractions = repository.recentInteractions(500)
        val firstSeenExercise = priorInteractions.none { it.exerciseId == exercise.id && it.graded }
        val transferEvidence = exercise.kcIds.size > 1 ||
            exercise.type in setOf(ExerciseType.PRON_DISCRIMINATION, ExerciseType.SPELL_FROM_AUDIO) ||
            exercise.tags.any { it.equals("transfer", true) || it.equals("contrast", true) || it.equals("context", true) }
        val updatedStates = exercise.kcIds.mapNotNull { kcId ->
            val component = beforeSnapshot.componentById[kcId] ?: return@mapNotNull null
            val prior = beforeSnapshot.stateByKcId[kcId] ?: LearnerKcState(kcId, mastery = component.priorMastery)
            val qWeight = diagnosticWeights[kcId] ?: (1.0 / exercise.kcIds.size.coerceAtLeast(1))

            if (!graded) {
                // A timeout is censored evidence: it is an exposure, not a successful/failed review.
                prior.copy(
                    uncertainty = (prior.uncertainty + 0.05).coerceAtMost(0.95),
                    exposureCount = prior.exposureCount + 1,
                    lastExposedAtEpochMs = now
                )
            } else {
                val objectiveWeight = if (correct) {
                    (0.55 + 0.45 * qWeight).coerceAtMost(1.0)
                } else {
                    (0.25 + 0.75 * qWeight).coerceAtMost(1.0)
                }
                val selfReportWeight = if (exercise.type == ExerciseType.SELF_ASSESS || exercise.type == ExerciseType.SPELL_FROM_AUDIO) {
                    val reliability = SelfAssessmentReliabilityEstimator.estimate(
                        kcId = kcId,
                        snapshot = beforeSnapshot,
                        interactions = priorInteractions,
                        policy = config.policy
                    )
                    when (confidence) {
                        ResponseConfidence.CORRECT -> reliability.positive
                        ResponseConfidence.UNSURE -> reliability.unsure
                        ResponseConfidence.WRONG -> reliability.negative
                        null -> reliability.positive
                    }
                } else 1.0
                val evidenceWeight = (objectiveWeight * selfReportWeight).coerceIn(0.10, 1.0)
                val elapsedHours = prior.lastReviewedAtEpochMs?.let { last ->
                    (now - last).coerceAtLeast(0L) / 3_600_000.0
                } ?: 0.0
                if (prior.lastReviewedAtEpochMs != null) {
                    onlineModels.observeMemory(kcId, elapsedHours, prior.halfLifeHours, correct)
                }
                val baseUpdate = stateUpdater.update(
                    component, prior, exercise, correct, confidence, latency, now, evidenceWeight
                )
                val particle = onlineModels.observeLatentState(
                    kcId = kcId,
                    priorMastery = prior.mastery,
                    priorHalfLifeHours = prior.halfLifeHours,
                    correct = correct,
                    elapsedHours = elapsedHours
                )
                baseUpdate.copy(
                    mastery = (0.78 * baseUpdate.mastery + 0.22 * particle.masteryMean).coerceIn(0.0, 1.0),
                    halfLifeHours = (0.88 * baseUpdate.halfLifeHours + 0.12 * particle.halfLifeMean).coerceIn(0.25, 24.0 * 365.0),
                    uncertainty = (0.75 * baseUpdate.uncertainty + 0.25 * (particle.masterySd * 2.5).coerceIn(0.04, 0.95)).coerceIn(0.04, 0.95),
                    exposureCount = prior.exposureCount + 1,
                    lastExposedAtEpochMs = now,
                    transferSuccesses = prior.transferSuccesses + if (correct && transferEvidence) 1 else 0,
                    novelSuccesses = prior.novelSuccesses + if (correct && firstSeenExercise) 1 else 0
                )
            }
        }

        val afterMastery = primaryKcId?.let { id -> updatedStates.firstOrNull { it.kcId == id }?.mastery }
        if (primaryKcId != null && graded) {
            val masteryGain = ((afterMastery ?: beforeMastery ?: 0.0) - (beforeMastery ?: 0.0)).coerceIn(-0.20, 0.20)
            val accuracyReward = if (correct) 0.70 else 0.0
            val speedReward = latency?.let { ms ->
                val target = (exercise.estimatedSeconds.coerceAtLeast(3) * 1000.0 * 0.30).coerceAtLeast(1200.0)
                (1.0 - (ms / (target * 2.0))).coerceIn(0.0, 1.0) * 0.15
            } ?: 0.05
            val learningReward = ((masteryGain + 0.20) / 0.40).coerceIn(0.0, 1.0) * 0.15
            onlineModels.observeStrategy(
                primaryKcId,
                exercise.type,
                (accuracyReward + speedReward + learningReward).coerceIn(0.0, 1.0)
            )
        }

        if (!isCurrent(generation, expectedTurnId)) return

        val misconceptionUpdates = buildMisconceptionUpdates(
            exercise = exercise,
            snapshot = beforeSnapshot,
            correct = correct,
            graded = graded,
            now = now
        )

        val interactionRecord = InteractionRecord(
            sessionId = sessionId,
            turnId = expectedTurnId,
            exerciseId = exercise.id,
            timestampEpochMs = now,
            response = response,
            correct = correct,
            graded = graded,
            kind = kind,
            confidence = confidence,
            latencyMs = latency,
            hintDepth = snap.helpDepth,
            plannerScore = activePlannerScore,
            stateBeforeMastery = beforeMastery,
            stateAfterMastery = afterMastery
        )

        repository.commitTurn(interactionRecord, updatedStates, misconceptionUpdates)
        if (!isCurrent(generation, expectedTurnId)) return

        recordEvent(
            eventType = RuntimeEventType.TURN_COMMITTED,
            stateBefore = LessonState.EVALUATING.name,
            stateAfter = LessonState.EVALUATING.name,
            exerciseId = exercise.id,
            payload = "Turn $expectedTurnId committed",
            turnIdOverride = expectedTurnId
        )

        val newTurns = snap.turnsCompleted + 1
        val newGraded = snap.gradedResponses + if (graded) 1 else 0
        val newCorrect = snap.correctResponses + if (graded && correct) 1 else 0
        transitionTo(
            LessonState.FEEDBACK,
            if (!graded) "Sin respuesta" else if (correct) "Correcto" else "Necesita ajuste",
            updateSnapshot = {
                it.copy(
                    turnsCompleted = newTurns,
                    gradedResponses = newGraded,
                    correctResponses = newCorrect,
                    completedItems = newTurns,
                    correctItems = newCorrect
                )
            }
        )

        val feedback = when {
            !graded -> "Tiempo agotado. Pasamos a la siguiente actividad."
            correct -> if (exercise.explanationEs.isNotBlank() && config.feedbackExplanations) {
                "Correcto. ${exercise.explanationEs}"
            } else "Correcto."
            else -> buildString {
                append("La respuesta correcta es ${exercise.correctOption.ifBlank { "la indicada" }}. ")
                if (exercise.explanationEs.isNotBlank()) append(exercise.explanationEs)
            }
        }
        recordEvent(
            eventType = RuntimeEventType.FEEDBACK,
            stateBefore = LessonState.EVALUATING.name,
            stateAfter = LessonState.FEEDBACK.name,
            exerciseId = exercise.id,
            payload = feedback.trim(),
            turnIdOverride = expectedTurnId
        )

        if (graded) {
            earcon.play(if (correct) EarconKind.CORRECT else EarconKind.INCORRECT)
        } else {
            earcon.play(EarconKind.ATTENTION)
        }
        delay(if (graded && correct) 160L else 200L)
        speakFeedback(exercise, feedback.trim())
        if (!isCurrent(generation, expectedTurnId)) return

        turnAction.set(TurnActionState.CLOSED)
        if (newTurns >= config.maxItems) finishNaturally("Objetivo de sesión completado")
        else nextExercise(generation)
    }

    private suspend fun speakFeedback(exercise: ExerciseDefinition, feedback: String) {
        if (exercise.spellTarget.isNotBlank() && exercise.type in setOf(ExerciseType.SPELLING_AB, ExerciseType.SPELL_FROM_AUDIO)) {
            when {
                feedback.startsWith("Correcto") -> speakText("Correcto.", "es-PE")
                feedback.startsWith("Tiempo agotado") -> speakText("Tiempo agotado.", "es-PE")
                else -> speakText("Revisemos la forma escrita.", "es-PE")
            }
            speakText("Se escribe", "es-PE")
            speakText(EnglishAlphabet.spellForSpeech(exercise.spellTarget), "en-US", 0.82f)
            speakText(exercise.spellTarget, "en-US", 0.90f)
            return
        }
        speakText(feedback, "es-PE")
    }

    private fun buildMisconceptionUpdates(
        exercise: ExerciseDefinition,
        snapshot: LearningSnapshot,
        correct: Boolean,
        graded: Boolean,
        now: Long
    ): List<Misconception> {
        if (!graded || exercise.misconceptionIds.isEmpty()) return emptyList()
        val primaryKcId = exercise.kcIds.firstOrNull() ?: return emptyList()
        val byId = snapshot.misconceptions.associateBy { it.id }
        return exercise.misconceptionIds.distinct().map { id ->
            val prior = byId[id]
            val p = prior?.probability ?: 0.10
            val next = if (correct) (p * 0.80) else (p + 0.15 * (1.0 - p))
            Misconception(
                id = id,
                kcId = prior?.kcId ?: primaryKcId,
                label = prior?.label ?: id,
                probability = next.coerceIn(0.0, 1.0),
                lastObservedAtEpochMs = now
            )
        }
    }

    private suspend fun nextExercise(generation: Long) {
        if (!isCurrent(generation) || sessionClosing.get()) return
        if (_snapshot.value.turnsCompleted >= config.maxItems) {
            finishNaturally("Objetivo de sesión completado")
            return
        }
        if (timeBudgetReached() && _snapshot.value.turnsCompleted >= MIN_ITEMS_BEFORE_TIME_STOP) {
            finishNaturally("Tiempo de sesión completado")
            return
        }

        timeoutJob?.cancel()
        turnAction.set(TurnActionState.CLOSED)
        answerAccepted.set(true)

        val nextTurnId = currentTurn.turnId + 1
        currentTurn = TurnContext(turnId = nextTurnId)
        activeExercise = null
        activePlannerScore = null
        transitionTo(
            LessonState.PLANNING_NEXT,
            "Planificando siguiente actividad",
            updateSnapshot = { it.copy(turnId = nextTurnId, promptFinishedAtMs = null) }
        )

        val now = clock.nowEpochMs()
        val learning = repository.loadSnapshot()
        if (!isCurrent(generation, nextTurnId)) return
        val recent = repository.recentInteractions(80)
        val candidate = planner.choose(config.mode, learning, recent, now, config.capabilities)
        if (!isCurrent(generation, nextTurnId)) return
        if (candidate == null) {
            finishNaturally("No quedan actividades elegibles")
            return
        }

        activeExercise = candidate.exercise
        activePlannerScore = candidate.utility
        currentTurn = currentTurn.copy(exerciseId = candidate.exercise.id)
        recordEvent(
            eventType = RuntimeEventType.EXERCISE_SELECTED,
            stateBefore = LessonState.PLANNING_NEXT.name,
            stateAfter = LessonState.PREPARING.name,
            exerciseId = candidate.exercise.id,
            payload = candidate.rationale,
            turnIdOverride = nextTurnId
        )
        recordEvent(
            eventType = RuntimeEventType.PREDICTION_RECORDED,
            stateBefore = LessonState.PLANNING_NEXT.name,
            stateAfter = LessonState.PREPARING.name,
            exerciseId = candidate.exercise.id,
            payload = "successProbability=${candidate.successProbability};utility=${candidate.utility};planner=${config.plannerVersion}",
            turnIdOverride = nextTurnId
        )
        transitionTo(
            LessonState.PREPARING,
            "Siguiente actividad",
            updateSnapshot = {
                it.copy(
                    currentExerciseId = candidate.exercise.id,
                    currentKcId = candidate.exercise.kcIds.firstOrNull(),
                    lastPlannerRationale = candidate.rationale,
                    helpDepth = 0
                )
            }
        )
        present(candidate.exercise, nextTurnId, generation)
    }

    private suspend fun present(exercise: ExerciseDefinition, expectedTurnId: Long, generation: Long) {
        when (exercise.type) {
            ExerciseType.TEACH -> presentTeaching(exercise, expectedTurnId, generation)
            ExerciseType.SELF_ASSESS, ExerciseType.SPELL_FROM_AUDIO -> presentSelfAssessment(exercise, expectedTurnId, generation)
            else -> presentBinary(exercise, expectedTurnId, generation)
        }
    }

    private suspend fun presentTeaching(exercise: ExerciseDefinition, expectedTurnId: Long, generation: Long) {
        if (!isCurrent(generation, expectedTurnId)) return
        recordEvent(
            RuntimeEventType.PROMPT_STARTED,
            _snapshot.value.state.name,
            LessonState.SPEAKING.name,
            exercise.id,
            turnIdOverride = expectedTurnId
        )
        transitionTo(LessonState.SPEAKING, "Explicación")
        speakTeachingPrompt(exercise)
        if (!isCurrent(generation, expectedTurnId)) return

        val sessionId = _snapshot.value.sessionId ?: return
        if (!repository.hasInteraction(sessionId, expectedTurnId)) {
            val learning = repository.loadSnapshot()
            if (!isCurrent(generation, expectedTurnId)) return
            val now = clock.nowEpochMs()
            val updatedStates = exercise.kcIds.mapNotNull { kcId ->
                val component = learning.componentById[kcId] ?: return@mapNotNull null
                val prior = learning.stateByKcId[kcId] ?: LearnerKcState(kcId, mastery = component.priorMastery)
                prior.copy(
                    exposureCount = prior.exposureCount + 1,
                    lastExposedAtEpochMs = now
                )
            }
            repository.commitTurn(
                interaction = InteractionRecord(
                    sessionId = sessionId,
                    turnId = expectedTurnId,
                    exerciseId = exercise.id,
                    timestampEpochMs = now,
                    response = "TEACH_PRESENTED",
                    correct = false,
                    graded = false,
                    kind = InteractionKind.TEACH_EXPOSURE,
                    plannerScore = activePlannerScore
                ),
                updatedStates = updatedStates,
                misconceptionUpdates = emptyList()
            )
            if (!isCurrent(generation, expectedTurnId)) return
            recordEvent(
                RuntimeEventType.TURN_COMMITTED,
                LessonState.SPEAKING.name,
                LessonState.SPEAKING.name,
                exercise.id,
                payload = "Turn $expectedTurnId committed (teach)",
                turnIdOverride = expectedTurnId
            )
        }

        val snap = _snapshot.value
        val newTurns = snap.turnsCompleted + 1
        _snapshot.value = snap.copy(
            turnsCompleted = newTurns,
            completedItems = newTurns
        )
        turnAction.set(TurnActionState.CLOSED)
        if (newTurns >= config.maxItems) finishNaturally("Objetivo de sesión completado")
        else nextExercise(generation)
    }

    private suspend fun speakTeachingPrompt(exercise: ExerciseDefinition) {
        earcon.play(EarconKind.NEW_PROMPT)
        delay(90L)
        if (exercise.promptEs.isNotBlank()) speakText(exercise.promptEs, "es-PE")
        if (exercise.stimulusEn.isNotBlank()) speakText(exercise.stimulusEn, "en-US")
        if (exercise.spellTarget.isNotBlank()) {
            speakText("Se escribe", "es-PE")
            speakText(EnglishAlphabet.spellForSpeech(exercise.spellTarget), "en-US")
        }
        if (exercise.explanationEs.isNotBlank()) speakText(exercise.explanationEs, "es-PE")
    }

    private suspend fun presentBinary(exercise: ExerciseDefinition, expectedTurnId: Long, generation: Long) {
        if (!isCurrent(generation, expectedTurnId)) return
        recordEvent(
            RuntimeEventType.PROMPT_STARTED,
            _snapshot.value.state.name,
            LessonState.SPEAKING.name,
            exercise.id,
            turnIdOverride = expectedTurnId
        )
        transitionTo(LessonState.SPEAKING, "Pregunta")
        speakBinaryPrompt(exercise)
        finishPromptWaiting(exercise, LessonState.WAITING_BINARY, expectedTurnId, generation)
    }

    private suspend fun speakBinaryPrompt(exercise: ExerciseDefinition) {
        earcon.play(EarconKind.NEW_PROMPT)
        delay(90L)
        if (exercise.promptEs.isNotBlank()) speakText(exercise.promptEs, "es-PE")
        if (exercise.stimulusEn.isNotBlank()) speakText(exercise.stimulusEn, "en-US")
        if (exercise.spellTarget.isNotBlank() && exercise.type == ExerciseType.SPELLING_AB) {
            speakText("Palabra objetivo.", "es-PE")
            speakText(exercise.spellTarget, "en-US")
            speakText("Opción A.", "es-PE")
            speakText(EnglishAlphabet.spellForSpeech(exercise.optionA), "en-US", 0.82f)
            speakText("Opción B.", "es-PE")
            speakText(EnglishAlphabet.spellForSpeech(exercise.optionB), "en-US", 0.82f)
        } else {
            speakText("Opción A. ${exercise.optionA}. Opción B. ${exercise.optionB}.", optionLanguage(exercise))
        }
        if (config.announceControls) speakText("Play pausa para A. Siguiente para B. Anterior para repetir.", "es-PE")
    }

    private suspend fun presentSelfAssessment(exercise: ExerciseDefinition, expectedTurnId: Long, generation: Long) {
        if (!isCurrent(generation, expectedTurnId)) return
        recordEvent(
            RuntimeEventType.PROMPT_STARTED,
            _snapshot.value.state.name,
            LessonState.SPEAKING.name,
            exercise.id,
            turnIdOverride = expectedTurnId
        )
        transitionTo(LessonState.SPEAKING, "Recuperación mental")
        speakSelfAssessmentPrompt(exercise)
        finishPromptWaiting(exercise, LessonState.WAITING_SELF_ASSESSMENT, expectedTurnId, generation)
    }

    private suspend fun speakSelfAssessmentPrompt(exercise: ExerciseDefinition) {
        earcon.play(EarconKind.NEW_PROMPT)
        delay(90L)
        if (exercise.promptEs.isNotBlank()) speakText(exercise.promptEs, "es-PE")
        if (exercise.stimulusEn.isNotBlank()) speakText(exercise.stimulusEn, "en-US")
        if (exercise.spellTarget.isNotBlank()) {
            speakText("Forma mentalmente las letras de la palabra que acabas de escuchar.", "es-PE")
        }
        delay(2200L)
        speakText("¿Lo resolviste? Play pausa para sí. Siguiente para dudé. Anterior para no.", "es-PE")
    }

    private suspend fun finishPromptWaiting(
        exercise: ExerciseDefinition,
        waitingState: LessonState,
        expectedTurnId: Long,
        generation: Long
    ) {
        if (!isCurrent(generation, expectedTurnId)) return
        val finishedAt = clock.nowEpochMs()
        currentTurn = currentTurn.copy(
            promptFinishedAtEpochMs = finishedAt,
            expectedState = waitingState
        )
        answerAccepted.set(false)
        turnAction.set(TurnActionState.OPEN)
        recordEvent(
            RuntimeEventType.PROMPT_FINISHED,
            LessonState.SPEAKING.name,
            waitingState.name,
            exercise.id,
            turnIdOverride = expectedTurnId
        )
        transitionTo(
            waitingState,
            if (waitingState == LessonState.WAITING_BINARY) "Esperando A o B" else "Autoevaluación",
            updateSnapshot = { it.copy(promptFinishedAtMs = finishedAt) }
        )
        scheduleTimeout(waitingState, expectedTurnId, generation)
    }

    private suspend fun replayPromptOnly(exercise: ExerciseDefinition, expectedTurnId: Long, generation: Long) {
        if (!isCurrent(generation, expectedTurnId)) return
        when (exercise.type) {
            ExerciseType.TEACH -> {
                // Teaching has no response timeout. A pause/restart replays the explanation safely.
                presentTeaching(exercise, expectedTurnId, generation)
            }
            ExerciseType.SELF_ASSESS, ExerciseType.SPELL_FROM_AUDIO -> {
                transitionTo(LessonState.SPEAKING, "Recuperación mental")
                speakSelfAssessmentPrompt(exercise)
                finishPromptWaiting(exercise, LessonState.WAITING_SELF_ASSESSMENT, expectedTurnId, generation)
            }
            else -> {
                transitionTo(LessonState.SPEAKING, "Pregunta")
                speakBinaryPrompt(exercise)
                finishPromptWaiting(exercise, LessonState.WAITING_BINARY, expectedTurnId, generation)
            }
        }
    }

    private suspend fun replayCurrentTurn(expectedTurnId: Long, generation: Long) {
        if (!isCurrent(generation, expectedTurnId)) return
        val sessionId = _snapshot.value.sessionId ?: return
        if (repository.hasInteraction(sessionId, expectedTurnId)) {
            syncCountersFromRepository(sessionId)
            if (isCurrent(generation, expectedTurnId)) nextExercise(generation)
            return
        }
        val exercise = activeExercise
        if (exercise == null) {
            nextExercise(generation)
            return
        }
        transitionTo(LessonState.PREPARING, "Reanudando actividad")
        replayPromptOnly(exercise, expectedTurnId, generation)
    }

    private suspend fun startOrRestoreSession(generation: Long) {
        val now = clock.nowEpochMs()
        val active = repository.activeSession()
        val resumable = active?.takeIf {
            it.endedAtEpochMs == null &&
                it.mode == config.mode &&
                (now - it.startedAtEpochMs).coerceAtLeast(0L) <= MAX_RESUME_AGE_MS
        }

        if (active != null && resumable == null) {
            repository.finishSession(active.id, now)
        }
        if (!isCurrent(generation)) return

        if (resumable != null) {
            restoreSession(resumable, generation)
            return
        }

        val id = repository.createSession(config.mode, now)
        if (!isCurrent(generation)) {
            repository.finishSession(id, clock.nowEpochMs())
            return
        }

        currentTurn = TurnContext(turnId = 0L)
        activeExercise = null
        activePlannerScore = null
        _snapshot.value = RuntimeSnapshot(
            state = LessonState.PREPARING,
            mode = config.mode,
            sessionId = id,
            turnId = 0L,
            message = "Preparando sesión",
            sessionStartedAtEpochMs = now,
            targetDurationMinutes = config.targetDurationMinutes
        )
        recordEvent(
            RuntimeEventType.SESSION_STARTED,
            LessonState.IDLE.name,
            LessonState.PREPARING.name,
            payload = "Mode=${config.mode.name};maxItems=${config.maxItems};minutes=${config.targetDurationMinutes};goal=${config.learningGoal};variety=${config.englishVariety};intensity=${config.intensity};planner=${config.plannerVersion};policy=${config.policyVersion};content=${config.contentVersion};deviceProfileId=${config.deviceProfileId};modelSeed=${config.modelSeed}",
            sessionIdOverride = id,
            turnIdOverride = 0L
        )

        launchTurnWork(generation) {
            speakText(introText(config.mode), "es-PE")
            if (isCurrent(generation)) nextExercise(generation)
        }
    }

    private suspend fun restoreSession(active: SessionRecord, generation: Long) {
        val interactions = repository.recentInteractions(1000).filter { it.sessionId == active.id }
        val events = repository.recentRuntimeEvents(1000).filter { it.sessionId == active.id }
        val turns = interactions.size
        val graded = interactions.count { it.graded }
        val correct = interactions.count { it.graded && it.correct }
        val committedTurn = interactions.maxOfOrNull { it.turnId } ?: 0L
        val pendingEvent = events
            .filter {
                it.turnId > committedTurn &&
                    it.eventType == RuntimeEventType.EXERCISE_SELECTED &&
                    it.exerciseId != null
            }
            .maxByOrNull { it.timestampEpochMs }

        currentTurn = TurnContext(
            turnId = pendingEvent?.turnId ?: committedTurn,
            exerciseId = pendingEvent?.exerciseId.orEmpty()
        )
        val learning = repository.loadSnapshot()
        activeExercise = pendingEvent?.exerciseId?.let { learning.exerciseById[it] }
        activePlannerScore = null

        _snapshot.value = RuntimeSnapshot(
            state = LessonState.PREPARING,
            mode = active.mode,
            sessionId = active.id,
            turnId = currentTurn.turnId,
            currentExerciseId = activeExercise?.id,
            currentKcId = activeExercise?.kcIds?.firstOrNull(),
            message = "Restaurando sesión",
            completedItems = turns,
            correctItems = correct,
            turnsCompleted = turns,
            gradedResponses = graded,
            correctResponses = correct,
            sessionStartedAtEpochMs = active.startedAtEpochMs,
            targetDurationMinutes = config.targetDurationMinutes
        )
        recordEvent(
            RuntimeEventType.SESSION_STARTED,
            LessonState.IDLE.name,
            LessonState.PREPARING.name,
            payload = "Restored active session;planner=${config.plannerVersion};policy=${config.policyVersion};content=${config.contentVersion};deviceProfileId=${config.deviceProfileId};modelSeed=${config.modelSeed}",
            sessionIdOverride = active.id,
            turnIdOverride = currentTurn.turnId
        )

        launchTurnWork(generation) {
            if (activeExercise != null && !repository.hasInteraction(active.id, currentTurn.turnId)) {
                replayCurrentTurn(currentTurn.turnId, generation)
            } else {
                nextExercise(generation)
            }
        }
    }

    private suspend fun syncCountersFromRepository(sessionId: Long) {
        val interactions = repository.recentInteractions(1000).filter { it.sessionId == sessionId }
        val turns = interactions.size
        val graded = interactions.count { it.graded }
        val correct = interactions.count { it.graded && it.correct }
        _snapshot.value = _snapshot.value.copy(
            completedItems = turns,
            correctItems = correct,
            turnsCompleted = turns,
            gradedResponses = graded,
            correctResponses = correct
        )
    }

    private fun launchTurnWork(generation: Long, block: suspend CoroutineScope.() -> Unit) {
        turnJob?.cancel()
        turnJob = scope.launch {
            try {
                if (!isCurrent(generation) || sessionClosing.get()) return@launch
                block()
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                if (isCurrent(generation) && !sessionClosing.get()) fail(t)
            }
        }
    }

    private fun isCurrent(generation: Long, expectedTurnId: Long? = null): Boolean {
        if (generation != executionGeneration.get()) return false
        return expectedTurnId == null || expectedTurnId == currentTurn.turnId
    }

    private suspend fun transitionTo(
        newState: LessonState,
        message: String,
        updateSnapshot: (RuntimeSnapshot) -> RuntimeSnapshot = { it }
    ) {
        val old = _snapshot.value
        val updated = updateSnapshot(old.copy(state = newState, message = message))
        _snapshot.value = updated
        recordEvent(
            RuntimeEventType.STATE_TRANSITION,
            old.state.name,
            newState.name,
            exerciseId = activeExercise?.id,
            sessionIdOverride = updated.sessionId,
            turnIdOverride = updated.turnId
        )
    }

    private suspend fun recordEvent(
        eventType: RuntimeEventType,
        stateBefore: String,
        stateAfter: String,
        exerciseId: String? = activeExercise?.id,
        runtimeCommand: String? = null,
        mediaKeyCode: Int? = null,
        payload: String? = null,
        sessionIdOverride: Long? = null,
        turnIdOverride: Long? = null
    ) {
        val sessionId = sessionIdOverride ?: _snapshot.value.sessionId ?: return
        repository.recordRuntimeEvent(
            RuntimeEvent(
                sessionId = sessionId,
                turnId = turnIdOverride ?: currentTurn.turnId,
                timestampEpochMs = clock.nowEpochMs(),
                eventType = eventType,
                stateBefore = stateBefore,
                stateAfter = stateAfter,
                exerciseId = exerciseId,
                runtimeCommand = runtimeCommand,
                mediaKeyCode = mediaKeyCode,
                payload = payload
            )
        )
    }

    private suspend fun finishNaturally(message: String) {
        if (!sessionClosing.compareAndSet(false, true)) return
        val snap = _snapshot.value
        executionGeneration.incrementAndGet()
        turnAction.set(TurnActionState.CLOSED)
        answerAccepted.set(true)
        timeoutJob?.cancel()
        _snapshot.value = snap.copy(
            state = LessonState.FINISHING,
            message = message,
            promptFinishedAtMs = null,
            pausedFrom = null,
            pauseReason = null
        )
        closeSessionInternal(snap.sessionId, currentTurn.turnId, snap.state, message, announce = true)
    }

    private suspend fun closeSessionInternal(
        sessionId: Long?,
        turnId: Long,
        stateBefore: LessonState,
        message: String,
        announce: Boolean
    ) {
        try {
            var finalMessage = message
            if (sessionId != null) {
                repository.finishSession(sessionId, clock.nowEpochMs())
                if (config.mode == SessionMode.PLACEMENT) {
                    val placement = repository.applyPlacementCalibration(sessionId)
                    if (placement != null) {
                        finalMessage = "$message. Nivel inicial estimado: ${placement.overallCefr}, con fiabilidad ${(placement.reliability * 100).toInt()} por ciento."
                    }
                } else {
                    val interactions = repository.sessionInteractions(sessionId)
                    val graded = interactions.filter { it.graded }
                    val correct = graded.count { it.correct }
                    val accuracy = if (graded.isEmpty()) 0 else (100.0 * correct / graded.size).toInt()
                    finalMessage = if (graded.isEmpty()) {
                        "$message. Completaste ${interactions.size} actividades."
                    } else {
                        "$message. Completaste ${interactions.size} actividades con $accuracy por ciento de precisión objetiva."
                    }
                }
                recordEvent(
                    RuntimeEventType.SESSION_STOPPED,
                    stateBefore.name,
                    LessonState.SESSION_END.name,
                    payload = finalMessage,
                    sessionIdOverride = sessionId,
                    turnIdOverride = turnId
                )
            }
            if (announce) {
                earcon.play(EarconKind.REGISTERED)
                delay(110L)
                speakText(finalMessage, "es-PE")
            }
            _snapshot.value = _snapshot.value.copy(lastSessionSummary = finalMessage)
        } catch (ce: CancellationException) {
            // Route loss/STOP may intentionally cancel the final utterance.
        } finally {
            _snapshot.value = _snapshot.value.copy(
                state = LessonState.SESSION_END,
                message = message,
                promptFinishedAtMs = null,
                pausedFrom = null,
                pauseReason = null
            )
        }
    }

    private fun fail(t: Throwable) {
        if (t is CancellationException) return
        executionGeneration.incrementAndGet()
        sessionClosing.set(true)
        turnAction.set(TurnActionState.CLOSED)
        answerAccepted.set(true)
        timeoutJob?.cancel()
        speech.stop()
        val snap = _snapshot.value
        _snapshot.value = snap.copy(
            state = LessonState.ERROR,
            message = "Error",
            error = t.message ?: t::class.java.simpleName,
            promptFinishedAtMs = null
        )
        scope.launch {
            recordEvent(
                RuntimeEventType.ERROR,
                snap.state.name,
                LessonState.ERROR.name,
                payload = t.message ?: t::class.java.simpleName,
                sessionIdOverride = snap.sessionId,
                turnIdOverride = snap.turnId
            )
            snap.sessionId?.let { repository.finishSession(it, clock.nowEpochMs()) }
        }
        earcon.play(EarconKind.WARNING)
    }

    private fun optionLanguage(exercise: ExerciseDefinition): String = when (exercise.type) {
        ExerciseType.MEANING_AB, ExerciseType.PRON_DISCRIMINATION -> "es-PE"
        else -> "en-US"
    }

    private fun introText(mode: SessionMode): String = when (mode) {
        SessionMode.PLACEMENT -> "Diagnóstico adaptativo iniciado. Responde sin adivinar; saltaremos entre niveles para encontrar tu punto de partida."
        SessionMode.ADAPTIVE -> "Sesión adaptativa iniciada. Guarda el teléfono."
        SessionMode.VOCABULARY -> "Modo vocabulario iniciado. Trabajaremos significado, sonido, escritura y uso."
        SessionMode.GRAMMAR -> "Modo gramática iniciado."
        SessionMode.LISTENING -> "Modo comprensión auditiva iniciado."
        SessionMode.SPELLING -> "Modo ortografía y deletreo iniciado."
        SessionMode.PRONUNCIATION -> "Modo discriminación de pronunciación iniciado."
        SessionMode.SPEAKING -> "La práctica de producción oral se realiza con el modo de pantalla y micrófono."
        SessionMode.READING -> "La práctica de lectura se realiza con el modo de pantalla."
        SessionMode.WRITING -> "La práctica de escritura se realiza con el modo de pantalla."
    }

    private fun timeBudgetReached(): Boolean {
        val started = _snapshot.value.sessionStartedAtEpochMs ?: return false
        val elapsed = (clock.nowEpochMs() - started).coerceAtLeast(0L)
        return elapsed >= config.targetDurationMinutes.coerceAtLeast(1) * 60_000L
    }

    companion object {
        private const val MAX_RESUME_AGE_MS = 12L * 60L * 60L * 1000L
        private const val MIN_ITEMS_BEFORE_TIME_STOP = 3
    }
}
