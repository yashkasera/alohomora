package io.github.yashkasera.alohomora.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Composable
fun AlohomoraTheme(
    content: @Composable () -> Unit
) {
    val systemIsDark = isSystemInDarkTheme()
    val isDarkState = remember(systemIsDark) { mutableStateOf(true) }
    CompositionLocalProvider(
        LocalThemeIsDark provides isDarkState,
        LocalAlohomoraDimens provides AlohomoraDimens(),
    ) {
        val isDark by isDarkState
        MaterialTheme(
            colorScheme = if (isDark) CanvasDarkColorScheme else CanvasLightColorScheme,
            typography = AlohomoraTypography(),
            shapes = AlohomoraShapes,
            content = { Surface(content = content) }
        )
    }
}

val LocalThemeIsDark = compositionLocalOf { mutableStateOf(true) }
