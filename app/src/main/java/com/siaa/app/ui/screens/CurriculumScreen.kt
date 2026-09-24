package com.siaa.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.siaa.app.media.AudioPlayerController
import com.siaa.app.ui.theme.SiaaAccentAmber
import com.siaa.app.ui.theme.SiaaAccentGreen
import com.siaa.app.ui.theme.SiaaBorder
import com.siaa.app.ui.theme.SiaaDarkBg
import com.siaa.app.ui.theme.SiaaPrimaryCyan
import com.siaa.app.ui.theme.SiaaSurfaceCard
import com.siaa.app.ui.theme.SiaaSurfaceVariant
import com.siaa.app.ui.theme.SiaaTextMuted
import com.siaa.app.ui.theme.SiaaTextPrimary
import com.siaa.app.ui.theme.SiaaTextSecondary
import com.siaa.core.data.CurriculumRepository
import com.siaa.core.model.CefrLevel
import com.siaa.core.model.ExerciseItem
import com.siaa.core.model.ExerciseKind
import com.siaa.core.runtime.SessionStateMachine

@Composable
fun CurriculumScreen(
    curriculumRepository: CurriculumRepository,
    sessionStateMachine: SessionStateMachine,
    audioPlayerController: AudioPlayerController,
    onStartPractice: () -> Unit
) {
    val levels = CefrLevel.values()
    var selectedLevelIndex by remember { mutableStateOf(1) } // default A1
    val currentLevel = levels[selectedLevelIndex]

    var selectedKindFilter by remember { mutableStateOf<ExerciseKind?>(null) }
    val isAudioPlaying by audioPlayerController.isPlaying.collectAsState()
    val currentTrackTitle by audioPlayerController.currentTrackTitle.collectAsState()

    val exercises = remember(selectedLevelIndex, selectedKindFilter) {
        curriculumRepository.getExercises(
            level = currentLevel.displayName,
            kind = selectedKindFilter
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SiaaDarkBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Currículo CEFR y Audio",
                color = SiaaTextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "3,648 audios sintéticos offline indexados por nivel de competencia.",
                color = SiaaTextSecondary,
                fontSize = 13.sp
            )
        }

        // Level Tabs
        item {
            ScrollableTabRow(
                selectedTabIndex = selectedLevelIndex,
                containerColor = SiaaSurfaceVariant,
                contentColor = SiaaPrimaryCyan,
                edgePadding = 8.dp,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                levels.forEachIndexed { index, level ->
                    Tab(
                        selected = selectedLevelIndex == index,
                        onClick = { selectedLevelIndex = index },
                        text = {
                            Text(
                                text = level.displayName,
                                fontWeight = if (selectedLevelIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedLevelIndex == index) SiaaPrimaryCyan else SiaaTextSecondary
                            )
                        }
                    )
                }
            }
        }

        // Level Description Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SiaaBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = SiaaSurfaceCard),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SiaaPrimaryCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentLevel.displayName,
                            color = SiaaPrimaryCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentLevel.description,
                            color = SiaaTextPrimary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${exercises.size} ítems disponibles",
                            color = SiaaAccentGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Kind Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    text = "Todos",
                    isSelected = selectedKindFilter == null,
                    onClick = { selectedKindFilter = null }
                )
                FilterChip(
                    text = "Listening",
                    isSelected = selectedKindFilter == ExerciseKind.LISTENING,
                    onClick = { selectedKindFilter = ExerciseKind.LISTENING }
                )
                FilterChip(
                    text = "Fonología",
                    isSelected = selectedKindFilter == ExerciseKind.PHONOLOGY,
                    onClick = { selectedKindFilter = ExerciseKind.PHONOLOGY }
                )
                FilterChip(
                    text = "Frases",
                    isSelected = selectedKindFilter == ExerciseKind.PHRASE,
                    onClick = { selectedKindFilter = ExerciseKind.PHRASE }
                )
                FilterChip(
                    text = "Alfabeto",
                    isSelected = selectedKindFilter == ExerciseKind.ALPHABET_SPELLING,
                    onClick = { selectedKindFilter = ExerciseKind.ALPHABET_SPELLING }
                )
            }
        }

        // Exercise items in level
        items(exercises) { exercise ->
            val isThisPlaying = isAudioPlaying && currentTrackTitle == exercise.title

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (isThisPlaying) SiaaPrimaryCyan else SiaaBorder, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = SiaaSurfaceCard),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (isThisPlaying) {
                                audioPlayerController.stop()
                            } else {
                                audioPlayerController.playAssetOrTts(
                                    assetPath = exercise.audioPath,
                                    textToSpeak = exercise.promptText,
                                    title = exercise.title
                                )
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isThisPlaying) SiaaAccentGreen else SiaaPrimaryCyan.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = if (isThisPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                            contentDescription = "Reproducir muestra",
                            tint = if (isThisPlaying) Color.Black else SiaaPrimaryCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = exercise.title,
                            color = SiaaTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = exercise.promptText,
                            color = SiaaTextSecondary,
                            fontSize = 12.sp,
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = exercise.kind.displayName,
                                color = SiaaAccentAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(text = "•", color = SiaaBorder)
                            Text(
                                text = "${exercise.speedWpm} WPM (${exercise.audioVoice})",
                                color = SiaaTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SiaaPrimaryCyan)
                            .clickable {
                                sessionStateMachine.startSession(
                                    listOf(exercise) + exercises.filter { it.id != exercise.id },
                                    sessionStateMachine.getUserProgress()
                                )
                                onStartPractice()
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("practice_item_${exercise.id}")
                    ) {
                        Text(
                            text = "Practicar",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) SiaaPrimaryCyan else SiaaSurfaceVariant)
            .border(1.dp, if (isSelected) SiaaPrimaryCyan else SiaaBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.Black else SiaaTextSecondary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 12.sp
        )
    }
}
