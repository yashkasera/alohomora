package io.github.yashkasera.alohomora.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class AlohomoraColorTheme(
    val id: String,
    val displayName: String,
    val isDark: Boolean,
    val materialColorScheme: ColorScheme,
    val accent: Color,
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val info: Color,
    val fatal: Color,
)

val LocalAlohomoraColors = staticCompositionLocalOf<AlohomoraColorTheme> {
    error("No AlohomoraColorTheme provided")
}

val MaterialTheme.alohomoraColors: AlohomoraColorTheme
    @Composable
    @ReadOnlyComposable
    get() = LocalAlohomoraColors.current
