package io.github.letsee.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DebugGreen = Color(0xFF4CAF50)
private val DebugGreenDark = Color(0xFF81C784)
private val DebugTeal = Color(0xFF00897B)
private val DebugTealLight = Color(0xFF4DB6AC)

private val DarkBackground = Color(0xFF1A1A2E)
private val DarkSurface = Color(0xFF16213E)
private val DarkSurfaceVariant = Color(0xFF1F2B47)
private val DarkOnSurface = Color(0xFFE0E0E0)
private val DarkOnBackground = Color(0xFFF5F5F5)

private val LightBackground = Color(0xFFF8F9FA)
private val LightSurface = Color(0xFFFFFFFF)
private val LightSurfaceVariant = Color(0xFFE8EAF0)
private val LightOnSurface = Color(0xFF1C1B1F)
private val LightOnBackground = Color(0xFF1C1B1F)

private val LetSeeDarkColorScheme = darkColorScheme(
    primary = DebugGreenDark,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = DebugGreenDark,
    secondary = DebugTealLight,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF004D40),
    onSecondaryContainer = DebugTealLight,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFBDBDBD),
    error = Color(0xFFEF5350),
    onError = Color.Black,
)

private val LetSeeLightColorScheme = lightColorScheme(
    primary = DebugGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = Color(0xFF1B5E20),
    secondary = DebugTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB2DFDB),
    onSecondaryContainer = Color(0xFF004D40),
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF49454F),
    error = Color(0xFFD32F2F),
    onError = Color.White,
)

@Composable
fun LetSeeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) LetSeeDarkColorScheme else LetSeeLightColorScheme,
        typography = Typography(),
        content = content,
    )
}
