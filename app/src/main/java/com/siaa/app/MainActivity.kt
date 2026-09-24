package com.siaa.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.siaa.app.media.SiaaPlaybackService
import com.siaa.app.ui.SiaaApp
import com.siaa.core.model.SessionMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel>()
    private val graph get() = (application as SiaaApplication).graph
    private var pendingSpeech = false

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private val audioPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && pendingSpeech) { pendingSpeech=false; beginSpeechRecognition() }
        else if (!granted) viewModel.setSystemMessage("El micrófono es necesario para evaluar speaking.")
    }
    private val exportBackup = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@registerForActivityResult
        lifecycleScope.launch {
            runCatching { withContext(Dispatchers.IO) {
                val root=JSONObject(graph.backupManager.exportJson()); root.put("userPreferences",graph.preferenceStore.exportJsonObject())
                contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(root.toString(2)) } ?: error("No se pudo abrir destino")
            } }
                .onSuccess { viewModel.setSystemMessage("Backup exportado correctamente.") }
                .onFailure { viewModel.setSystemMessage("Error al exportar: ${it.message}") }
        }
    }
    private val importBackup = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        lifecycleScope.launch {
            runCatching { withContext(Dispatchers.IO) {
                val text=contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("No se pudo leer backup")
                val root=JSONObject(text); graph.backupManager.importJson(root.toString()); root.optJSONObject("userPreferences")?.let(graph.preferenceStore::importJsonObject)
            } }
                .onSuccess { viewModel.setSystemMessage("Backup restaurado."); viewModel.refresh() }
                .onFailure { viewModel.setSystemMessage("Backup rechazado: ${it.message}") }
        }
    }
    private val importContentPack = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        lifecycleScope.launch {
            runCatching { withContext(Dispatchers.IO) { contentResolver.openInputStream(uri)?.use { graph.contentPackManager.install(it) } ?: error("No se pudo leer pack") } }
                .onSuccess { r ->
                    withContext(Dispatchers.IO) { graph.seeder.seedIfNeeded(force = true) }
                    viewModel.setSystemMessage("Content pack ${r.version} instalado y verificado."); viewModel.refresh()
                }.onFailure { viewModel.setSystemMessage("Content pack rechazado: ${it.message}") }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        setContent {
            SiaaApp(
                viewModel = viewModel,
                onStartMode = ::startMode,
                onStop = ::stopSession,
                onStartCalibration = ::startCalibration,
                onStopCalibration = ::stopCalibration,
                onStartSpeaking = ::requestSpeaking,
                onExportBackup = { exportBackup.launch("siaa-learning-backup.json") },
                onImportBackup = { importBackup.launch(arrayOf("application/json","text/plain")) },
                onImportContentPack = { importContentPack.launch(arrayOf("application/zip","application/octet-stream")) },
                onRollbackContent = ::rollbackContent
            )
        }
    }

    override fun onResume() { super.onResume(); viewModel.refresh() }

    private fun startMode(mode: SessionMode) {
        val p = graph.preferenceStore.current()
        val intent = Intent(this, SiaaPlaybackService::class.java).apply {
            action = SiaaPlaybackService.ACTION_START_SESSION
            putExtra(SiaaPlaybackService.EXTRA_MODE, mode.name)
            putExtra(SiaaPlaybackService.EXTRA_MAX_ITEMS, p.maxItems)
            putExtra(SiaaPlaybackService.EXTRA_TARGET_DURATION_MINUTES, p.targetDurationMinutes)
            putExtra(SiaaPlaybackService.EXTRA_LEARNING_GOAL, p.learningGoal.name)
            putExtra(SiaaPlaybackService.EXTRA_ENGLISH_VARIETY, p.englishVariety.name)
            putExtra(SiaaPlaybackService.EXTRA_INTENSITY, p.intensity.name)
            putExtra(SiaaPlaybackService.EXTRA_ANNOUNCE_CONTROLS, p.announceControls)
            putExtra(SiaaPlaybackService.EXTRA_FEEDBACK_EXPLANATIONS, p.feedbackExplanations)
            putExtra(SiaaPlaybackService.EXTRA_SPEECH_RATE, p.speechRate)
            putExtra(SiaaPlaybackService.EXTRA_CONTROL_TOKEN, graph.controlToken)
        }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun requestSpeaking() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            pendingSpeech=true; audioPermission.launch(Manifest.permission.RECORD_AUDIO)
        } else beginSpeechRecognition()
    }
    private fun beginSpeechRecognition() {
        val item=viewModel.uiState.value.practice.speaking ?: return
        viewModel.setSystemMessage("Escuchando: ${item.targetEn}")
        graph.productionSpeechRecognizer.start { result -> runOnUiThread {
            result.onSuccess(viewModel::completeSpeaking).onFailure { viewModel.setSystemMessage("No se pudo evaluar speaking: ${it.message}") }
        } }
    }

    private fun rollbackContent() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { graph.contentPackManager.rollbackToBundled(); graph.seeder.seedIfNeeded(force=true) }
            viewModel.setSystemMessage("Se restauró el contenido incluido en la app."); viewModel.refresh()
        }
    }

    private fun stopSession() { startService(Intent(this, SiaaPlaybackService::class.java).apply { action=SiaaPlaybackService.ACTION_STOP_SESSION; putExtra(SiaaPlaybackService.EXTRA_CONTROL_TOKEN,graph.controlToken) }) }
    private fun startCalibration() { ContextCompat.startForegroundService(this, Intent(this,SiaaPlaybackService::class.java).apply { action=SiaaPlaybackService.ACTION_START_CALIBRATION; putExtra(SiaaPlaybackService.EXTRA_CONTROL_TOKEN,graph.controlToken) }) }
    private fun stopCalibration() { startService(Intent(this,SiaaPlaybackService::class.java).apply { action=SiaaPlaybackService.ACTION_STOP_CALIBRATION; putExtra(SiaaPlaybackService.EXTRA_CONTROL_TOKEN,graph.controlToken) }) }
    private fun requestNotificationPermissionIfNeeded() { if (Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }
}
