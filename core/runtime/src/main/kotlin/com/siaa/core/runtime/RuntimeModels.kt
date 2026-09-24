package com.siaa.core.runtime

import com.siaa.core.model.*

enum class LessonState {
    IDLE,
    PREPARING,
    SPEAKING,
    WAITING_BINARY,
    WAITING_SELF_ASSESSMENT,
    EVALUATING,
    FEEDBACK,
    HELPING,
    PLANNING_NEXT,
    FINISHING,
    PAUSED,
    SESSION_END,
    ERROR
}

enum class RuntimeCommand {
    PRIMARY,
    SECONDARY,
    BACK,
    PAUSE,
    PLAY,
    STOP
}

enum class PauseReason {
    USER_REQUESTED,
    AUDIO_ROUTE_LOST,
    AUDIO_FOCUS_LOST
}

enum class TurnActionState {
    OPEN,
    HELPING,
    ANSWERING,
    PAUSED,
    CLOSED
}

data class TurnContext(
    val turnId: Long = 0L,
    val exerciseId: String = "",
    val expectedState: LessonState = LessonState.IDLE,
    val helpDepth: Int = 0,
    val promptVariant: String = "",
    val promptFinishedAtEpochMs: Long = 0L,
    val timeoutRetries: Int = 0
)

data class SessionConfig(
    val mode: SessionMode = SessionMode.ADAPTIVE,
    val maxItems: Int = 50,
    val targetDurationMinutes: Int = 25,
    val learningGoal: LearningGoal = LearningGoal.GENERAL,
    val englishVariety: EnglishVariety = EnglishVariety.MIXED,
    val intensity: SessionIntensity = SessionIntensity.BALANCED,
    val announceControls: Boolean = true,
    val feedbackExplanations: Boolean = true,
    val capabilities: SessionCapabilities = SessionCapabilities(),
    val policy: SessionPolicy = SessionPolicy(),
    val speechRate: Float = 1.0f,
    val plannerVersion: String = "pomdp-lookahead-v3",
    val policyVersion: String = "2026-09-v24",
    val contentVersion: String = "2.4.0|schema=1.1|generator=4",
    val deviceProfileId: Long? = null,
    val modelSeed: Long = 0x51AA2026L
)

data class RuntimeSnapshot(
    val state: LessonState = LessonState.IDLE,
    val mode: SessionMode = SessionMode.ADAPTIVE,
    val sessionId: Long? = null,
    val turnId: Long = 0L,
    val currentExerciseId: String? = null,
    val currentKcId: String? = null,
    val message: String = "Listo",
    val completedItems: Int = 0,
    val correctItems: Int = 0,
    val turnsCompleted: Int = 0,
    val gradedResponses: Int = 0,
    val correctResponses: Int = 0,
    val promptFinishedAtMs: Long? = null,
    val helpDepth: Int = 0,
    val lastPlannerRationale: String = "",
    val pausedFrom: LessonState? = null,
    val pauseReason: PauseReason? = null,
    val sessionStartedAtEpochMs: Long? = null,
    val targetDurationMinutes: Int = 25,
    val lastSessionSummary: String = "",
    val error: String? = null
)

