package io.github.yashkasera.alohomora.ui.theme.themes

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import io.github.yashkasera.alohomora.ui.theme.AlohomoraColorTheme

private val Base03 = Color(0xFF002B36)
private val Base02 = Color(0xFF073642)
private val Base01 = Color(0xFF586E75)
private val Base00 = Color(0xFF657B83)
private val Base0 = Color(0xFF839496)
private val Base1 = Color(0xFF93A1A1)
private val Base2 = Color(0xFFEEE8D5)
private val Base3 = Color(0xFFFDF6E3)
private val SolYellow = Color(0xFFB58900)
private val SolOrange = Color(0xFFCB4B16)
private val SolRed = Color(0xFFDC322F)
private val SolMagenta = Color(0xFFD33682)
private val SolViolet = Color(0xFF6C71C4)
private val SolBlue = Color(0xFF268BD2)
private val SolCyan = Color(0xFF2AA198)
private val SolGreen = Color(0xFF859900)

val SolarizedLightTheme = AlohomoraColorTheme(
    id = "solarized",
    displayName = "Solarized",
    isDark = false,
    materialColorScheme = lightColorScheme(
        primary = SolBlue,
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD0E8F8),
        onPrimaryContainer = Color(0xFF0A3A5A),
        secondary = Base01,
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Base2,
        onSecondaryContainer = Base02,
        tertiary = SolCyan,
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFC8EDEA),
        onTertiaryContainer = Color(0xFF0A4A46),
        error = SolRed,
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDADA),
        onErrorContainer = Color(0xFF5A0A0A),
        background = Base3,
        onBackground = Base00,
        surface = Base3,
        onSurface = Base00,
        surfaceVariant = Base2,
        onSurfaceVariant = Base01,
        outline = Base1,
        outlineVariant = Color(0xFFD6CEB8),
        scrim = Color(0xFF000000),
        inverseSurface = Base02,
        inverseOnSurface = Base2,
        inversePrimary = Color(0xFF6ABAEE),
        surfaceDim = Color(0xFFE0D9C5),
        surfaceBright = Base3,
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF7F0DD),
        surfaceContainer = Color(0xFFF2EBD6),
        surfaceContainerHigh = Color(0xFFEDE5D0),
        surfaceContainerHighest = Base2,
    ),
    accent = SolBlue,
    success = SolGreen,
    successContainer = Color(0x1F859900),
    warning = SolYellow,
    info = SolCyan,
    fatal = SolViolet,
)

val SolarizedDarkTheme = AlohomoraColorTheme(
    id = "solarized",
    displayName = "Solarized",
    isDark = true,
    materialColorScheme = darkColorScheme(
        primary = SolBlue,
        onPrimary = Color(0xFF001828),
        primaryContainer = Color(0xFF0A3A5A),
        onPrimaryContainer = Color(0xFF8AC4E8),
        secondary = Base1,
        onSecondary = Base03,
        secondaryContainer = Base02,
        onSecondaryContainer = Base1,
        tertiary = SolCyan,
        onTertiary = Base03,
        tertiaryContainer = Color(0xFF0A3A38),
        onTertiaryContainer = Color(0xFF6AD4CC),
        error = SolRed,
        onError = Color(0xFF2A0000),
        errorContainer = Color(0xFF4A0A0A),
        onErrorContainer = Color(0xFFFFAAAA),
        background = Base03,
        onBackground = Base0,
        surface = Base03,
        onSurface = Base0,
        surfaceVariant = Base02,
        onSurfaceVariant = Base1,
        outline = Base00,
        outlineVariant = Color(0xFF1A3A42),
        scrim = Color(0xFF000000),
        inverseSurface = Base2,
        inverseOnSurface = Base03,
        inversePrimary = Color(0xFF0A5A8A),
        surfaceDim = Color(0xFF001820),
        surfaceBright = Base02,
        surfaceContainerLowest = Color(0xFF001418),
        surfaceContainerLow = Color(0xFF002028),
        surfaceContainer = Color(0xFF052A35),
        surfaceContainerHigh = Color(0xFF0A3540),
        surfaceContainerHighest = Base02,
    ),
    accent = SolBlue,
    success = SolGreen,
    successContainer = Color(0x1F859900),
    warning = SolYellow,
    info = SolCyan,
    fatal = SolViolet,
)
