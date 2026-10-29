package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CyberTerminalColorScheme = darkColorScheme(
    primary = TerminalEmerald,
    onPrimary = Color(0xFF031811),
    primaryContainer = TerminalEmeraldDim,
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = CyberCyan,
    onSecondary = Color(0xFF041E24),
    secondaryContainer = CyberCyanDim,
    onSecondaryContainer = Color(0xFFCFFAFE),
    tertiary = NeuralViolet,
    onTertiary = Color.White,
    tertiaryContainer = NeuralVioletDim,
    onTertiaryContainer = Color(0xFFEDE9FE),
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = TerminalSurface,
    onSurface = TextPrimary,
    surfaceVariant = ElevatedPanel,
    onSurfaceVariant = TextSecondary,
    outline = GridBorder,
    error = CrimsonAlert,
    onError = Color.White
)

@Composable
fun NexusCodeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CyberTerminalColorScheme,
        typography = Typography,
        content = content
    )
}
