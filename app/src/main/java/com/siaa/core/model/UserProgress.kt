package com.siaa.core.model

data class AttemptHistoryItem(
    val exerciseId: String,
    val title: String,
    val level: String,
    val kind: ExerciseKind,
    val wasCorrect: Boolean,
    val helpStepsUsed: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class UserProgress(
    val totalAttempts: Int = 0,
    val correctAttempts: Int = 0,
    val currentStreakDays: Int = 3,
    val minutesStudiedToday: Int = 18,
    val dailyGoalMinutes: Int = 20,
    val selectedLevel: String = "A1",
    val selectedKind: ExerciseKind? = null,
    val kcMastery: Map<String, Double> = emptyMap(),
    val recentHistory: List<AttemptHistoryItem> = emptyList()
) {
    val overallAccuracy: Float
        get() = if (totalAttempts > 0) (correctAttempts.toFloat() / totalAttempts) * 100f else 0f

    val averageMastery: Double
        get() = if (kcMastery.isNotEmpty()) kcMastery.values.average() else 0.42
}
