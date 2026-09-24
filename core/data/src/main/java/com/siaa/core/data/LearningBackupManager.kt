package com.siaa.core.data

import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject

/** Portable, local-only JSON backup. Content tables are intentionally excluded: they are restored from a signed content pack. */
class LearningBackupManager(private val db: SiaaDatabase) {
    suspend fun exportJson(): String {
        val root = JSONObject()
        root.put("format", "siaa-learning-backup")
        root.put("version", 1)
        root.put("exportedAtEpochMs", System.currentTimeMillis())
        root.put("states", JSONArray(db.learnerDao().allStates().map(::stateJson)))
        root.put("misconceptions", JSONArray(db.learnerDao().allMisconceptions().map(::misconceptionJson)))
        root.put("sessions", JSONArray(db.sessionDao().allSessions().map(::sessionJson)))
        root.put("interactions", JSONArray(db.sessionDao().allInteractions().map(::interactionJson)))
        root.put("runtimeEvents", JSONArray(db.sessionDao().allRuntimeEvents().map(::eventJson)))
        root.put("skillEvidence", JSONArray(db.evidenceDao().all().map(::evidenceJson)))
        root.put("devices", JSONArray(db.deviceDao().all().map(::deviceJson)))
        root.put("meta", JSONArray(db.metaDao().all().map { JSONObject().put("key", it.key).put("value", it.value) }))
        return root.toString(2)
    }

    suspend fun importJson(text: String) {
        val root = JSONObject(text)
        require(root.optString("format") == "siaa-learning-backup") { "Formato de backup no reconocido" }
        require(root.optInt("version") == 1) { "Versión de backup no compatible" }
        db.withTransaction {
            db.evidenceDao().clear()
            db.learnerDao().clearMisconceptions()
            db.learnerDao().allStates().forEach { db.learnerDao().deleteState(it.kcId) }
            db.sessionDao().clearInteractions(); db.sessionDao().clearRuntimeEvents(); db.sessionDao().clearSessions()
            db.deviceDao().clearProfiles()
            val states = root.optJSONArray("states").objects().map(::stateEntity)
            if (states.isNotEmpty()) db.learnerDao().upsertStates(states)
            root.optJSONArray("misconceptions").objects().forEach { o -> db.learnerDao().upsertMisconception(MisconceptionEntity(o.s("id"),o.s("kcId"),o.s("label"),o.d("probability"),o.l("lastObservedAtEpochMs"))) }
            val sessions = root.optJSONArray("sessions").objects().map { o -> SessionEntity(o.l("id"),o.s("mode"),o.l("startedAtEpochMs"),o.optLongOrNull("endedAtEpochMs")) }
            if (sessions.isNotEmpty()) db.sessionDao().restoreSessions(sessions)
            val interactions = root.optJSONArray("interactions").objects().map(::interactionEntity)
            if (interactions.isNotEmpty()) db.sessionDao().restoreInteractions(interactions)
            val events = root.optJSONArray("runtimeEvents").objects().map(::eventEntity)
            if (events.isNotEmpty()) db.sessionDao().restoreRuntimeEvents(events)
            root.optJSONArray("skillEvidence").objects().forEach { o -> db.evidenceDao().insert(evidenceEntity(o)) }
            val devices = root.optJSONArray("devices").objects().map(::deviceEntity)
            if (devices.isNotEmpty()) db.deviceDao().restore(devices)
            root.optJSONArray("meta").objects().forEach { o -> db.metaDao().put(AppMetaEntity(o.s("key"),o.s("value"))) }
        }
    }

    private fun stateJson(x: LearnerKcStateEntity)=JSONObject().apply {
        put("kcId",x.kcId); put("mastery",x.mastery); put("recognition",x.recognition); put("production",x.production); put("orthography",x.orthography); put("automaticity",x.automaticity); put("halfLifeHours",x.halfLifeHours); put("uncertainty",x.uncertainty); putNullable("lastReviewedAtEpochMs",x.lastReviewedAtEpochMs); put("consecutiveSuccess",x.consecutiveSuccess); put("consecutiveFailure",x.consecutiveFailure); put("totalAttempts",x.totalAttempts); put("totalCorrect",x.totalCorrect); put("exposureCount",x.exposureCount); putNullable("lastExposedAtEpochMs",x.lastExposedAtEpochMs); put("transferSuccesses",x.transferSuccesses); put("novelSuccesses",x.novelSuccesses)
    }
    private fun stateEntity(o:JSONObject)=LearnerKcStateEntity(o.s("kcId"),o.d("mastery"),o.d("recognition"),o.d("production"),o.d("orthography"),o.d("automaticity"),o.d("halfLifeHours"),o.d("uncertainty"),o.optLongOrNull("lastReviewedAtEpochMs"),o.i("consecutiveSuccess"),o.i("consecutiveFailure"),o.i("totalAttempts"),o.i("totalCorrect"),o.i("exposureCount"),o.optLongOrNull("lastExposedAtEpochMs"),o.i("transferSuccesses"),o.i("novelSuccesses"))
    private fun misconceptionJson(x:MisconceptionEntity)=JSONObject().put("id",x.id).put("kcId",x.kcId).put("label",x.label).put("probability",x.probability).put("lastObservedAtEpochMs",x.lastObservedAtEpochMs)
    private fun sessionJson(x:SessionEntity)=JSONObject().put("id",x.id).put("mode",x.mode).put("startedAtEpochMs",x.startedAtEpochMs).putNullable("endedAtEpochMs",x.endedAtEpochMs)
    private fun interactionJson(x:InteractionEntity)=JSONObject().apply { put("id",x.id);put("sessionId",x.sessionId);put("turnId",x.turnId);put("exerciseId",x.exerciseId);put("timestampEpochMs",x.timestampEpochMs);put("response",x.response);put("correct",x.correct);put("graded",x.graded);put("kind",x.kind);putNullable("confidence",x.confidence);putNullable("latencyMs",x.latencyMs);put("hintDepth",x.hintDepth);putNullable("plannerScore",x.plannerScore);putNullable("stateBeforeMastery",x.stateBeforeMastery);putNullable("stateAfterMastery",x.stateAfterMastery) }
    private fun interactionEntity(o:JSONObject)=InteractionEntity(o.l("id"),o.l("sessionId"),o.l("turnId"),o.s("exerciseId"),o.l("timestampEpochMs"),o.s("response"),o.b("correct"),o.b("graded"),o.s("kind"),o.optStringOrNull("confidence"),o.optLongOrNull("latencyMs"),o.i("hintDepth"),o.optDoubleOrNull("plannerScore"),o.optDoubleOrNull("stateBeforeMastery"),o.optDoubleOrNull("stateAfterMastery"))
    private fun eventJson(x:RuntimeEventEntity)=JSONObject().apply { put("id",x.id);put("sessionId",x.sessionId);put("turnId",x.turnId);put("timestampEpochMs",x.timestampEpochMs);put("eventType",x.eventType);put("stateBefore",x.stateBefore);put("stateAfter",x.stateAfter);putNullable("exerciseId",x.exerciseId);putNullable("runtimeCommand",x.runtimeCommand);putNullable("mediaKeyCode",x.mediaKeyCode);putNullable("payload",x.payload) }
    private fun eventEntity(o:JSONObject)=RuntimeEventEntity(o.l("id"),o.l("sessionId"),o.l("turnId"),o.l("timestampEpochMs"),o.s("eventType"),o.s("stateBefore"),o.s("stateAfter"),o.optStringOrNull("exerciseId"),o.optStringOrNull("runtimeCommand"),o.optIntOrNull("mediaKeyCode"),o.optStringOrNull("payload"))
    private fun evidenceJson(x:SkillEvidenceEntity)=JSONObject().apply { put("id",x.id);putNullable("sessionId",x.sessionId);put("activityId",x.activityId);put("kcIdsCsv",x.kcIdsCsv);put("dimension",x.dimension);put("score",x.score);put("timestampEpochMs",x.timestampEpochMs);putNullable("latencyMs",x.latencyMs);put("rawResponse",x.rawResponse);put("source",x.source) }
    private fun evidenceEntity(o:JSONObject)=SkillEvidenceEntity(o.l("id"),o.optLongOrNull("sessionId"),o.s("activityId"),o.s("kcIdsCsv"),o.s("dimension"),o.d("score"),o.l("timestampEpochMs"),o.optLongOrNull("latencyMs"),o.s("rawResponse"),o.s("source"))
    private fun deviceJson(x:DeviceProfileEntity)=JSONObject().apply { put("id",x.id);put("name",x.name);putNullable("primaryKeyCode",x.primaryKeyCode);putNullable("secondaryKeyCode",x.secondaryKeyCode);putNullable("backKeyCode",x.backKeyCode);putNullable("stopKeyCode",x.stopKeyCode);put("playPauseAvailable",x.playPauseAvailable);put("nextAvailable",x.nextAvailable);put("previousAvailable",x.previousAvailable);put("lastSeenAtEpochMs",x.lastSeenAtEpochMs) }
    private fun deviceEntity(o:JSONObject)=DeviceProfileEntity(o.l("id"),o.s("name"),o.optIntOrNull("primaryKeyCode"),o.optIntOrNull("secondaryKeyCode"),o.optIntOrNull("backKeyCode"),o.optIntOrNull("stopKeyCode"),o.b("playPauseAvailable"),o.b("nextAvailable"),o.b("previousAvailable"),o.l("lastSeenAtEpochMs"))

    private fun JSONArray?.objects(): List<JSONObject> = if (this==null) emptyList() else (0 until length()).map { getJSONObject(it) }
    private fun JSONObject.s(k:String)=optString(k,"")
    private fun JSONObject.i(k:String)=optInt(k,0)
    private fun JSONObject.l(k:String)=optLong(k,0L)
    private fun JSONObject.d(k:String)=optDouble(k,0.0)
    private fun JSONObject.b(k:String)=optBoolean(k,false)
    private fun JSONObject.optStringOrNull(k:String)=if (isNull(k)) null else optString(k).takeIf { it.isNotEmpty() }
    private fun JSONObject.optLongOrNull(k:String)=if (isNull(k)) null else getLong(k)
    private fun JSONObject.optIntOrNull(k:String)=if (isNull(k)) null else getInt(k)
    private fun JSONObject.optDoubleOrNull(k:String)=if (isNull(k)) null else getDouble(k)
    private fun JSONObject.putNullable(k:String,v:Any?)=apply { if(v==null) put(k,JSONObject.NULL) else put(k,v) }
}
