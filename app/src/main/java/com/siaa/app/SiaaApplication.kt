package com.siaa.app

import android.app.Application
import com.siaa.core.algorithm.PomdpLookaheadPlanner
import com.siaa.core.algorithm.AdaptiveUtilityPlanner
import com.siaa.core.algorithm.OnlineAdaptiveModels
import com.siaa.core.algorithm.StateUpdater
import com.siaa.core.audio.AndroidEarconPort
import com.siaa.core.audio.AudioPreflight
import com.siaa.core.audio.BundledAudioSpeechPort
import com.siaa.core.data.ContentSeeder
import com.siaa.core.data.ContentPackManager
import com.siaa.core.data.LearningBackupManager
import com.siaa.core.data.RoomLearningRepository
import com.siaa.core.data.SiaaDatabase
import com.siaa.core.runtime.LessonRuntime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface ContentInitState {
    data object Loading : ContentInitState
    data object Ready : ContentInitState
    data class Error(val throwable: Throwable) : ContentInitState
}

class SiaaApplication : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
        graph.initialize()
    }
}

class AppGraph(application: Application) {
    val controlToken: String = java.util.UUID.randomUUID().toString()
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _contentState = MutableStateFlow<ContentInitState>(ContentInitState.Loading)
    val contentState = _contentState.asStateFlow()
    private val _contentReady = MutableStateFlow(false)
    val contentReady = _contentReady.asStateFlow()
    val database = SiaaDatabase.build(application)
    val repository = RoomLearningRepository(database)
    val seeder = ContentSeeder(application, database)
    val contentPackManager = ContentPackManager(application)
    val backupManager = LearningBackupManager(database)
    val practiceContent = PracticeContentRepository(application, contentPackManager)
    val productionSpeechRecognizer = ProductionSpeechRecognizer(application)
    val audioPreflight = AudioPreflight(application)
    val speech = BundledAudioSpeechPort(application)
    val earcon = AndroidEarconPort()
    val onlineModels = OnlineAdaptiveModels()
    val planner = PomdpLookaheadPlanner(AdaptiveUtilityPlanner(onlineModels))
    val runtime = LessonRuntime(
        repository = repository,
        planner = planner,
        stateUpdater = StateUpdater(),
        speech = speech,
        earcon = earcon,
        onlineModels = onlineModels
    )
    val mediaDiagnostics = MediaDiagnostics()
    val preferenceStore = PreferenceStore(application)

    fun initialize() {
        appScope.launch {
            try {
                seeder.seedIfNeeded()
                _contentState.value = ContentInitState.Ready
                _contentReady.value = true
            } catch (t: Throwable) {
                _contentState.value = ContentInitState.Error(t)
                mediaDiagnostics.record("Error al inicializar contenido: ${t.message}")
            }
        }
    }
}
