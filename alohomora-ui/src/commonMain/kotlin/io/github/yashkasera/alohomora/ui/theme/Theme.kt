package io.github.yashkasera.alohomora.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Composable
fun AppTheme(
    isDarkState: MutableState<Boolean>? = null,
    initialIsDark: Boolean = isSystemInDarkTheme(),
    themeId: String = "default",
    content: @Composable () -> Unit,
) {
    val ownedState = remember { mutableStateOf(initialIsDark) }
    val effectiveState = isDarkState ?: ownedState
    val isDark by effectiveState
    val theme = AlohomoraThemes.forId(themeId, isDark)

    CompositionLocalProvider(
        LocalThemeIsDark provides effectiveState,
        LocalAlohomoraDimens provides AlohomoraDimens(),
        LocalAlohomoraColors provides theme,
    ) {
        MaterialTheme(
            colorScheme = theme.materialColorScheme,
            typography = AlohomoraTypography(),
            content = content,
        )
    }
}

val LocalThemeIsDark = compositionLocalOf { mutableStateOf(true) }
