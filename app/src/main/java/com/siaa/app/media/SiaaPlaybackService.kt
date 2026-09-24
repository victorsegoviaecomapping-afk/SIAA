package com.siaa.app.media

import android.app.Notification
import android.app.PendingIntent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import com.siaa.app.MainActivity
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import android.net.Uri
import com.siaa.app.R
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.siaa.app.SiaaApplication
import com.siaa.core.audio.AudioOutputGuard
import com.siaa.core.audio.AudioFocusController
import com.siaa.core.model.SessionCapabilities
import com.siaa.core.model.SessionMode
import com.siaa.core.model.LearningGoal
import com.siaa.core.model.EnglishVariety
import com.siaa.core.model.SessionIntensity
import com.siaa.core.runtime.RuntimeCommand
import com.siaa.core.runtime.SessionConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SiaaPlaybackService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private var session: MediaSession? = null
    private var activeDeviceProfile: com.siaa.core.model.DeviceProfile? = null
    private lateinit var audioOutputGuard: AudioOutputGuard
    private lateinit var audioFocus: AudioFocusController
    private var calibrationActive = false
    private val router = EarbudCommandRouter()
    private val interpreter = com.siaa.core.runtime.MediaCommandInterpreter()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val appGraph get() = (application as SiaaApplication).graph
    private val runtime get() = appGraph.runtime

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, notification("SIAA listo"))

        player = ExoPlayer.Builder(this).build()
        session = MediaSession.Builder(this, player)
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                        .build()
                    val playerCommands = Player.Commands.Builder()
                        .addAll(
                            Player.COMMAND_PLAY_PAUSE,
                            Player.COMMAND_STOP,
                            Player.COMMAND_SEEK_TO_NEXT,
                            Player.COMMAND_SEEK_TO_PREVIOUS
                        )
                        .build()
                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailablePlayerCommands(playerCommands)
                        .setAvailableSessionCommands(sessionCommands)
                        .build()
                }

                override fun onMediaButtonEvent(
                    session: MediaSession,
                    controllerInfo: MediaSession.ControllerInfo,
                    intent: Intent
                ): Boolean {
                    router.rawKeyCode(intent)?.let(appGraph.mediaDiagnostics::recordRawKeyCode)
                    val mapped = router.fromMediaButtonIntent(intent, activeDeviceProfile) ?: return false
                    val (mediaEvent, keyCode) = mapped
                    appGraph.mediaDiagnostics.record(mediaEvent, keyCode)

                    val snap = runtime.snapshot.value
                    serviceScope.launch {
                        snap.sessionId?.let { sid ->
                            appGraph.repository.recordRuntimeEvent(
                                com.siaa.core.model.RuntimeEvent(
                                    sessionId = sid,
                                    turnId = snap.turnId,
                                    timestampEpochMs = System.currentTimeMillis(),
                                    eventType = com.siaa.core.model.RuntimeEventType.MEDIA_COMMAND_RECEIVED,
                                    stateBefore = snap.state.name,
                                    stateAfter = snap.state.name,
                                    mediaKeyCode = keyCode,
                                    payload = mediaEvent.name
                                )
                            )
                        }
                    }

                    val cmd = interpreter.interpret(
                        event = mediaEvent,
                        state = snap.state
                    ) ?: return false

                    if (cmd == RuntimeCommand.PLAY && !audioOutputGuard.hasSafePrivateOutput()) {
                        appGraph.mediaDiagnostics.record("Reanudación bloqueada: conecta audífonos o una salida privada")
                        return true
                    }
                    return runtime.onCommand(cmd)
                }
            })
            .build()

        audioFocus = AudioFocusController(this, onTransientLoss = { runtime.pauseForFocusLoss() })
        audioOutputGuard = AudioOutputGuard(this) {
            if (calibrationActive) {
                appGraph.mediaDiagnostics.record("Calibración detenida: se perdió la salida privada")
                calibrationActive = false
                deactivateMediaAnchor()
                audioFocus.abandon()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            } else {
                runtime.pauseForRouteChange()
            }
        }
        audioOutputGuard.register()

        serviceScope.launch {
            runtime.snapshot.collectLatest { snap ->
                val manager = getSystemService(NotificationManager::class.java)
                manager.notify(NOTIFICATION_ID, notification("${snap.mode}: ${snap.message} · ${snap.turnsCompleted}"))
                if (snap.state == com.siaa.core.runtime.LessonState.SESSION_END || snap.state == com.siaa.core.runtime.LessonState.ERROR) {
                    calibrationActive = false
                    finishService()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val isInternalControl = action in setOf(
            ACTION_START_SESSION,
            ACTION_STOP_SESSION,
            ACTION_START_CALIBRATION,
            ACTION_STOP_CALIBRATION
        )
        if (isInternalControl && intent?.getStringExtra(EXTRA_CONTROL_TOKEN) != appGraph.controlToken) {
            appGraph.mediaDiagnostics.record("Control interno rechazado: token inválido")
            stopServiceIfIdle(startId)
            return START_NOT_STICKY
        }
        when (action) {
            ACTION_START_SESSION -> {
                val mode = intent.getStringExtra(EXTRA_MODE)?.let { runCatching { SessionMode.valueOf(it) }.getOrNull() }
                    ?: SessionMode.ADAPTIVE
                val maxItems = intent.getIntExtra(EXTRA_MAX_ITEMS, 60).coerceIn(1, 500)
                val targetDurationMinutes = intent.getIntExtra(EXTRA_TARGET_DURATION_MINUTES, 25).coerceIn(5, 180)
                val learningGoal = intent.getStringExtra(EXTRA_LEARNING_GOAL)?.let { runCatching { LearningGoal.valueOf(it) }.getOrNull() } ?: LearningGoal.GENERAL
                val englishVariety = intent.getStringExtra(EXTRA_ENGLISH_VARIETY)?.let { runCatching { EnglishVariety.valueOf(it) }.getOrNull() } ?: EnglishVariety.MIXED
                val intensity = intent.getStringExtra(EXTRA_INTENSITY)?.let { runCatching { SessionIntensity.valueOf(it) }.getOrNull() } ?: SessionIntensity.BALANCED
                val announceControls = intent.getBooleanExtra(EXTRA_ANNOUNCE_CONTROLS, true)
                val feedbackExplanations = intent.getBooleanExtra(EXTRA_FEEDBACK_EXPLANATIONS, true)
                val speechRate = intent.getFloatExtra(EXTRA_SPEECH_RATE, 1.0f).coerceIn(0.5f, 2.0f)
                if (!audioOutputGuard.hasSafePrivateOutput()) {
                    appGraph.mediaDiagnostics.record("Sesión bloqueada: conecta audífonos o una salida privada antes de iniciar")
                    getSystemService(NotificationManager::class.java).notify(
                        NOTIFICATION_ID, notification("Conecta audífonos para iniciar SIAA")
                    )
                    stopServiceIfIdle(startId)
                    return START_NOT_STICKY
                }
                if (audioFocus.request()) {
                    calibrationActive = false
                    activateMediaAnchor()
                    serviceScope.launch {
                        val preflight = appGraph.audioPreflight.run(requirePrivateOutput = true)
                        if (!preflight.ready) {
                            appGraph.mediaDiagnostics.record("Preflight falló: ${preflight.message}")
                            finishService(startId)
                            return@launch
                        }
                        val init = appGraph.contentState.first { it !is com.siaa.app.ContentInitState.Loading }
                        if (init is com.siaa.app.ContentInitState.Error) {
                            appGraph.mediaDiagnostics.record("Error al inicializar contenido: ${init.throwable.message}")
                            finishService()
                            return@launch
                        }
                        val latestProfile = appGraph.repository.latestDeviceProfile()
                        activeDeviceProfile = latestProfile
                        val capabilities = if (latestProfile != null) {
                            SessionCapabilities(
                                hasPrimary = latestProfile.playPauseAvailable,
                                hasSecondary = latestProfile.nextAvailable,
                                hasBack = latestProfile.previousAvailable
                            )
                        } else SessionCapabilities(
                            hasPrimary = true,
                            hasSecondary = true,
                            hasBack = false
                        )
                        runtime.start(SessionConfig(
                            mode = mode,
                            maxItems = maxItems,
                            targetDurationMinutes = targetDurationMinutes,
                            learningGoal = learningGoal,
                            englishVariety = englishVariety,
                            intensity = intensity,
                            announceControls = announceControls,
                            feedbackExplanations = feedbackExplanations,
                            capabilities = capabilities,
                            speechRate = speechRate,
                            deviceProfileId = latestProfile?.id
                        ))
                    }
                } else {
                    appGraph.mediaDiagnostics.record("AudioFocus no concedido")
                    stopServiceIfIdle(startId)
                }
            }
            ACTION_STOP_SESSION -> {
                runtime.onCommand(RuntimeCommand.STOP)
                calibrationActive = false
                finishService(startId)
            }
            ACTION_START_CALIBRATION -> {
                if (!audioOutputGuard.hasSafePrivateOutput()) {
                    appGraph.mediaDiagnostics.record("Calibración bloqueada: conecta los audífonos antes de iniciarla")
                    getSystemService(NotificationManager::class.java).notify(
                        NOTIFICATION_ID, notification("Conecta audífonos para calibrar")
                    )
                    stopServiceIfIdle(startId)
                    return START_NOT_STICKY
                }
                if (!audioFocus.request()) {
                    appGraph.mediaDiagnostics.record("Calibración bloqueada: AudioFocus no concedido")
                    stopServiceIfIdle(startId)
                    return START_NOT_STICKY
                }
                calibrationActive = true
                activateMediaAnchor()
                appGraph.mediaDiagnostics.record("Calibración activa: esperando comandos multimedia")
                getSystemService(NotificationManager::class.java).notify(
                    NOTIFICATION_ID, notification("Calibración de audífonos activa")
                )
            }
            ACTION_STOP_CALIBRATION -> {
                calibrationActive = false
                appGraph.mediaDiagnostics.record("Calibración detenida")
                finishService(startId)
            }
        }
        return START_NOT_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Mantener la sesión: el caso de uso exige pantalla apagada / UI cerrada.
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        if (runtime.snapshot.value.state !in setOf(
                com.siaa.core.runtime.LessonState.IDLE,
                com.siaa.core.runtime.LessonState.SESSION_END,
                com.siaa.core.runtime.LessonState.ERROR
            )) {
            runtime.stop()
        }
        calibrationActive = false
        if (::audioOutputGuard.isInitialized) audioOutputGuard.unregister()
        if (::audioFocus.isInitialized) audioFocus.abandon()
        session?.release()
        session = null
        player.release()
        serviceScope.cancel()
        super.onDestroy()
    }


    /**
     * Media3 enruta mejor controles externos cuando la sesión tiene un Player en estado activo.
     * El audio pedagógico real sigue siendo TTS; este WAV local, en loop y volumen cero, actúa
     * únicamente como ancla de playback para la MediaSession.
     */
    private fun activateMediaAnchor() {
        if (player.isPlaying) return
        val uri = Uri.parse("android.resource://$packageName/${R.raw.siaa_silence}")
        player.setMediaItem(MediaItem.fromUri(uri))
        player.repeatMode = Player.REPEAT_MODE_ONE
        player.volume = 0f
        player.prepare()
        player.play()
    }

    private fun deactivateMediaAnchor() {
        player.pause()
        player.clearMediaItems()
    }

    private fun stopServiceIfIdle(startId: Int) {
        val state = runtime.snapshot.value.state
        if (state in setOf(
                com.siaa.core.runtime.LessonState.IDLE,
                com.siaa.core.runtime.LessonState.SESSION_END,
                com.siaa.core.runtime.LessonState.ERROR
            ) && !calibrationActive
        ) {
            finishService(startId)
        }
    }

    private fun finishService(startId: Int? = null) {
        calibrationActive = false
        if (::player.isInitialized) deactivateMediaAnchor()
        if (::audioFocus.isInitialized) audioFocus.abandon()
        stopForeground(STOP_FOREGROUND_REMOVE)
        if (startId != null) stopSelf(startId) else stopSelf()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Sesiones SIAA", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun notification(text: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 10, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 11, Intent(this, SiaaPlaybackService::class.java).apply {
                action = ACTION_STOP_SESSION
                putExtra(EXTRA_CONTROL_TOKEN, appGraph.controlToken)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("SIAA · entrenamiento auditivo")
            .setContentText(text)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_media_pause, "Detener", stopIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_START_SESSION = "com.siaa.app.action.START_SESSION"
        const val ACTION_STOP_SESSION = "com.siaa.app.action.STOP_SESSION"
        const val ACTION_START_CALIBRATION = "com.siaa.app.action.START_CALIBRATION"
        const val ACTION_STOP_CALIBRATION = "com.siaa.app.action.STOP_CALIBRATION"
        const val EXTRA_MODE = "mode"
        const val EXTRA_MAX_ITEMS = "max_items"
        const val EXTRA_TARGET_DURATION_MINUTES = "target_duration_minutes"
        const val EXTRA_LEARNING_GOAL = "learning_goal"
        const val EXTRA_ENGLISH_VARIETY = "english_variety"
        const val EXTRA_INTENSITY = "intensity"
        const val EXTRA_ANNOUNCE_CONTROLS = "announce_controls"
        const val EXTRA_FEEDBACK_EXPLANATIONS = "feedback_explanations"
        const val EXTRA_SPEECH_RATE = "speech_rate"
        const val EXTRA_CONTROL_TOKEN = "control_token"
        private const val CHANNEL_ID = "siaa_session"
        private const val NOTIFICATION_ID = 1001
    }
}
