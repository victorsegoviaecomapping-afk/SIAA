package com.siaa.core.data

import androidx.room.*

@Dao
interface ContentDao {
    @Query("SELECT * FROM knowledge_components") suspend fun allKcs(): List<KnowledgeComponentEntity>
    @Query("SELECT * FROM knowledge_edges") suspend fun allEdges(): List<KnowledgeEdgeEntity>
    @Query("SELECT * FROM exercises") suspend fun allExercises(): List<ExerciseEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertKcs(items: List<KnowledgeComponentEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertEdges(items: List<KnowledgeEdgeEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertExercises(items: List<ExerciseEntity>)
    @Query("SELECT COUNT(*) FROM knowledge_components") suspend fun kcCount(): Int
    @Query("DELETE FROM knowledge_edges") suspend fun clearEdges()
    @Query("DELETE FROM exercises") suspend fun clearExercises()
    @Query("DELETE FROM knowledge_components") suspend fun clearKcs()
}

@Dao
interface LearnerDao {
    @Query("SELECT * FROM learner_kc_state") suspend fun allStates(): List<LearnerKcStateEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertState(state: LearnerKcStateEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertStates(states: List<LearnerKcStateEntity>)
    @Query("DELETE FROM learner_kc_state WHERE kcId = :kcId") suspend fun deleteState(kcId: String)
    @Query("DELETE FROM learner_kc_state WHERE kcId NOT IN (:validKcIds)") suspend fun deleteOrphanStates(validKcIds: List<String>)
    @Query("SELECT * FROM misconceptions") suspend fun allMisconceptions(): List<MisconceptionEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertMisconception(item: MisconceptionEntity)
    @Query("DELETE FROM misconceptions") suspend fun clearMisconceptions()
}


data class SessionSummaryRow(
    val id: Long,
    val mode: String,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long?,
    val completedItems: Int,
    val correctItems: Int,
    val gradedResponses: Int,
    val meanLatencyMs: Double?
)

@Dao
interface SessionDao {
    @Insert suspend fun insertSession(session: SessionEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreSessions(items: List<SessionEntity>)
    @Query("UPDATE sessions SET endedAtEpochMs=:endedAt WHERE id=:id") suspend fun finishSession(id: Long, endedAt: Long)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertInteraction(interaction: InteractionEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreInteractions(items: List<InteractionEntity>)
    @Query("SELECT * FROM interactions ORDER BY timestampEpochMs DESC LIMIT :limit") suspend fun recentInteractions(limit: Int): List<InteractionEntity>
    @Query("SELECT * FROM interactions WHERE sessionId=:sessionId ORDER BY turnId ASC") suspend fun sessionInteractions(sessionId: Long): List<InteractionEntity>
    @Query("SELECT * FROM interactions ORDER BY id ASC") suspend fun allInteractions(): List<InteractionEntity>
    @Query("SELECT * FROM sessions ORDER BY id ASC") suspend fun allSessions(): List<SessionEntity>
    @Query("SELECT * FROM runtime_events ORDER BY id ASC") suspend fun allRuntimeEvents(): List<RuntimeEventEntity>
    @Query("SELECT COUNT(*) FROM interactions WHERE graded = 1") suspend fun interactionCount(): Int
    @Query("SELECT EXISTS(SELECT 1 FROM interactions WHERE sessionId = :sessionId AND turnId = :turnId)")
    suspend fun hasInteraction(sessionId: Long, turnId: Long): Boolean
    @Query("SELECT * FROM sessions WHERE endedAtEpochMs IS NULL ORDER BY startedAtEpochMs DESC LIMIT 1")
    suspend fun activeSession(): SessionEntity?
    @Query("""
        SELECT s.id AS id, s.mode AS mode, s.startedAtEpochMs AS startedAtEpochMs, s.endedAtEpochMs AS endedAtEpochMs,
               COALESCE(COUNT(i.id), 0) AS completedItems,
               COALESCE(SUM(CASE WHEN i.graded = 1 AND i.correct = 1 THEN 1 ELSE 0 END), 0) AS correctItems,
               COALESCE(SUM(CASE WHEN i.graded = 1 THEN 1 ELSE 0 END), 0) AS gradedResponses,
               AVG(CASE WHEN i.graded = 1 THEN i.latencyMs ELSE NULL END) AS meanLatencyMs
        FROM sessions s
        LEFT JOIN interactions i ON i.sessionId = s.id
        GROUP BY s.id
        ORDER BY s.startedAtEpochMs DESC
        LIMIT :limit
    """)
    suspend fun recentSessions(limit: Int): List<SessionSummaryRow>

    @Insert suspend fun insertRuntimeEvent(event: RuntimeEventEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreRuntimeEvents(items: List<RuntimeEventEntity>)
    @Query("SELECT * FROM runtime_events ORDER BY timestampEpochMs DESC LIMIT :limit") suspend fun recentRuntimeEvents(limit: Int): List<RuntimeEventEntity>
    @Query("DELETE FROM interactions") suspend fun clearInteractions()
    @Query("DELETE FROM runtime_events") suspend fun clearRuntimeEvents()
    @Query("DELETE FROM sessions") suspend fun clearSessions()
}

@Dao
interface EvidenceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(item: SkillEvidenceEntity): Long
    @Query("SELECT * FROM skill_evidence ORDER BY timestampEpochMs DESC LIMIT :limit") suspend fun recent(limit: Int): List<SkillEvidenceEntity>
    @Query("SELECT * FROM skill_evidence ORDER BY id ASC") suspend fun all(): List<SkillEvidenceEntity>
    @Query("DELETE FROM skill_evidence") suspend fun clear()
}



@Dao
interface DeviceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(profile: DeviceProfileEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restore(items: List<DeviceProfileEntity>)
    @Query("SELECT * FROM device_profiles") suspend fun all(): List<DeviceProfileEntity>
    @Query("SELECT * FROM device_profiles ORDER BY lastSeenAtEpochMs DESC LIMIT 1") suspend fun latest(): DeviceProfileEntity?
    @Query("DELETE FROM device_profiles") suspend fun clearProfiles()
}

@Dao
interface MetaDao {
    @Query("SELECT value FROM app_meta WHERE `key`=:key LIMIT 1") suspend fun get(key: String): String?
    @Query("SELECT * FROM app_meta") suspend fun all(): List<AppMetaEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun put(item: AppMetaEntity)
}
