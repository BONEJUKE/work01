package com.example.calendar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat

private val LightColors = lightColorScheme(
    primary = Color(0xFF3366FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE3FF),
    onPrimaryContainer = Color(0xFF001352),
    secondary = Color(0xFF44546A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDE2F1),
    onSecondaryContainer = Color(0xFF01142A),
    background = Color(0xFFFDFBFF),
    onBackground = Color(0xFF1B1B1F),
    surface = Color(0xFFFDFBFF),
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFE1E2EC),
    onSurfaceVariant = Color(0xFF44474F)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB7C3FF),
    onPrimary = Color(0xFF032978),
    primaryContainer = Color(0xFF1C4099),
    onPrimaryContainer = Color(0xFFDDE3FF),
    secondary = Color(0xFFBEC6DC),
    onSecondary = Color(0xFF273042),
    secondaryContainer = Color(0xFF3D4759),
    onSecondaryContainer = Color(0xFFDDE2F1),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = Color(0xFF44474F),
    onSurfaceVariant = Color(0xFFC3C6D0)
)

@Composable
fun CalendarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorScheme: ColorScheme = if (darkTheme) DarkColors else LightColors,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            ViewCompat.getWindowInsetsController(view)?.isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
