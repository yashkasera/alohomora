package io.github.yashkasera.alohomora.ui.theme.themes

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import io.github.yashkasera.alohomora.ui.theme.AlohomoraColorTheme

private val DraculaBackground = Color(0xFF282A36)
private val DraculaCurrentLine = Color(0xFF44475A)
private val DraculaForeground = Color(0xFFF8F8F2)
private val DraculaComment = Color(0xFF6272A4)
private val DraculaCyan = Color(0xFF8BE9FD)
private val DraculaGreen = Color(0xFF50FA7B)
private val DraculaOrange = Color(0xFFFFB86C)
private val DraculaPink = Color(0xFFFF79C6)
private val DraculaPurple = Color(0xFFBD93F9)
private val DraculaRed = Color(0xFFFF5555)
private val DraculaYellow = Color(0xFFF1FA8C)

val DraculaLightTheme = AlohomoraColorTheme(
    id = "dracula",
    displayName = "Dracula",
    isDark = false,
    materialColorScheme = lightColorScheme(
        primary = Color(0xFF6C3FC5),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFEDE4FB),
        onPrimaryContainer = Color(0xFF3B1E78),
        secondary = Color(0xFF4B5E8A),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD6DFEF),
        onSecondaryContainer = Color(0xFF2D3E5E),
        tertiary = Color(0xFF8040A0),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFF0DFF6),
        onTertiaryContainer = Color(0xFF4E1A6E),
        error = Color(0xFFCC3333),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDADA),
        onErrorContainer = Color(0xFF7A0000),
        background = Color(0xFFFAF9FC),
        onBackground = Color(0xFF282A36),
        surface = Color(0xFFFAF9FC),
        onSurface = Color(0xFF282A36),
        surfaceVariant = Color(0xFFE8E5EE),
        onSurfaceVariant = Color(0xFF5A5870),
        outline = Color(0xFF8A88A0),
        outlineVariant = Color(0xFFD8D5E2),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFF312F3E),
        inverseOnSurface = Color(0xFFF4F2F8),
        inversePrimary = Color(0xFFCAB5F5),
        surfaceDim = Color(0xFFDEDBE6),
        surfaceBright = Color(0xFFFAF9FC),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF5F3F8),
        surfaceContainer = Color(0xFFEFEDF4),
        surfaceContainerHigh = Color(0xFFE9E7F0),
        surfaceContainerHighest = Color(0xFFE3E1EA),
    ),
    accent = Color(0xFF6C3FC5),
    success = Color(0xFF2D8F50),
    successContainer = Color(0x1F2D8F50),
    warning = Color(0xFFC47A1A),
    info = Color(0xFF2E8FAA),
    fatal = Color(0xFFC23B8C),
)

val DraculaDarkTheme = AlohomoraColorTheme(
    id = "dracula",
    displayName = "Dracula",
    isDark = true,
    materialColorScheme = darkColorScheme(
        primary = DraculaPurple,
        onPrimary = Color(0xFF1E0E3E),
        primaryContainer = Color(0xFF3B2070),
        onPrimaryContainer = DraculaPurple,
        secondary = DraculaComment,
        onSecondary = Color(0xFF1A2040),
        secondaryContainer = DraculaCurrentLine,
        onSecondaryContainer = Color(0xFFA4B2D8),
        tertiary = DraculaPink,
        onTertiary = Color(0xFF3E0028),
        tertiaryContainer = Color(0xFF601848),
        onTertiaryContainer = DraculaPink,
        error = DraculaRed,
        onError = Color(0xFF3A0000),
        errorContainer = Color(0xFF5A1010),
        onErrorContainer = Color(0xFFFFAAAA),
        background = DraculaBackground,
        onBackground = DraculaForeground,
        surface = DraculaBackground,
        onSurface = DraculaForeground,
        surfaceVariant = DraculaCurrentLine,
        onSurfaceVariant = DraculaComment,
        outline = DraculaComment,
        outlineVariant = Color(0xFF383A4A),
        scrim = Color(0xFF000000),
        inverseSurface = DraculaForeground,
        inverseOnSurface = DraculaBackground,
        inversePrimary = Color(0xFF5A30A0),
        surfaceDim = Color(0xFF1E1F2A),
        surfaceBright = Color(0xFF3A3C4E),
        surfaceContainerLowest = Color(0xFF181920),
        surfaceContainerLow = Color(0xFF242530),
        surfaceContainer = Color(0xFF2A2B38),
        surfaceContainerHigh = Color(0xFF333445),
        surfaceContainerHighest = DraculaCurrentLine,
    ),
    accent = DraculaPurple,
    success = DraculaGreen,
    successContainer = Color(0x1F50FA7B),
    warning = DraculaOrange,
    info = DraculaCyan,
    fatal = DraculaPink,
)
