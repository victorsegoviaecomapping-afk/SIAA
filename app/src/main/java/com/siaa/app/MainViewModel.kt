package com.siaa.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.siaa.core.model.DashboardStats
import com.siaa.core.model.DeviceProfile
import com.siaa.core.model.LearnerKcState
import com.siaa.core.model.KcDomain
import com.siaa.core.model.SessionPolicy
import com.siaa.core.model.*
import com.siaa.core.algorithm.ProductionScoring
import com.siaa.core.audio.AudioPreflightReport
import com.siaa.core.model.SessionSummary
import com.siaa.core.algorithm.KnowledgeGraphEngine
import com.siaa.core.runtime.RuntimeSnapshot
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val graph = (application as SiaaApplication).graph
    private val _stats = MutableStateFlow<DashboardStats?>(null)
    private val _states = MutableStateFlow<List<LearnerKcState>>(emptyList())
    private val _deviceProfile = MutableStateFlow<DeviceProfile?>(null)
    private val _curriculum = MutableStateFlow<List<CurriculumItem>>(emptyList())
    private val _sessions = MutableStateFlow<List<SessionSummary>>(emptyList())
    private val _practice = MutableStateFlow(PracticeUiState())
    private val _preflight = MutableStateFlow<AudioPreflightReport?>(null)
    private val _systemMessage = MutableStateFlow("")
    private var speakingIndex = 0
    private var readingIndex = 0
    private var writingIndex = 0

    private val mediaUi = combine(
        graph.mediaDiagnostics.lastEvent,
        graph.mediaDiagnostics.observedCommands,
        graph.preferenceStore.state
    ) { media, observed, preferences -> MediaUi(media, observed, preferences) }

    private val learnerUi = combine(_stats, _states, _deviceProfile, _curriculum, _sessions) { stats, states, profile, curriculum, sessions ->
        LearnerUi(stats, states, profile, curriculum, sessions)
    }

    private val auxiliaryUi = combine(_practice, _preflight, _systemMessage) { practice, preflight, message ->
        Triple(practice, preflight, message)
    }

    val uiState: StateFlow<MainUiState> = combine(
        graph.runtime.snapshot,
        graph.contentState,
        mediaUi,
        learnerUi,
        auxiliaryUi
    ) { runtime, contentState, media, learner, auxiliary ->
        MainUiState(
            runtime = runtime,
            contentReady = contentState is ContentInitState.Ready,
            contentError = (contentState as? ContentInitState.Error)?.throwable?.message,
            lastMediaEvent = media.lastEvent,
            observedCommands = media.observedCommands,
            stats = learner.stats,
            states = learner.states,
            deviceProfile = learner.deviceProfile,
            curriculum = learner.curriculum,
            sessions = learner.sessions,
            preferences = media.preferences,
            practice = auxiliary.first,
            preflight = auxiliary.second,
            systemMessage = auxiliary.third
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    init {
        viewModelScope.launch {
            graph.contentReady.collect { ready -> if (ready) { refresh(); refreshPractice() } }
        }
        viewModelScope.launch {
            graph.runtime.snapshot.collect { snap ->
                if (snap.state == com.siaa.core.runtime.LessonState.SESSION_END && snap.mode == SessionMode.PLACEMENT && snap.lastSessionSummary.contains("Nivel inicial estimado")) {
                    graph.preferenceStore.update { it.copy(placementCompleted = true) }
                    refresh()
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            if (!graph.contentReady.value) return@launch
            val now = System.currentTimeMillis()
            _stats.value = graph.repository.dashboardStats(now)
            val snapshot = graph.repository.loadSnapshot()
            _states.value = snapshot.states.sortedBy { it.kcId }
            val stateMap = snapshot.stateByKcId
            val kg = KnowledgeGraphEngine(snapshot.components, snapshot.edges)
            val policy = SessionPolicy()
            _curriculum.value = snapshot.components.map { kc ->
                CurriculumItem(
                    id = kc.id,
                    name = kc.name,
                    cefr = kc.cefr,
                    domain = kc.domain,
                    mastery = stateMap[kc.id]?.mastery ?: kc.priorMastery,
                    readiness = kg.readiness(kc.id, stateMap),
                    unlocked = kg.isUnlocked(kc.id, stateMap, now, policy)
                )
            }.sortedWith(compareBy<CurriculumItem> { cefrOrder(it.cefr) }.thenBy { it.domain.name }.thenBy { it.name })
            _deviceProfile.value = graph.repository.latestDeviceProfile()
            _sessions.value = graph.repository.recentSessions(20)
        }
    }


    fun setMaxItems(value: Int) = graph.preferenceStore.update { it.copy(maxItems = value) }
    fun setAnnounceControls(value: Boolean) = graph.preferenceStore.update { it.copy(announceControls = value) }
    fun setFeedbackExplanations(value: Boolean) = graph.preferenceStore.update { it.copy(feedbackExplanations = value) }
    fun setTargetDuration(value: Int) = graph.preferenceStore.update { it.copy(targetDurationMinutes = value) }
    fun setDailyGoal(value: Int) = graph.preferenceStore.update { it.copy(dailyGoalMinutes = value) }
    fun setSpeechRate(value: Float) = graph.preferenceStore.update { it.copy(speechRate = value) }
    fun setLearningGoal(value: LearningGoal) = graph.preferenceStore.update { it.copy(learningGoal = value) }
    fun setEnglishVariety(value: EnglishVariety) = graph.preferenceStore.update { it.copy(englishVariety = value) }
    fun setIntensity(value: SessionIntensity) = graph.preferenceStore.update { it.copy(intensity = value) }
    fun setTargetCefr(value: String) { graph.preferenceStore.update { it.copy(targetCefr = value) }; refreshPractice() }
    fun completeOnboarding() = graph.preferenceStore.update { it.copy(onboardingCompleted = true) }
    fun markPlacementCompleted() = graph.preferenceStore.update { it.copy(placementCompleted = true) }
    fun setExtendedSkills(value: Boolean) = graph.preferenceStore.update { it.copy(extendedSkillsEnabled = value) }

    fun runAudioPreflight() { viewModelScope.launch { _preflight.value = graph.audioPreflight.run(requirePrivateOutput = false) } }
    fun setSystemMessage(message: String) { _systemMessage.value = message }
    fun resetLearningData() { viewModelScope.launch { graph.repository.resetLearningData(); graph.preferenceStore.update { it.copy(placementCompleted=false) }; _systemMessage.value="Progreso reiniciado; contenido y preferencias generales se conservaron."; refresh() } }

    fun refreshPractice() {
        val level = graph.preferenceStore.current().targetCefr
        val c = graph.practiceContent
        val sp = c.speaking(level); val rd = c.reading(level); val wr = c.writing(level)
        _practice.value = PracticeUiState(
            speaking = sp.getOrNull(speakingIndex % sp.size.coerceAtLeast(1)),
            reading = rd.getOrNull(readingIndex % rd.size.coerceAtLeast(1)),
            writing = wr.getOrNull(writingIndex % wr.size.coerceAtLeast(1)),
            lastResult = _practice.value.lastResult
        )
    }
    fun nextSpeaking(){ speakingIndex++; refreshPractice() }
    fun nextReading(){ readingIndex++; refreshPractice() }
    fun nextWriting(){ writingIndex++; refreshPractice() }

    fun completeSpeaking(transcript: String) {
        val item=_practice.value.speaking ?: return
        val score=ProductionScoring.speakingSimilarity(item.targetEn, transcript)
        viewModelScope.launch {
            graph.repository.recordSkillEvidence(SkillEvidence(activityId=item.id,kcIds=item.kcIds,dimension=EvidenceDimension.PRONUNCIATION,score=score,timestampEpochMs=System.currentTimeMillis(),rawResponse=transcript,source="android-speech-recognizer"))
            _practice.value=_practice.value.copy(lastResult="Speaking ${(score*100).toInt()} % · $transcript")
            refresh(); nextSpeaking()
        }
    }
    fun answerReading(option:String) {
        val item=_practice.value.reading ?: return
        val score=if(option.equals(item.correctOption,true)) 1.0 else 0.0
        viewModelScope.launch {
            graph.repository.recordSkillEvidence(SkillEvidence(activityId=item.id,kcIds=item.kcIds,dimension=EvidenceDimension.READING,score=score,timestampEpochMs=System.currentTimeMillis(),rawResponse=option,source="screen-reading"))
            _practice.value=_practice.value.copy(lastResult=if(score==1.0) "Lectura correcta" else "Lectura: revisa la respuesta")
            refresh(); nextReading()
        }
    }
    fun submitWriting(text:String) {
        val item=_practice.value.writing ?: return
        val score=ProductionScoring.writingScore(text,item.keywords,item.minWords)
        viewModelScope.launch {
            graph.repository.recordSkillEvidence(SkillEvidence(activityId=item.id,kcIds=item.kcIds,dimension=EvidenceDimension.WRITING,score=score,timestampEpochMs=System.currentTimeMillis(),rawResponse=text,source="screen-writing-rubric"))
            _practice.value=_practice.value.copy(lastResult="Writing ${(score*100).toInt()} %")
            refresh(); nextWriting()
        }
    }

    private fun cefrOrder(level: String): Int = when (level.uppercase()) {
        "PRE-A1", "PREA1" -> 0
        "A1" -> 1
        "A2" -> 2
        "B1" -> 3
        "B2" -> 4
        "C1" -> 5
        "C2" -> 6
        else -> 99
    }

    fun clearCalibration() {
        graph.mediaDiagnostics.clear()
        viewModelScope.launch {
            graph.repository.clearDeviceProfiles()
            _deviceProfile.value = null
        }
    }

    fun assignLastPrimary() { graph.mediaDiagnostics.assignLast("PRIMARY") }
    fun assignLastSecondary() { graph.mediaDiagnostics.assignLast("SECONDARY") }
    fun assignLastBack() { graph.mediaDiagnostics.assignLast("BACK") }
    fun assignLastStop() { graph.mediaDiagnostics.assignLast("STOP") }

    fun saveCalibration(name: String = "Audífonos actuales") {
        viewModelScope.launch {
            val observed = graph.mediaDiagnostics.observedEvents.value
            val assigned = graph.mediaDiagnostics.assignedKeyCodes.value
            val primaryCode = assigned["PRIMARY"] ?: observed.lastOrNull { it.event == com.siaa.core.runtime.MediaControlEvent.PLAY_PAUSE || it.event == com.siaa.core.runtime.MediaControlEvent.PLAY }?.keyCode
            val secondaryCode = assigned["SECONDARY"] ?: observed.lastOrNull { it.event == com.siaa.core.runtime.MediaControlEvent.NEXT }?.keyCode
            val backCode = assigned["BACK"] ?: observed.lastOrNull { it.event == com.siaa.core.runtime.MediaControlEvent.PREVIOUS }?.keyCode
            val stopCode = assigned["STOP"] ?: observed.lastOrNull { it.event == com.siaa.core.runtime.MediaControlEvent.STOP }?.keyCode

            val profile = DeviceProfile(
                name = name,
                primaryKeyCode = primaryCode,
                secondaryKeyCode = secondaryCode,
                backKeyCode = backCode,
                stopKeyCode = stopCode,
                playPauseAvailable = primaryCode != null,
                nextAvailable = secondaryCode != null,
                previousAvailable = backCode != null,
                lastSeenAtEpochMs = System.currentTimeMillis()
            )
            graph.repository.saveDeviceProfile(profile)
            _deviceProfile.value = graph.repository.latestDeviceProfile()
            _sessions.value = graph.repository.recentSessions(20)
        }
    }
}

private data class MediaUi(
    val lastEvent: String,
    val observedCommands: Set<String>,
    val preferences: UserPreferences
)

private data class LearnerUi(
    val stats: DashboardStats?,
    val states: List<LearnerKcState>,
    val deviceProfile: DeviceProfile?,
    val curriculum: List<CurriculumItem>,
    val sessions: List<SessionSummary>
)

data class MainUiState(
    val runtime: RuntimeSnapshot = RuntimeSnapshot(),
    val contentReady: Boolean = false,
    val contentError: String? = null,
    val lastMediaEvent: String = "Sin eventos",
    val observedCommands: Set<String> = emptySet(),
    val stats: DashboardStats? = null,
    val states: List<LearnerKcState> = emptyList(),
    val deviceProfile: DeviceProfile? = null,
    val curriculum: List<CurriculumItem> = emptyList(),
    val sessions: List<SessionSummary> = emptyList(),
    val preferences: UserPreferences = UserPreferences(),
    val practice: PracticeUiState = PracticeUiState(),
    val preflight: AudioPreflightReport? = null,
    val systemMessage: String = ""
)

data class PracticeUiState(
    val speaking: SpeakingActivity? = null,
    val reading: ReadingActivity? = null,
    val writing: WritingActivity? = null,
    val lastResult: String = ""
)


data class CurriculumItem(
    val id: String,
    val name: String,
    val cefr: String,
    val domain: KcDomain,
    val mastery: Double,
    val readiness: Double,
    val unlocked: Boolean
)
