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
    // Same recipe as successContainer: a 12% tint of the status colour, laid over surface, so
    // content on it keeps the onSurface/onSurfaceVariant contract in every theme.
    val warningContainer: Color = warning.copy(alpha = 0.12f),
    val info: Color,
    val fatal: Color,
)

val LocalAlohomoraColors = staticCompositionLocalOf<AlohomoraColorTheme> {
    error("No AlohomoraColorTheme provided")
}

@Suppress("UnusedReceiverParameter")
val MaterialTheme.alohomoraColors: AlohomoraColorTheme
    @Composable
    @ReadOnlyComposable
    get() = LocalAlohomoraColors.current
