package com.siaa.core.data

import androidx.room.withTransaction
import com.siaa.core.algorithm.HalfLifeModel
import com.siaa.core.algorithm.MasteryCheckpointEvaluator
import com.siaa.core.algorithm.AdaptivePlacementEngine
import com.siaa.core.model.*
import com.siaa.core.runtime.LearningRepository

class RoomLearningRepository(
    private val db: SiaaDatabase,
    private val policy: SessionPolicy = SessionPolicy()
) : LearningRepository {
    override suspend fun loadSnapshot(): LearningSnapshot {
        val components = db.contentDao().allKcs().map { it.toModel() }
        val states = db.learnerDao().allStates().map { it.toModel() }
        return LearningSnapshot(
            components = components,
            edges = db.contentDao().allEdges().map { it.toModel() },
            states = states,
            exercises = db.contentDao().allExercises().map { it.toModel() },
            misconceptions = db.learnerDao().allMisconceptions().map { it.toModel() }
        )
    }

    override suspend fun createSession(mode: SessionMode, nowEpochMs: Long): Long =
        db.sessionDao().insertSession(SessionEntity(mode = mode.name, startedAtEpochMs = nowEpochMs))

    override suspend fun finishSession(sessionId: Long, nowEpochMs: Long) =
        db.sessionDao().finishSession(sessionId, nowEpochMs)

    override suspend fun recordInteraction(record: InteractionRecord): Long =
        db.sessionDao().insertInteraction(record.toEntity())

    override suspend fun updateLearnerState(state: LearnerKcState) = db.learnerDao().upsertState(state.toEntity())

    override suspend fun commitTurn(
        interaction: InteractionRecord,
        updatedStates: List<LearnerKcState>,
        misconceptionUpdates: List<Misconception>
    ): Long = db.withTransaction {
        updatedStates.forEach { db.learnerDao().upsertState(it.toEntity()) }
        misconceptionUpdates.forEach { db.learnerDao().upsertMisconception(it.toEntity()) }
        db.sessionDao().insertInteraction(interaction.toEntity())
    }

    override suspend fun recordRuntimeEvent(event: RuntimeEvent): Long =
        db.sessionDao().insertRuntimeEvent(event.toEntity())

    override suspend fun recentRuntimeEvents(limit: Int): List<RuntimeEvent> =
        db.sessionDao().recentRuntimeEvents(limit).map { it.toModel() }

    override suspend fun recentInteractions(limit: Int): List<InteractionRecord> =
        db.sessionDao().recentInteractions(limit).map { it.toModel() }

    override suspend fun sessionInteractions(sessionId: Long): List<InteractionRecord> =
        db.sessionDao().sessionInteractions(sessionId).map { it.toModel() }

    override suspend fun recordSkillEvidence(evidence: SkillEvidence): Long = db.withTransaction {
        val snapshot = loadSnapshot()
        val score = evidence.score.coerceIn(0.0, 1.0)
        evidence.kcIds.distinct().forEach { kcId ->
            val component = snapshot.componentById[kcId] ?: return@forEach
            val prior = snapshot.stateByKcId[kcId] ?: LearnerKcState(kcId, mastery = component.priorMastery)
            val objective = 0.18 + 0.24 * score
            val nextMastery = (prior.mastery + objective * (score - prior.mastery)).coerceIn(0.0, 1.0)
            val next = when (evidence.dimension) {
                EvidenceDimension.PRODUCTION, EvidenceDimension.PRONUNCIATION -> prior.copy(
                    mastery = nextMastery,
                    production = (prior.production + 0.35 * (score - prior.production)).coerceIn(0.0, 1.0),
                    automaticity = (prior.automaticity + 0.15 * (score - prior.automaticity)).coerceIn(0.0, 1.0),
                    totalAttempts = prior.totalAttempts + 1, totalCorrect = prior.totalCorrect + if (score >= 0.72) 1 else 0,
                    lastReviewedAtEpochMs = evidence.timestampEpochMs, uncertainty = (prior.uncertainty * 0.90).coerceAtLeast(0.05)
                )
                EvidenceDimension.WRITING -> prior.copy(
                    mastery = nextMastery,
                    production = (prior.production + 0.25 * (score - prior.production)).coerceIn(0.0, 1.0),
                    orthography = (prior.orthography + 0.35 * (score - prior.orthography)).coerceIn(0.0, 1.0),
                    totalAttempts = prior.totalAttempts + 1, totalCorrect = prior.totalCorrect + if (score >= 0.72) 1 else 0,
                    lastReviewedAtEpochMs = evidence.timestampEpochMs, uncertainty = (prior.uncertainty * 0.91).coerceAtLeast(0.05)
                )
                EvidenceDimension.READING, EvidenceDimension.LISTENING, EvidenceDimension.RECOGNITION -> prior.copy(
                    mastery = nextMastery,
                    recognition = (prior.recognition + 0.35 * (score - prior.recognition)).coerceIn(0.0, 1.0),
                    totalAttempts = prior.totalAttempts + 1, totalCorrect = prior.totalCorrect + if (score >= 0.72) 1 else 0,
                    lastReviewedAtEpochMs = evidence.timestampEpochMs, uncertainty = (prior.uncertainty * 0.91).coerceAtLeast(0.05)
                )
                EvidenceDimension.ORTHOGRAPHY -> prior.copy(
                    mastery = nextMastery,
                    orthography = (prior.orthography + 0.40 * (score - prior.orthography)).coerceIn(0.0, 1.0),
                    totalAttempts = prior.totalAttempts + 1, totalCorrect = prior.totalCorrect + if (score >= 0.72) 1 else 0,
                    lastReviewedAtEpochMs = evidence.timestampEpochMs, uncertainty = (prior.uncertainty * 0.91).coerceAtLeast(0.05)
                )
            }
            db.learnerDao().upsertState(next.toEntity())
        }
        db.evidenceDao().insert(evidence.toEntity())
    }

    override suspend fun recentSkillEvidence(limit: Int): List<SkillEvidence> =
        db.evidenceDao().recent(limit).map { it.toModel() }

    override suspend fun applyPlacementCalibration(sessionId: Long): PlacementProfile? = db.withTransaction {
        val interactions = db.sessionDao().sessionInteractions(sessionId).map { it.toModel() }
        if (interactions.count { it.graded } < 8) return@withTransaction null
        val snapshot = loadSnapshot()
        val engine = AdaptivePlacementEngine()
        val profile = engine.estimate(snapshot, interactions)
        snapshot.components.forEach { kc ->
            val prior = snapshot.stateByKcId[kc.id]
            if (prior == null || (prior.totalAttempts + prior.exposureCount) == 0) {
                val m = engine.seedMastery(kc, profile)
                db.learnerDao().upsertState(
                    LearnerKcState(
                        kcId = kc.id, mastery = m, recognition = (m + 0.04).coerceAtMost(0.76),
                        production = (m - 0.10).coerceAtLeast(0.04), orthography = (m - 0.08).coerceAtLeast(0.04),
                        automaticity = (m - 0.18).coerceAtLeast(0.03), uncertainty = (0.42 - 0.16 * profile.reliability).coerceIn(0.18,0.50)
                    ).toEntity()
                )
            }
        }
        db.metaDao().put(AppMetaEntity("placement_profile", "${profile.overallCefr}|${profile.answeredItems}|${"%.3f".format(java.util.Locale.US, profile.reliability)}"))
        profile
    }

    override suspend fun resetLearningData() = db.withTransaction {
        db.learnerDao().allStates().forEach { db.learnerDao().deleteState(it.kcId) }
        db.learnerDao().clearMisconceptions()
        db.evidenceDao().clear()
        db.sessionDao().clearInteractions()
        db.sessionDao().clearRuntimeEvents()
        db.sessionDao().clearSessions()
    }

    override suspend fun recentSessions(limit: Int): List<SessionSummary> =
        db.sessionDao().recentSessions(limit).map { row ->
            SessionSummary(
                id = row.id,
                mode = runCatching { SessionMode.valueOf(row.mode) }.getOrDefault(SessionMode.ADAPTIVE),
                startedAtEpochMs = row.startedAtEpochMs,
                endedAtEpochMs = row.endedAtEpochMs,
                completedItems = row.completedItems,
                correctItems = row.correctItems,
                meanLatencyMs = row.meanLatencyMs,
                gradedResponses = row.gradedResponses
            )
        }

    override suspend fun dashboardStats(nowEpochMs: Long): DashboardStats {
        val snapshot = loadSnapshot()
        val states = snapshot.states
        val total = snapshot.components.size
        val mastered = states.count { s ->
            MasteryCheckpointEvaluator.evaluate(s, nowEpochMs, policy).passed
        }
        val due = states.count { s ->
            val elapsed = s.lastReviewedAtEpochMs?.let { (nowEpochMs - it).coerceAtLeast(0L) / 3_600_000.0 } ?: Double.POSITIVE_INFINITY
            val recall = if (elapsed.isFinite()) HalfLifeModel.recallProbability(elapsed, s.halfLifeHours) else 0.0
            recall < 0.75 && s.totalAttempts > 0
        }
        val avgMastery = if (snapshot.components.isEmpty()) 0.0 else snapshot.components.map { kc ->
            snapshot.stateByKcId[kc.id]?.mastery ?: kc.priorMastery
        }.average()
        val avgRetention = if (snapshot.components.isEmpty()) 0.0 else snapshot.components.map { kc ->
            val s = snapshot.stateByKcId[kc.id]
            val elapsed = s?.lastReviewedAtEpochMs?.let { (nowEpochMs - it).coerceAtLeast(0L) / 3_600_000.0 } ?: Double.POSITIVE_INFINITY
            if (elapsed.isFinite() && s != null) HalfLifeModel.recallProbability(elapsed, s.halfLifeHours) else 0.0
        }.average()
        val cefr = estimateCefr(snapshot, nowEpochMs)
        return DashboardStats(total, mastered, due, avgMastery, avgRetention, db.sessionDao().interactionCount(), cefr)
    }

    override suspend fun saveDeviceProfile(profile: DeviceProfile): Long = db.deviceDao().save(profile.toEntity())
    override suspend fun latestDeviceProfile(): DeviceProfile? = db.deviceDao().latest()?.toModel()
    override suspend fun clearDeviceProfiles() = db.deviceDao().clearProfiles()

    override suspend fun hasInteraction(sessionId: Long, turnId: Long): Boolean =
        db.sessionDao().hasInteraction(sessionId, turnId)

    override suspend fun activeSession(): SessionRecord? =
        db.sessionDao().activeSession()?.let {
            SessionRecord(
                id = it.id,
                mode = runCatching { SessionMode.valueOf(it.mode) }.getOrDefault(SessionMode.ADAPTIVE),
                startedAtEpochMs = it.startedAtEpochMs,
                endedAtEpochMs = it.endedAtEpochMs
            )
        }

    private fun estimateCefr(snapshot: LearningSnapshot, nowEpochMs: Long): String =
        com.siaa.core.algorithm.CefrProgressEstimator.estimate(snapshot, nowEpochMs, policy)

}

