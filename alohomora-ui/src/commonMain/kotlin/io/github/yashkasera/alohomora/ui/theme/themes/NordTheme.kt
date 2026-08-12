package io.github.yashkasera.alohomora.ui.theme.themes

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import io.github.yashkasera.alohomora.ui.theme.AlohomoraColorTheme

private val Nord0 = Color(0xFF2E3440)
private val Nord1 = Color(0xFF3B4252)
private val Nord2 = Color(0xFF434C5E)
private val Nord3 = Color(0xFF4C566A)
private val Nord4 = Color(0xFFD8DEE9)
private val Nord5 = Color(0xFFE5E9F0)
private val Nord6 = Color(0xFFECEFF4)
private val Nord7 = Color(0xFF8FBCBB)
private val Nord8 = Color(0xFF88C0D0)
private val Nord9 = Color(0xFF81A1C1)
private val Nord10 = Color(0xFF5E81AC)
private val Nord11 = Color(0xFFBF616A)
private val Nord12 = Color(0xFFD08770)
private val Nord13 = Color(0xFFEBCB8B)
private val Nord14 = Color(0xFFA3BE8C)
private val Nord15 = Color(0xFFB48EAD)

val NordLightTheme = AlohomoraColorTheme(
    id = "nord",
    displayName = "Nord",
    isDark = false,
    materialColorScheme = lightColorScheme(
        primary = Nord10,
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD4E4F7),
        onPrimaryContainer = Color(0xFF1C3A5E),
        secondary = Nord3,
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Nord4,
        onSecondaryContainer = Nord1,
        tertiary = Nord9,
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFD2E2F2),
        onTertiaryContainer = Color(0xFF2A4A6A),
        error = Nord11,
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDADA),
        onErrorContainer = Color(0xFF5A1A1A),
        background = Nord6,
        onBackground = Nord0,
        surface = Nord6,
        onSurface = Nord0,
        surfaceVariant = Nord4,
        onSurfaceVariant = Nord3,
        outline = Color(0xFF8A91A0),
        outlineVariant = Color(0xFFCDD3DE),
        scrim = Color(0xFF000000),
        inverseSurface = Nord1,
        inverseOnSurface = Nord5,
        inversePrimary = Nord8,
        surfaceDim = Color(0xFFD0D6E0),
        surfaceBright = Nord6,
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Nord5,
        surfaceContainer = Color(0xFFE0E4EB),
        surfaceContainerHigh = Color(0xFFDADFE8),
        surfaceContainerHighest = Nord4,
    ),
    accent = Nord10,
    success = Color(0xFF5A8A5A),
    successContainer = Color(0x1F5A8A5A),
    warning = Color(0xFFA86840),
    info = Color(0xFF4A8A9A),
    fatal = Color(0xFF8A5A80),
)

val NordDarkTheme = AlohomoraColorTheme(
    id = "nord",
    displayName = "Nord",
    isDark = true,
    materialColorScheme = darkColorScheme(
        primary = Nord8,
        onPrimary = Nord0,
        primaryContainer = Color(0xFF2A4A5A),
        onPrimaryContainer = Nord8,
        secondary = Nord4,
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Nord2,
        onSecondaryContainer = Nord4,
        tertiary = Nord9,
        onTertiary = Nord0,
        tertiaryContainer = Color(0xFF2A3A5A),
        onTertiaryContainer = Nord9,
        error = Nord11,
        onError = Color(0xFF3A0A0A),
        errorContainer = Color(0xFF5A1A1A),
        onErrorContainer = Color(0xFFFFBBBB),
        background = Nord0,
        onBackground = Nord4,
        surface = Nord0,
        onSurface = Nord4,
        surfaceVariant = Nord2,
        onSurfaceVariant = Nord4,
        outline = Nord3,
        outlineVariant = Color(0xFF3A4050),
        scrim = Color(0xFF000000),
        inverseSurface = Nord5,
        inverseOnSurface = Nord0,
        inversePrimary = Nord10,
        surfaceDim = Color(0xFF232830),
        surfaceBright = Nord2,
        surfaceContainerLowest = Color(0xFF1E222A),
        surfaceContainerLow = Color(0xFF282D38),
        surfaceContainer = Nord1,
        surfaceContainerHigh = Color(0xFF3E4555),
        surfaceContainerHighest = Nord2,
    ),
    accent = Nord8,
    success = Nord14,
    successContainer = Color(0x1FA3BE8C),
    warning = Nord12,
    info = Nord9,
    fatal = Nord15,
)
