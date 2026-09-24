package com.siaa.core.runtime

import com.siaa.core.algorithm.BktEngine
import com.siaa.core.model.AttemptHistoryItem
import com.siaa.core.model.ExerciseItem
import com.siaa.core.model.ExerciseOption
import com.siaa.core.model.UserProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class HelpLadderStep {
    INITIAL,
    HINT,
    SLOW_MODEL,
    FULL_EXPLANATION
}

data class SessionUiState(
    val currentExercise: ExerciseItem? = null,
    val exerciseIndex: Int = 0,
    val totalExercises: Int = 0,
    val isPresentingPrompt: Boolean = false,
    val selectedOptionId: String? = null,
    val isAnswerChecked: Boolean = false,
    val isAnswerCorrect: Boolean = false,
    val helpStep: HelpLadderStep = HelpLadderStep.INITIAL,
    val currentMastery: Double = 0.35,
    val sessionScore: Int = 0,
    val streak: Int = 3
)

class SessionStateMachine(
    private val bktEngine: BktEngine = BktEngine()
) {
    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private var exerciseList: List<ExerciseItem> = emptyList()
    private var currentIndex = 0
    private var userProgress = UserProgress()

    fun startSession(exercises: List<ExerciseItem>, initialProgress: UserProgress) {
        this.exerciseList = exercises
        this.currentIndex = 0
        this.userProgress = initialProgress

        if (exercises.isNotEmpty()) {
            loadExercise(0)
        }
    }

    private fun loadExercise(index: Int) {
        if (index in exerciseList.indices) {
            val ex = exerciseList[index]
            val priorMastery = userProgress.kcMastery[ex.id] ?: 0.30

            _uiState.value = _uiState.value.copy(
                currentExercise = ex,
                exerciseIndex = index + 1,
                totalExercises = exerciseList.size,
                isPresentingPrompt = true,
                selectedOptionId = null,
                isAnswerChecked = false,
                isAnswerCorrect = false,
                helpStep = HelpLadderStep.INITIAL,
                currentMastery = priorMastery
            )
        }
    }

    fun selectOption(optionId: String): Boolean {
        val currentEx = _uiState.value.currentExercise ?: return false
        val option = currentEx.options.find { it.id == optionId } ?: return false

        val isCorrect = option.isCorrect
        val updatedMastery = bktEngine.updateMastery(
            currentMastery = _uiState.value.currentMastery,
            correct = isCorrect
        )

        val updatedKcMap = userProgress.kcMastery.toMutableMap().apply {
            put(currentEx.id, updatedMastery)
        }

        val historyItem = AttemptHistoryItem(
            exerciseId = currentEx.id,
            title = currentEx.title,
            level = currentEx.level,
            kind = currentEx.kind,
            wasCorrect = isCorrect,
            helpStepsUsed = _uiState.value.helpStep.ordinal
        )

        userProgress = userProgress.copy(
            totalAttempts = userProgress.totalAttempts + 1,
            correctAttempts = userProgress.correctAttempts + (if (isCorrect) 1 else 0),
            kcMastery = updatedKcMap,
            recentHistory = listOf(historyItem) + userProgress.recentHistory
        )

        _uiState.value = _uiState.value.copy(
            selectedOptionId = optionId,
            isAnswerChecked = true,
            isAnswerCorrect = isCorrect,
            currentMastery = updatedMastery,
            sessionScore = _uiState.value.sessionScore + (if (isCorrect) 10 else 0)
        )

        return isCorrect
    }

    fun triggerHelpLadder(): HelpLadderStep {
        val nextStep = when (_uiState.value.helpStep) {
            HelpLadderStep.INITIAL -> HelpLadderStep.HINT
            HelpLadderStep.HINT -> HelpLadderStep.SLOW_MODEL
            HelpLadderStep.SLOW_MODEL -> HelpLadderStep.FULL_EXPLANATION
            HelpLadderStep.FULL_EXPLANATION -> HelpLadderStep.FULL_EXPLANATION
        }
        _uiState.value = _uiState.value.copy(helpStep = nextStep)
        return nextStep
    }

    fun nextExercise(): Boolean {
        if (currentIndex + 1 < exerciseList.size) {
            currentIndex++
            loadExercise(currentIndex)
            return true
        }
        return false
    }

    fun previousExercise(): Boolean {
        if (currentIndex > 0) {
            currentIndex--
            loadExercise(currentIndex)
            return true
        }
        return false
    }

    fun handleMediaControlEvent(event: MediaControlEvent): Boolean {
        val state = _uiState.value
        val ex = state.currentExercise ?: return false

        return when (event) {
            MediaControlEvent.PLAY_PAUSE -> {
                // Replay prompt
                true
            }
            MediaControlEvent.NEXT -> {
                if (state.isAnswerChecked) {
                    nextExercise()
                } else {
                    // Option 1 ("Next" earbud key)
                    val opt1 = ex.options.getOrNull(0)
                    if (opt1 != null) {
                        selectOption(opt1.id)
                    }
                }
                true
            }
            MediaControlEvent.PREVIOUS -> {
                if (state.isAnswerChecked) {
                    previousExercise()
                } else {
                    // Option 2 ("Prev" earbud key) or trigger hint
                    val opt2 = ex.options.getOrNull(1)
                    if (opt2 != null) {
                        selectOption(opt2.id)
                    } else {
                        triggerHelpLadder()
                    }
                }
                true
            }
            MediaControlEvent.STOP -> {
                false
            }
        }
    }

    fun getUserProgress(): UserProgress = userProgress
}
