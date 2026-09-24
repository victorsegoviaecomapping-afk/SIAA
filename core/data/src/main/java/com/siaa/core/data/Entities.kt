package com.siaa.core.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "knowledge_components")
data class KnowledgeComponentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val cefr: String,
    val domain: String,
    val form: String,
    val meaning: String,
    val useText: String,
    val importance: Double,
    val priorMastery: Double,
    val tagsCsv: String
)

@Entity(tableName = "knowledge_edges", primaryKeys = ["fromId", "toId"])
data class KnowledgeEdgeEntity(
    val fromId: String,
    val toId: String,
    val weight: Double,
    val hardPrerequisite: Boolean
)

@Entity(
    tableName = "interactions",
    indices = [
        androidx.room.Index(value = ["sessionId", "turnId"], unique = true)
    ]
)
data class InteractionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    @ColumnInfo(defaultValue = "0") val turnId: Long = 0L,
    val exerciseId: String,
    val timestampEpochMs: Long,
    val response: String,
    val correct: Boolean,
    @ColumnInfo(defaultValue = "1") val graded: Boolean = true,
    @ColumnInfo(defaultValue = "'GRADED_RESPONSE'") val kind: String = "GRADED_RESPONSE",
    val confidence: String?,
    val latencyMs: Long?,
    val hintDepth: Int,
    val plannerScore: Double?,
    val stateBeforeMastery: Double?,
    val stateAfterMastery: Double?
)

@Entity(tableName = "runtime_events")
data class RuntimeEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val turnId: Long,
    val timestampEpochMs: Long,
    val eventType: String,
    val stateBefore: String,
    val stateAfter: String,
    val exerciseId: String?,
    val runtimeCommand: String?,
    val mediaKeyCode: Int?,
    val payload: String?
)

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val type: String,
    val kcIdsCsv: String,
    val cefr: String,
    val difficulty: Double,
    val promptEs: String,
    val stimulusEn: String,
    val optionA: String,
    val optionB: String,
    val correctOption: String,
    val explanationEs: String,
    val spellTarget: String,
    val estimatedSeconds: Int,
    val tagsCsv: String,
    @ColumnInfo(defaultValue = "''") val misconceptionIdsCsv: String = ""
)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: String,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long? = null
)

@Entity(tableName = "learner_kc_state")

data class LearnerKcStateEntity(
    @PrimaryKey val kcId: String,
    val mastery: Double,
    val recognition: Double,
    val production: Double,
    val orthography: Double,
    val automaticity: Double,
    val halfLifeHours: Double,
    val uncertainty: Double,
    val lastReviewedAtEpochMs: Long?,
    val consecutiveSuccess: Int,
    val consecutiveFailure: Int,
    val totalAttempts: Int,
    val totalCorrect: Int,
    @ColumnInfo(defaultValue = "0") val exposureCount: Int = 0,
    val lastExposedAtEpochMs: Long? = null,
    @ColumnInfo(defaultValue = "0") val transferSuccesses: Int = 0,
    @ColumnInfo(defaultValue = "0") val novelSuccesses: Int = 0
)

@Entity(tableName = "device_profiles")
data class DeviceProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val primaryKeyCode: Int? = null,
    val secondaryKeyCode: Int? = null,
    val backKeyCode: Int? = null,
    val stopKeyCode: Int? = null,
    val playPauseAvailable: Boolean,
    val nextAvailable: Boolean,
    val previousAvailable: Boolean,
    val lastSeenAtEpochMs: Long
)


@Entity(tableName = "misconceptions")
data class MisconceptionEntity(
    @PrimaryKey val id: String,
    val kcId: String,
    val label: String,
    val probability: Double,
    val lastObservedAtEpochMs: Long
)

@Entity(tableName = "skill_evidence")
data class SkillEvidenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sessionId: Long? = null,
    val activityId: String,
    val kcIdsCsv: String,
    val dimension: String,
    val score: Double,
    val timestampEpochMs: Long,
    val latencyMs: Long? = null,
    val rawResponse: String = "",
    val source: String = "local"
)

@Entity(tableName = "app_meta")
data class AppMetaEntity(
    @PrimaryKey val key: String,
    val value: String
)
