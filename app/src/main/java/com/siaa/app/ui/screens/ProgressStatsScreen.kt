package com.siaa.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.siaa.core.data.UserPreferencesRepository
import com.siaa.core.model.ExerciseKind

@Composable
fun ProgressStatsScreen(
    userPreferencesRepository: UserPreferencesRepository
) {
    val progress by userPreferencesRepository.userProgress.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SiaaDarkBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Progreso y Dominio BKT",
                color = SiaaTextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Rastreo adaptativo bayesiano de conocimiento y estabilidad en memoria.",
                color = SiaaTextSecondary,
                fontSize = 13.sp
            )
        }

        // BKT Hero Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SiaaBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = SiaaSurfaceCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { progress.averageMastery.toFloat() },
                            modifier = Modifier.fillMaxSize(),
                            color = SiaaPrimaryCyan,
                            strokeWidth = 8.dp,
                            trackColor = SiaaBorder
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${(progress.averageMastery * 100).toInt()}%",
                                color = SiaaPrimaryCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "P(L)",
                                color = SiaaTextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Probabilidad de Dominio",
                            color = SiaaTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Calculado con el modelo Bayesian Knowledge Tracing según aciertos y slips.",
                            color = SiaaTextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column {
                                Text("Aciertos", color = SiaaTextMuted, fontSize = 11.sp)
                                Text("${progress.correctAttempts}/${progress.totalAttempts}", color = SiaaAccentGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column {
                                Text("Precisión", color = SiaaTextMuted, fontSize = 11.sp)
                                Text("${progress.overallAccuracy.toInt()}%", color = SiaaAccentAmber, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        // Daily Streaks & Time Card
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, SiaaBorder, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = SiaaSurfaceCard),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = SiaaAccentAmber, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Racha", color = SiaaTextSecondary, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${progress.currentStreakDays} días", color = SiaaTextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Meta diaria cumplida", color = SiaaAccentGreen, fontSize = 11.sp)
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, SiaaBorder, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = SiaaSurfaceCard),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = SiaaPrimaryCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tiempo Hoy", color = SiaaTextSecondary, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${progress.minutesStudiedToday} min", color = SiaaTextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Meta: ${progress.dailyGoalMinutes} min", color = SiaaTextMuted, fontSize = 11.sp)
                    }
                }
            }
        }

        // Skill Breakdown
        item {
            Text(
                text = "Desglose por Habilidad",
                color = SiaaTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SiaaBorder, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = SiaaSurfaceCard),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SkillProgressRow("Comprensión Auditiva (Listening)", 0.82f, SiaaPrimaryCyan)
                    SkillProgressRow("Discriminación Fonológica", 0.68f, SiaaAccentAmber)
                    SkillProgressRow("Frases y Expresiones Idiomáticas", 0.74f, SiaaAccentGreen)
                    SkillProgressRow("Deletreo y Alfabeto (Pre-A1)", 0.95f, SiaaPrimaryCyan)
                }
            }
        }

        // Spaced Repetition Stability explanation
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SiaaBorder, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = SiaaSurfaceVariant.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = SiaaPrimaryCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Curva de Estabilidad de Memoria (Half-life)", color = SiaaTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "R(t) = 2^(-Δt/S). Cada repetición auditiva exitosa incrementa el intervalo de repaso de 24h a 72h y 168h para asegurar retención a largo plazo.",
                        color = SiaaTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SkillProgressRow(title: String, progress: Float, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, color = SiaaTextPrimary, fontSize = 13.sp)
            Text("${(progress * 100).toInt()}%", color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = SiaaBorder
        )
    }
}
