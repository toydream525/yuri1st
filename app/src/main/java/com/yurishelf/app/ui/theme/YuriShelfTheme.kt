package com.yurishelf.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.yurishelf.app.domain.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF9A3C72),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD8E9),
    onPrimaryContainer = Color(0xFF3D0028),
    secondary = Color(0xFF745663),
    background = Color(0xFFFFF7FC),
    surface = Color(0xFFFFF7FC),
    surfaceVariant = Color(0xFFF3DDE6),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFAFD2),
    onPrimary = Color(0xFF5E1043),
    primaryContainer = Color(0xFF7B285A),
    secondary = Color(0xFFE2BDCC),
    background = Color(0xFF1F1A1D),
    surface = Color(0xFF1F1A1D),
)

@Composable
fun YuriShelfTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
