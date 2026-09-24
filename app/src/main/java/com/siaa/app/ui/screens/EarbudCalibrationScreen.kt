package com.siaa.app.ui.screens

import android.content.Intent
import android.os.Build
import android.view.KeyEvent
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.siaa.app.media.EarbudCommandRouter
import com.siaa.app.media.SiaaPlaybackService
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
import com.siaa.core.data.UserPreferencesRepository
import com.siaa.core.model.DeviceProfile

@Composable
fun EarbudCalibrationScreen(
    userPreferencesRepository: UserPreferencesRepository,
    earbudCommandRouter: EarbudCommandRouter
) {
    val context = LocalContext.current
    val currentProfile by userPreferencesRepository.deviceProfile.collectAsState()

    var isForegroundServiceActive by remember { mutableStateOf(false) }
    var detectedKeyLog by remember { mutableStateOf("Ninguna tecla detectada aún.") }
    var lastMappedEvent by remember { mutableStateOf<String?>(null) }

    val presetProfiles = listOf(
        DeviceProfile(
            id = 1L,
            name = "Predeterminado Estándar",
            primaryKeyCode = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            secondaryKeyCode = KeyEvent.KEYCODE_MEDIA_NEXT,
            backKeyCode = KeyEvent.KEYCODE_MEDIA_PREVIOUS,
            stopKeyCode = KeyEvent.KEYCODE_MEDIA_STOP
        ),
        DeviceProfile(
            id = 2L,
            name = "Samsung Galaxy Buds",
            primaryKeyCode = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            secondaryKeyCode = KeyEvent.KEYCODE_MEDIA_NEXT,
            backKeyCode = KeyEvent.KEYCODE_MEDIA_PREVIOUS,
            stopKeyCode = KeyEvent.KEYCODE_MEDIA_STOP
        ),
        DeviceProfile(
            id = 3L,
            name = "AirPods Pro (Media Hook)",
            primaryKeyCode = KeyEvent.KEYCODE_HEADSETHOOK,
            secondaryKeyCode = KeyEvent.KEYCODE_MEDIA_NEXT,
            backKeyCode = KeyEvent.KEYCODE_MEDIA_PREVIOUS,
            stopKeyCode = KeyEvent.KEYCODE_MEDIA_STOP
        ),
        DeviceProfile(
            id = 4L,
            name = "Sony WF / WH Serie 1000X",
            primaryKeyCode = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            secondaryKeyCode = KeyEvent.KEYCODE_MEDIA_NEXT,
            backKeyCode = KeyEvent.KEYCODE_MEDIA_PREVIOUS,
            stopKeyCode = KeyEvent.KEYCODE_MEDIA_STOP
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SiaaDarkBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Calibración de Audífonos",
                color = SiaaTextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Configura el enrutamiento de botones multimedia para interactuar con pantalla apagada.",
                color = SiaaTextSecondary,
                fontSize = 13.sp
            )
        }

        // Pocket Mode & Foreground Service toggle
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (isForegroundServiceActive) SiaaAccentGreen else SiaaBorder, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = SiaaSurfaceCard),
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
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isForegroundServiceActive) SiaaAccentGreen.copy(alpha = 0.2f) else SiaaSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (isForegroundServiceActive) SiaaAccentGreen else SiaaTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Modo Bolsillo (Foreground)",
                            color = SiaaTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (isForegroundServiceActive) "Servicio activo. Reproducción continua con pantalla bloqueada." else "Activar para escuchar con pantalla apagada.",
                            color = SiaaTextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Switch(
                        checked = isForegroundServiceActive,
                        onCheckedChange = { active ->
                            isForegroundServiceActive = active
                            val serviceIntent = Intent(context, SiaaPlaybackService::class.java)
                            if (active) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context.startForegroundService(serviceIntent)
                                } else {
                                    context.startService(serviceIntent)
                                }
                            } else {
                                context.stopService(serviceIntent)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = SiaaAccentGreen
                        )
                    )
                }
            }
        }

        // Active Profile Selector
        item {
            Text(
                text = "Perfiles de Dispositivo",
                color = SiaaTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        presetProfiles.forEach { profile ->
            val isSelected = currentProfile.name == profile.name
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, if (isSelected) SiaaPrimaryCyan else SiaaBorder, RoundedCornerShape(12.dp))
                        .clickable {
                            userPreferencesRepository.saveDeviceProfile(profile)
                        },
                    colors = CardDefaults.cardColors(containerColor = SiaaSurfaceCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (isSelected) SiaaPrimaryCyan else SiaaTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.name,
                                color = SiaaTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Play: ${KeyEvent.keyCodeToString(profile.primaryKeyCode)} | Next: ${KeyEvent.keyCodeToString(profile.secondaryKeyCode)}",
                                color = SiaaTextMuted,
                                fontSize = 11.sp
                            )
                        }
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SiaaPrimaryCyan.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Activo",
                                    color = SiaaPrimaryCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live Key Event Detector & Router Tester
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SiaaBorder, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = SiaaSurfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = SiaaAccentAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Probador de Enrutamiento (Earbud Router)",
                            color = SiaaTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Pulsa botones en tu headset o prueba los eventos para verificar cómo se interpretan en SIAA:",
                        color = SiaaTextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val ev = earbudCommandRouter.mapKeyCodeToEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, currentProfile)
                                detectedKeyLog = "KEYCODE_MEDIA_PLAY_PAUSE (${KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE})"
                                lastMappedEvent = ev?.name ?: "Ignorado"
                            },
                            modifier = Modifier.weight(1f).height(40.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SiaaSurfaceVariant)
                        ) {
                            Text("Test Play", fontSize = 11.sp, color = SiaaTextPrimary)
                        }

                        Button(
                            onClick = {
                                val ev = earbudCommandRouter.mapKeyCodeToEvent(KeyEvent.KEYCODE_MEDIA_NEXT, currentProfile)
                                detectedKeyLog = "KEYCODE_MEDIA_NEXT (${KeyEvent.KEYCODE_MEDIA_NEXT})"
                                lastMappedEvent = ev?.name ?: "Ignorado"
                            },
                            modifier = Modifier.weight(1f).height(40.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SiaaSurfaceVariant)
                        ) {
                            Text("Test Next", fontSize = 11.sp, color = SiaaTextPrimary)
                        }

                        Button(
                            onClick = {
                                val ev = earbudCommandRouter.mapKeyCodeToEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS, currentProfile)
                                detectedKeyLog = "KEYCODE_MEDIA_PREVIOUS (${KeyEvent.KEYCODE_MEDIA_PREVIOUS})"
                                lastMappedEvent = ev?.name ?: "Ignorado"
                            },
                            modifier = Modifier.weight(1f).height(40.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SiaaSurfaceVariant)
                        ) {
                            Text("Test Prev", fontSize = 11.sp, color = SiaaTextPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Output display
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "Última Tecla: $detectedKeyLog",
                                color = SiaaTextSecondary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Acción SIAA resultante: ${lastMappedEvent ?: "En espera"}",
                                color = SiaaPrimaryCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
