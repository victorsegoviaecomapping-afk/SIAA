package com.siaa.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.siaa.app.media.AudioPlayerController
import com.siaa.app.ui.theme.SiaaAccentAmber
import com.siaa.app.ui.theme.SiaaAccentGreen
import com.siaa.app.ui.theme.SiaaAccentRose
import com.siaa.app.ui.theme.SiaaBorder
import com.siaa.app.ui.theme.SiaaDarkBg
import com.siaa.app.ui.theme.SiaaPrimaryCyan
import com.siaa.app.ui.theme.SiaaSurfaceCard
import com.siaa.app.ui.theme.SiaaSurfaceVariant
import com.siaa.app.ui.theme.SiaaTextMuted
import com.siaa.app.ui.theme.SiaaTextPrimary
import com.siaa.app.ui.theme.SiaaTextSecondary
import com.siaa.core.model.ExerciseItem
import com.siaa.core.model.ExerciseOption
import com.siaa.core.runtime.HelpLadderStep
import com.siaa.core.runtime.MediaControlEvent
import com.siaa.core.runtime.SessionStateMachine

@Composable
fun StudySessionScreen(
    sessionStateMachine: SessionStateMachine,
    audioPlayerController: AudioPlayerController,
    onNavigateToCalibration: () -> Unit
) {
    val state by sessionStateMachine.uiState.collectAsState()
    val isPlaying by audioPlayerController.isPlaying.collectAsState()
    val playbackSpeed by audioPlayerController.playbackSpeed.collectAsState()

    val currentEx = state.currentExercise

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SiaaDarkBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header: Stats & Level
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SiaaPrimaryCyan.copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = currentEx?.level ?: "A1",
                            color = SiaaPrimaryCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentEx?.category ?: "Inglés Auditivo",
                        color = SiaaTextSecondary,
                        fontSize = 14.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SiaaAccentAmber.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "🔥 ${state.streak} días",
                            color = SiaaAccentAmber,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SiaaAccentGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Score: ${state.sessionScore}",
                            color = SiaaAccentGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Progress bar in exercise queue
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Ejercicio ${state.exerciseIndex} de ${state.totalExercises}",
                        color = SiaaTextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Dominio BKT: ${(state.currentMastery * 100).toInt()}%",
                        color = SiaaPrimaryCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { if (state.totalExercises > 0) state.exerciseIndex.toFloat() / state.totalExercises else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = SiaaPrimaryCyan,
                    trackColor = SiaaBorder
                )
            }
        }

        // Audio Player & Waveform Hero Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SiaaBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = SiaaSurfaceCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentEx?.title ?: "Cargando audio...",
                        color = SiaaTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Audio Soundwave visualization
                    AudioWaveformVisualizer(isPlaying = isPlaying)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Player Controls Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Speed Chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(SiaaSurfaceVariant)
                                .clickable {
                                    val nextSpeed = when (playbackSpeed) {
                                        0.75f -> 1.0f
                                        1.0f -> 1.25f
                                        else -> 0.75f
                                    }
                                    audioPlayerController.setSpeed(nextSpeed)
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "${playbackSpeed}x",
                                color = SiaaPrimaryCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Big Play / Replay button
                        IconButton(
                            onClick = {
                                if (isPlaying) {
                                    audioPlayerController.togglePlayPause()
                                } else {
                                    currentEx?.let {
                                        audioPlayerController.playAssetOrTts(
                                            assetPath = it.audioPath,
                                            textToSpeak = it.promptText,
                                            title = it.title
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(SiaaPrimaryCyan)
                                .testTag("play_button")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Refresh else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pausar/Replay" else "Reproducir",
                                tint = Color.Black,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Help Ladder Trigger
                        IconButton(
                            onClick = {
                                val step = sessionStateMachine.triggerHelpLadder()
                                if (step == HelpLadderStep.SLOW_MODEL) {
                                    audioPlayerController.setSpeed(0.75f)
                                    currentEx?.let {
                                        audioPlayerController.playAssetOrTts(it.audioPath, it.promptText, it.title)
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(SiaaSurfaceVariant)
                                .testTag("help_ladder_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Escalera de Ayuda",
                                tint = SiaaAccentAmber
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isPlaying) "Reproduciendo audio sintético offline (eSpeak)" else "Toca para reproducir o pulsa Play en tus audífonos",
                        color = SiaaTextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Help Ladder Display (Animated visibility based on steps)
        if (state.helpStep != HelpLadderStep.INITIAL && currentEx != null) {
            item {
                HelpLadderCard(
                    step = state.helpStep,
                    exercise = currentEx,
                    onReplaySlow = {
                        audioPlayerController.setSpeed(0.75f)
                        audioPlayerController.playAssetOrTts(currentEx.audioPath, currentEx.promptText, currentEx.title)
                    }
                )
            }
        }

        // Earbud Prompt Instructions Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToCalibration() },
                colors = CardDefaults.cardColors(containerColor = SiaaSurfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Audífonos",
                        tint = SiaaPrimaryCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Control Manos Libres Activo",
                            color = SiaaTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Play: Repetir | Next: Opción 1 | Prev: Opción 2",
                            color = SiaaTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = "Configurar",
                        color = SiaaPrimaryCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Exercise Options List
        currentEx?.options?.forEach { option ->
            item {
                OptionCard(
                    option = option,
                    isSelected = state.selectedOptionId == option.id,
                    isChecked = state.isAnswerChecked,
                    onSelect = {
                        sessionStateMachine.selectOption(option.id)
                    }
                )
            }
        }

        // Feedback & Next Button
        if (state.isAnswerChecked) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (state.isAnswerCorrect) SiaaAccentGreen.copy(alpha = 0.15f) else SiaaAccentRose.copy(alpha = 0.15f))
                        .border(
                            1.dp,
                            if (state.isAnswerCorrect) SiaaAccentGreen else SiaaAccentRose,
                            RoundedCornerShape(14.dp)
                        )
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (state.isAnswerCorrect) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (state.isAnswerCorrect) SiaaAccentGreen else SiaaAccentRose,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (state.isAnswerCorrect) "¡Excelente comprensión!" else "Revisa la explicación",
                            color = if (state.isAnswerCorrect) SiaaAccentGreen else SiaaAccentRose,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currentEx?.explanation ?: "",
                        color = SiaaTextPrimary,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            sessionStateMachine.nextExercise()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("next_exercise_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.isAnswerCorrect) SiaaAccentGreen else SiaaPrimaryCyan
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Siguiente Ejercicio (o pulsa Next)",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Earbud Button Simulation Panel (so user can test earbud events directly)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SiaaBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = SiaaSurfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Simulador de Botones de Audífonos",
                        color = SiaaTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = {
                                sessionStateMachine.handleMediaControlEvent(MediaControlEvent.PREVIOUS)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("sim_prev_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Prev", fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedButton(
                            onClick = {
                                sessionStateMachine.handleMediaControlEvent(MediaControlEvent.PLAY_PAUSE)
                                currentEx?.let {
                                    audioPlayerController.playAssetOrTts(it.audioPath, it.promptText, it.title)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("sim_play_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Play", fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedButton(
                            onClick = {
                                sessionStateMachine.handleMediaControlEvent(MediaControlEvent.NEXT)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("sim_next_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Next", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OptionCard(
    option: ExerciseOption,
    isSelected: Boolean,
    isChecked: Boolean,
    onSelect: () -> Unit
) {
    val borderColor = when {
        isChecked && option.isCorrect -> SiaaAccentGreen
        isChecked && isSelected && !option.isCorrect -> SiaaAccentRose
        isSelected -> SiaaPrimaryCyan
        else -> SiaaBorder
    }

    val backgroundColor = when {
        isChecked && option.isCorrect -> SiaaAccentGreen.copy(alpha = 0.12f)
        isChecked && isSelected && !option.isCorrect -> SiaaAccentRose.copy(alpha = 0.12f)
        isSelected -> SiaaPrimaryCyan.copy(alpha = 0.12f)
        else -> SiaaSurfaceCard
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = !isChecked) { onSelect() }
            .testTag("option_${option.id}"),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (option.earbudCommand == "Next") SiaaPrimaryCyan.copy(alpha = 0.2f) else SiaaAccentAmber.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = option.earbudCommand,
                    color = if (option.earbudCommand == "Next") SiaaPrimaryCyan else SiaaAccentAmber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = option.text,
                color = SiaaTextPrimary,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f)
            )

            if (isChecked) {
                if (option.isCorrect) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Correcto",
                        tint = SiaaAccentGreen,
                        modifier = Modifier.size(22.dp)
                    )
                } else if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Incorrecto",
                        tint = SiaaAccentRose,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HelpLadderCard(
    step: HelpLadderStep,
    exercise: ExerciseItem,
    onReplaySlow: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SiaaAccentAmber.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = SiaaSurfaceCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = SiaaAccentAmber,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (step) {
                        HelpLadderStep.HINT -> "Escalera de Ayuda: Nivel 1 (Pista)"
                        HelpLadderStep.SLOW_MODEL -> "Escalera de Ayuda: Nivel 2 (Modelo Lento)"
                        HelpLadderStep.FULL_EXPLANATION -> "Escalera de Ayuda: Nivel 3 (Transcripción)"
                        else -> "Ayuda"
                    },
                    color = SiaaAccentAmber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (step) {
                HelpLadderStep.HINT -> {
                    Text(
                        text = exercise.hint,
                        color = SiaaTextPrimary,
                        fontSize = 14.sp
                    )
                }
                HelpLadderStep.SLOW_MODEL -> {
                    Text(
                        text = "El audio se ha ajustado a velocidad lenta (0.75x) para facilitar la discriminación acústica de los fonemas.",
                        color = SiaaTextPrimary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onReplaySlow,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reescuchar en 0.75x", fontSize = 12.sp)
                    }
                }
                HelpLadderStep.FULL_EXPLANATION -> {
                    Text(
                        text = "Transcripción: \"${exercise.promptText}\"",
                        color = SiaaPrimaryCyan,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = exercise.explanation,
                        color = SiaaTextSecondary,
                        fontSize = 13.sp
                    )
                }
                else -> Unit
            }
        }
    }
}

@Composable
fun AudioWaveformVisualizer(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "soundwave")
    val heights = (0..15).map { index ->
        infiniteTransition.animateFloat(
            initialValue = 8f,
            targetValue = if (isPlaying) (16f + (index * 7 % 28f)) else 10f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 350 + (index * 45), easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar_$index"
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        heights.forEachIndexed { i, animatedHeight ->
            val barHeight = if (isPlaying) animatedHeight.value.dp else 8.dp
            val color = if (isPlaying) {
                if (i % 2 == 0) SiaaPrimaryCyan else SiaaAccentAmber
            } else {
                SiaaBorder
            }
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}
