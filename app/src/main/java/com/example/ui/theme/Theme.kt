package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AnosColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color(0xFF001A24),
    primaryContainer = Color(0xFF003644),
    onPrimaryContainer = CyberCyan,
    secondary = CyberCrimson,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4D0017),
    onSecondaryContainer = Color(0xFFFFB2C1),
    tertiary = CyberNeonGreen,
    onTertiary = Color(0xFF002111),
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    outlineVariant = Color(0xFF1E2D44)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep dark futuristic identity consistent
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AnosColorScheme,
        typography = Typography,
        content = content
    )
}
