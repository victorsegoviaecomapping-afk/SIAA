package com.siaa.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SiaaPrimaryCyan,
    onPrimary = SiaaOnPrimary,
    primaryContainer = SiaaPrimaryContainer,
    secondary = SiaaAccentAmber,
    tertiary = SiaaAccentGreen,
    background = SiaaDarkBg,
    surface = SiaaSurface,
    surfaceVariant = SiaaSurfaceVariant,
    onBackground = SiaaTextPrimary,
    onSurface = SiaaTextPrimary,
    onSurfaceVariant = SiaaTextSecondary,
    outline = SiaaBorder
)

@Composable
fun SiaaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
