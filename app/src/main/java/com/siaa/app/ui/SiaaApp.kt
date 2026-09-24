package com.siaa.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.siaa.app.MainViewModel
import com.siaa.core.model.KcDomain
import com.siaa.core.model.SessionMode
import com.siaa.core.model.LearningGoal
import com.siaa.core.model.EnglishVariety
import com.siaa.core.model.SessionIntensity
import com.siaa.core.runtime.LessonState

private enum class AppTab { HOME, PRACTICE, PROGRESS, CURRICULUM, CALIBRATION, SETTINGS }

@Composable
fun SiaaApp(
    viewModel: MainViewModel,
    onStartMode: (SessionMode) -> Unit,
    onStop: () -> Unit,
    onStartCalibration: () -> Unit,
    onStopCalibration: () -> Unit,
    onStartSpeaking: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onImportContentPack: () -> Unit,
    onRollbackContent: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var tab by remember { mutableStateOf(AppTab.HOME) }
    MaterialTheme {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(selected = tab == AppTab.HOME, onClick = { tab = AppTab.HOME }, icon = {}, label = { Text("Inicio") })
                    NavigationBarItem(selected = tab == AppTab.PRACTICE, onClick = { tab = AppTab.PRACTICE }, icon = {}, label = { Text("Práctica") })
                    NavigationBarItem(selected = tab == AppTab.PROGRESS, onClick = { tab = AppTab.PROGRESS }, icon = {}, label = { Text("Progreso") })
                    NavigationBarItem(selected = tab == AppTab.CURRICULUM, onClick = { tab = AppTab.CURRICULUM }, icon = {}, label = { Text("Mapa") })
                    NavigationBarItem(selected = tab == AppTab.CALIBRATION, onClick = { tab = AppTab.CALIBRATION }, icon = {}, label = { Text("Audífonos") })
                    NavigationBarItem(selected = tab == AppTab.SETTINGS, onClick = { tab = AppTab.SETTINGS }, icon = {}, label = { Text("Ajustes") })
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (tab) {
                    AppTab.HOME -> if (!state.preferences.onboardingCompleted) {
                        OnboardingScreen(state.preferences, viewModel, onStartMode)
                    } else HomeScreen(state.contentReady, state.contentError, state.runtime, state.stats, state.preferences, onStartMode, onStop)
                    AppTab.PRACTICE -> PracticeScreen(state.practice, onStartSpeaking, viewModel::answerReading, viewModel::submitWriting, viewModel::nextSpeaking, viewModel::nextReading, viewModel::nextWriting)
                    AppTab.PROGRESS -> ProgressScreen(state.states, state.sessions, viewModel::refresh)
                    AppTab.CURRICULUM -> CurriculumScreen(state.curriculum)
                    AppTab.CALIBRATION -> CalibrationScreen(
                        lastMediaEvent = state.lastMediaEvent,
                        observedCommands = state.observedCommands,
                        profile = state.deviceProfile,
                        onClear = viewModel::clearCalibration,
                        onSave = { viewModel.saveCalibration() },
                        onAssignPrimary = viewModel::assignLastPrimary,
                        onAssignSecondary = viewModel::assignLastSecondary,
                        onAssignBack = viewModel::assignLastBack,
                        onAssignStop = viewModel::assignLastStop,
                        onStartListening = onStartCalibration,
                        onStopListening = onStopCalibration
                    )
                    AppTab.SETTINGS -> SettingsScreen(
                        preferences = state.preferences,
                        preflight = state.preflight,
                        systemMessage = state.systemMessage,
                        onMaxItems = viewModel::setMaxItems,
                        onDuration = viewModel::setTargetDuration,
                        onDailyGoal = viewModel::setDailyGoal,
                        onSpeechRate = viewModel::setSpeechRate,
                        onGoal = viewModel::setLearningGoal,
                        onVariety = viewModel::setEnglishVariety,
                        onIntensity = viewModel::setIntensity,
                        onTargetCefr = viewModel::setTargetCefr,
                        onExtendedSkills = viewModel::setExtendedSkills,
                        onAnnounceControls = viewModel::setAnnounceControls,
                        onFeedbackExplanations = viewModel::setFeedbackExplanations,
                        onPreflight = viewModel::runAudioPreflight,
                        onExportBackup = onExportBackup,
                        onImportBackup = onImportBackup,
                        onImportContentPack = onImportContentPack,
                        onRollbackContent = onRollbackContent,
                        onResetLearning = viewModel::resetLearningData
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    contentReady: Boolean,
    contentError: String?,
    runtime: com.siaa.core.runtime.RuntimeSnapshot,
    stats: com.siaa.core.model.DashboardStats?,
    preferences: com.siaa.app.UserPreferences,
    onStartMode: (SessionMode) -> Unit,
    onStop: () -> Unit
) {
    val scroll = rememberScrollState()
    Column(
        Modifier.fillMaxSize().verticalScroll(scroll).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("SIAA", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Sistema de Inglés Auditivo Adaptativo")
        if (!contentReady && contentError == null) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("Cargando currículo local…")
        }
        if (contentError != null) {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Error de contenido", fontWeight = FontWeight.SemiBold)
                    Text(contentError)
                    Text("Reinicia la aplicación después de corregir o reinstalar el paquete de contenido.")
                }
            }
        }
        stats?.let {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Estado actual", fontWeight = FontWeight.SemiBold)
                    Text("Nivel estimado: ${it.currentCefrEstimate}")
                    Text("KCs dominados: ${it.masteredKcs}/${it.totalKcs}")
                    Text("Repasos debidos: ${it.dueKcs}")
                    Text("Dominio medio: ${(it.averageMastery * 100).toInt()} %")
                    Text("Interacciones registradas: ${it.totalInteractions}")
                }
            }
        }
        if (runtime.state !in setOf(LessonState.IDLE, LessonState.SESSION_END, LessonState.ERROR)) {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Sesión en curso", fontWeight = FontWeight.SemiBold)
                    Text("${runtime.mode} · ${runtime.message}")
                    Text("Completadas: ${runtime.completedItems} · correctas: ${runtime.correctItems}")
                    if (runtime.currentKcId != null) Text("KC: ${runtime.currentKcId}")
                    Button(onClick = onStop) { Text("Detener sesión") }
                }
            }
        }
        Text("Entrenamiento", style = MaterialTheme.typography.titleLarge)
        if (!preferences.placementCompleted) ModeButton("Diagnóstico inicial adaptativo", "Encuentra rápidamente tu punto de partida por dominio.", SessionMode.PLACEMENT, contentReady, onStartMode)
        ModeButton("Sesión adaptativa", "Mezcla gramática, vocabulario, listening, ortografía y repasos.", SessionMode.ADAPTIVE, contentReady, onStartMode)
        ModeButton("Vocabulario", "Significado ↔ sonido ↔ escritura ↔ uso.", SessionMode.VOCABULARY, contentReady, onStartMode)
        ModeButton("Ortografía y deletreo", "Letras inglesas, spelling, patrones grafema-sonido.", SessionMode.SPELLING, contentReady, onStartMode)
        ModeButton("Gramática", "Reglas, contraste, construcción y transferencia.", SessionMode.GRAMMAR, contentReady, onStartMode)
        ModeButton("Listening", "Segmentación, comprensión y discriminación auditiva.", SessionMode.LISTENING, contentReady, onStartMode)
        ModeButton("Pronunciación", "Discriminación perceptiva y formas sonoras.", SessionMode.PRONUNCIATION, contentReady, onStartMode)
        if (runtime.lastSessionSummary.isNotBlank()) {
            HorizontalDivider(); Text("Último resumen", fontWeight = FontWeight.SemiBold); Text(runtime.lastSessionSummary)
        }
        HorizontalDivider()
        Text("Controles durante preguntas A/B", fontWeight = FontWeight.SemiBold)
        Text("Play/Pause = A · Siguiente = B · Anterior = repetir. En autoevaluación: Play/Pause = sí · Siguiente = dudé · Anterior = no.")
        Text("La sesión continúa con la pantalla apagada mediante MediaSessionService.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ModeButton(
    title: String,
    subtitle: String,
    mode: SessionMode,
    enabled: Boolean,
    onStartMode: (SessionMode) -> Unit
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = { onStartMode(mode) }, enabled = enabled) { Text("Iniciar") }
        }
    }
}

@Composable
private fun OnboardingScreen(preferences: com.siaa.app.UserPreferences, viewModel: MainViewModel, onStartMode:(SessionMode)->Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text("Configura SIAA", style=MaterialTheme.typography.headlineMedium, fontWeight=FontWeight.Bold)
        Text("Elige una meta aproximada. El diagnóstico adaptativo la corregirá con evidencia objetiva.")
        EnumSelector("Objetivo",preferences.learningGoal,LearningGoal.entries,viewModel::setLearningGoal)
        EnumSelector("Variedad",preferences.englishVariety,EnglishVariety.entries,viewModel::setEnglishVariety)
        EnumSelector("Intensidad",preferences.intensity,SessionIntensity.entries,viewModel::setIntensity)
        StringSelector("Meta CEFR",preferences.targetCefr,listOf("A1","A2","B1","B2","C1","C2"),viewModel::setTargetCefr)
        Text("Viaje/sesión habitual: ${preferences.targetDurationMinutes} min")
        Slider(preferences.targetDurationMinutes.toFloat(),{viewModel.setTargetDuration(it.toInt())},valueRange=5f..90f,steps=16)
        Button(onClick={ viewModel.completeOnboarding(); onStartMode(SessionMode.PLACEMENT) }, modifier=Modifier.fillMaxWidth()){Text("Guardar e iniciar diagnóstico")}
        TextButton(onClick=viewModel::completeOnboarding){Text("Configurar ahora y diagnosticar después")}
    }
}

@Composable
private fun PracticeScreen(
    practice: com.siaa.app.PracticeUiState,
    onStartSpeaking:()->Unit,
    onReading:(String)->Unit,
    onWriting:(String)->Unit,
    onNextSpeaking:()->Unit,
    onNextReading:()->Unit,
    onNextWriting:()->Unit
) {
    var writing by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text("Producción y habilidades de pantalla",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        if(practice.lastResult.isNotBlank()) AssistChip(onClick={},label={Text(practice.lastResult)})
        practice.speaking?.let { x -> OutlinedCard(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Speaking / shadowing · ${x.cefr}",fontWeight=FontWeight.SemiBold);Text(x.promptEs);Text(x.targetEn,style=MaterialTheme.typography.titleMedium);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick=onStartSpeaking){Text("Hablar y evaluar")};TextButton(onClick=onNextSpeaking){Text("Otra")}}}} }
        practice.reading?.let { x -> OutlinedCard(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Reading · ${x.cefr}",fontWeight=FontWeight.SemiBold);Text(x.passage);Text(x.questionEs);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={onReading("A")}){Text("A · ${x.optionA}")};Button(onClick={onReading("B")}){Text("B · ${x.optionB}")}};TextButton(onClick=onNextReading){Text("Otra")}}} }
        practice.writing?.let { x -> OutlinedCard(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Writing · ${x.cefr}",fontWeight=FontWeight.SemiBold);Text(x.promptEs);OutlinedTextField(writing,{writing=it},modifier=Modifier.fillMaxWidth(),minLines=3);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={onWriting(writing);writing=""}){Text("Evaluar")};TextButton(onClick=onNextWriting){Text("Otra")}}}} }
        Text("Speaking usa reconocimiento de voz como evidencia de producción. No equivale a una calificación fonética clínica: el score compara la transcripción con el objetivo y se guarda separado de la autoevaluación.",style=MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ProgressScreen(
    states: List<com.siaa.core.model.LearnerKcState>,
    sessions: List<com.siaa.core.model.SessionSummary>,
    onRefresh: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Progreso", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            TextButton(onClick = onRefresh) { Text("Actualizar") }
        }
        if (sessions.isNotEmpty()) {
            Text("Sesiones recientes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            val fmt = remember { java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault()) }
            sessions.take(8).forEach { session ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp)) {
                        Text("${session.mode} · ${fmt.format(java.util.Date(session.startedAtEpochMs))}", fontWeight = FontWeight.Medium)
                        Text("${session.completedItems} actividades · ${(session.accuracy * 100).toInt()} % correctas" +
                            (session.meanLatencyMs?.let { " · latencia media ${(it / 1000.0).let { s -> "%.1f".format(s) }} s" } ?: ""),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            HorizontalDivider()
        }
        if (states.isEmpty()) Text("Aún no hay evidencia suficiente.")
        Text("Componentes de conocimiento", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        states.sortedByDescending { it.totalAttempts }.forEach { s ->
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(s.kcId, fontWeight = FontWeight.Medium)
                    Text("Dominio ${(s.mastery * 100).toInt()} % · retención h½ ${"%.1f".format(s.halfLifeHours)} h")
                    Text("Reconocimiento ${(s.recognition * 100).toInt()} % · producción ${(s.production * 100).toInt()} % · ortografía ${(s.orthography * 100).toInt()} %")
                    Text("Intentos ${s.totalAttempts} · aciertos ${s.totalCorrect}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun CurriculumScreen(items: List<com.siaa.app.CurriculumItem>) {
    var level by remember { mutableStateOf("Todos") }
    var domain by remember { mutableStateOf("Todos") }
    val levels = listOf("Todos", "Pre-A1", "A1", "A2", "B1", "B2", "C1", "C2")
    val domains = listOf("Todos") + KcDomain.entries.map { it.name }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Mapa curricular", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Cada KC muestra dominio, readiness y si sus prerrequisitos duros permiten trabajarlo.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            var levelOpen by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { levelOpen = true }) { Text(level) }
                DropdownMenu(expanded = levelOpen, onDismissRequest = { levelOpen = false }) {
                    levels.forEach { x -> DropdownMenuItem(text = { Text(x) }, onClick = { level = x; levelOpen = false }) }
                }
            }
            var domainOpen by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { domainOpen = true }) { Text(domain) }
                DropdownMenu(expanded = domainOpen, onDismissRequest = { domainOpen = false }) {
                    domains.forEach { x -> DropdownMenuItem(text = { Text(x) }, onClick = { domain = x; domainOpen = false }) }
                }
            }
        }
        items.asSequence()
            .filter { level == "Todos" || it.cefr.equals(level, true) }
            .filter { domain == "Todos" || it.domain.name == domain }
            .forEach { item ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(item.name, fontWeight = FontWeight.SemiBold)
                            Text("${item.cefr} · ${item.domain.name}", style = MaterialTheme.typography.bodySmall)
                        }
                        LinearProgressIndicator(progress = { item.mastery.toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                        Text("Dominio ${(item.mastery * 100).toInt()} % · readiness ${(item.readiness * 100).toInt()} % · ${if (item.unlocked) "desbloqueado" else "bloqueado"}", style = MaterialTheme.typography.bodySmall)
                        Text(item.id, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
    }
}

@Composable
private fun CalibrationScreen(
    lastMediaEvent: String,
    observedCommands: Set<String>,
    profile: com.siaa.core.model.DeviceProfile?,
    onClear: () -> Unit,
    onSave: () -> Unit,
    onAssignPrimary: () -> Unit,
    onAssignSecondary: () -> Unit,
    onAssignBack: () -> Unit,
    onAssignStop: () -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Calibración de audífonos", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Pulsa ‘Escuchar controles’ y luego prueba cada gesto de tus audífonos. Android no entrega el número de taps físico; entrega comandos multimedia generados por el firmware.")
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Último evento recibido", fontWeight = FontWeight.SemiBold)
                Text(lastMediaEvent)
                Text("Comandos observados: ${observedCommands.sorted().joinToString().ifBlank { "ninguno" }}")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onStartListening) { Text("Escuchar controles") }
            OutlinedButton(onClick = onStopListening) { Text("Detener escucha") }
        }
        Text("Para controles no estándar: pulsa un gesto, luego asigna el último keyCode recibido.", style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = onAssignPrimary) { Text("→ Play") }
            OutlinedButton(onClick = onAssignSecondary) { Text("→ Next") }
            OutlinedButton(onClick = onAssignBack) { Text("→ Previous") }
            OutlinedButton(onClick = onAssignStop) { Text("→ Stop") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSave) { Text("Guardar perfil") }
            OutlinedButton(onClick = onClear) { Text("Limpiar") }
        }
        profile?.let {
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("Perfil guardado", fontWeight = FontWeight.SemiBold)
                    Text(it.name)
                    Text("Play/Pause: ${it.playPauseAvailable} · Next: ${it.nextAvailable} · Previous: ${it.previousAvailable}")
                }
            }
        }
        Text("Objetivo mínimo: Play/Pause y Next. Previous mejora la experiencia porque permite repetir o marcar ‘no’.")
        Text("Si tu modelo reasigna doble/triple toque desde su app del fabricante, configúralo para exponer Play/Pause, Next y Previous.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SettingsScreen(
    preferences: com.siaa.app.UserPreferences,
    preflight: com.siaa.core.audio.AudioPreflightReport?,
    systemMessage: String,
    onMaxItems: (Int) -> Unit,
    onDuration: (Int) -> Unit,
    onDailyGoal: (Int) -> Unit,
    onSpeechRate: (Float) -> Unit,
    onGoal: (LearningGoal) -> Unit,
    onVariety: (EnglishVariety) -> Unit,
    onIntensity: (SessionIntensity) -> Unit,
    onTargetCefr: (String) -> Unit,
    onExtendedSkills: (Boolean) -> Unit,
    onAnnounceControls: (Boolean) -> Unit,
    onFeedbackExplanations: (Boolean) -> Unit,
    onPreflight: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onImportContentPack: () -> Unit,
    onRollbackContent: () -> Unit,
    onResetLearning: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Ajustes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (systemMessage.isNotBlank()) AssistChip(onClick={}, label={ Text(systemMessage) })
        OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Plan de aprendizaje", fontWeight = FontWeight.SemiBold)
            Text("Duración objetivo: ${preferences.targetDurationMinutes} min")
            Slider(preferences.targetDurationMinutes.toFloat(), { onDuration(it.toInt()) }, valueRange=5f..90f, steps=16)
            Text("Meta diaria: ${preferences.dailyGoalMinutes} min")
            Slider(preferences.dailyGoalMinutes.toFloat(), { onDailyGoal(it.toInt()) }, valueRange=5f..120f, steps=22)
            Text("Máximo de actividades: ${preferences.maxItems}")
            Slider(preferences.maxItems.toFloat(), { onMaxItems(it.toInt()) }, valueRange=10f..150f, steps=13)
            Text("Velocidad de voz: ${"%.2f".format(preferences.speechRate)}×")
            Slider(preferences.speechRate, onSpeechRate, valueRange=0.65f..1.5f)
            EnumSelector("Objetivo", preferences.learningGoal, LearningGoal.entries, onGoal)
            EnumSelector("Variedad", preferences.englishVariety, EnglishVariety.entries, onVariety)
            EnumSelector("Intensidad", preferences.intensity, SessionIntensity.entries, onIntensity)
            StringSelector("Meta CEFR", preferences.targetCefr, listOf("Pre-A1","A1","A2","B1","B2","C1","C2"), onTargetCefr)
            ToggleRow("Anunciar controles",preferences.announceControls,onAnnounceControls)
            ToggleRow("Explicación tras respuesta",preferences.feedbackExplanations,onFeedbackExplanations)
            ToggleRow("Speaking / reading / writing",preferences.extendedSkillsEnabled,onExtendedSkills)
        }}
        OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Preflight", fontWeight=FontWeight.SemiBold)
            Text(preflight?.message ?: "Comprueba TTS inglés/español y ruta de audio antes de una sesión.")
            Button(onClick=onPreflight){ Text("Comprobar audio/TTS") }
        }}
        OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Datos y contenido", fontWeight=FontWeight.SemiBold)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ Button(onClick=onExportBackup){Text("Exportar")}; OutlinedButton(onClick=onImportBackup){Text("Importar")}}
            Button(onClick=onImportContentPack){Text("Instalar content pack firmado")}
            OutlinedButton(onClick=onRollbackContent){Text("Volver al contenido incluido")}
            OutlinedButton(onClick=onResetLearning){Text("Reiniciar progreso de aprendizaje")}
            Text("El backup guarda progreso, evidencia, sesiones y calibración; el contenido se distribuye por packs firmados.", style=MaterialTheme.typography.bodySmall)
        }}
        SettingCard("Offline-first", "Grafo, modelo del alumno, planificador, contenido, práctica y base Room funcionan localmente.")
        SettingCard("Privacidad", "No hay telemetría remota obligatoria. Los datos pueden exportarse para análisis/calibración con decisión explícita del usuario.")
    }
}

@Composable
private fun <T: Enum<T>> EnumSelector(label:String,value:T,values:List<T>,onChange:(T)->Unit){
    var open by remember { mutableStateOf(false) }; Box { OutlinedButton(onClick={open=true}){Text("$label: ${value.name}")}; DropdownMenu(open,{open=false}){values.forEach{v->DropdownMenuItem({Text(v.name)},{onChange(v);open=false})}} }
}
@Composable private fun StringSelector(label:String,value:String,values:List<String>,onChange:(String)->Unit){ var open by remember{mutableStateOf(false)}; Box{OutlinedButton(onClick={open=true}){Text("$label: $value")};DropdownMenu(open,{open=false}){values.forEach{v->DropdownMenuItem({Text(v)},{onChange(v);open=false})}}} }
@Composable private fun ToggleRow(label:String,value:Boolean,onChange:(Boolean)->Unit){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text(label);Switch(value,onChange)}}

@Composable
private fun SettingCard(title: String, text: String) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(text)
        }
    }
}
