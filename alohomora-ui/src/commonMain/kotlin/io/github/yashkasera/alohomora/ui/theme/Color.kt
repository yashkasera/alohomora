package io.github.yashkasera.alohomora.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val CanvasBlack = Color(0xFF0A0A0A)
val CanvasWhite = Color(0xFFFFFFFF)
val CanvasDarkGray = Color(0xFF2D2D2D)
val CanvasLightGray = Color(0xFFF5F4F1)
val CanvasError = Color(0xFFDC2626)
val CanvasAlertRed = Color(0xFFDC2626)
val CanvasSuccessGreen = Color(0xFF059669)

private val CanvasBrandIndigoLight = Color(0xFF6366F1)
private val CanvasBrandIndigoDark = Color(0xFFA5B4FC)
private val CanvasWarningLight = Color(0xFFD97706)
private val CanvasWarningDark = Color(0xFFFFC857)
private val CanvasMutedLight = Color(0xFF71717A)
private val CanvasMutedDark = Color(0xFFB0B0B0)
private val CanvasMutedContainerLight = Color(0xFFE4E4E7)
private val CanvasMutedContainerDark = Color(0xFF2D2D2D)
private val CanvasSubtleSurfaceLight = Color(0xFFFAFAF8)
private val CanvasSubtleSurfaceDark = Color(0xFF1A1A1A)
private val CanvasSubtleSurfaceAltLight = Color(0xFFF5F4F1)
private val CanvasSubtleSurfaceAltDark = Color(0xFF242424)
private val CanvasPanelBorderLight = Color(0xFFE4E2DC)
private val CanvasPanelBorderDark = Color(0xFF3A332B)

private val CanvasLogVerboseLight = Color(0xFF9E9E9E)
private val CanvasLogVerboseDark = Color(0xFFBDBDBD)
private val CanvasLogDebugLight = Color(0xFF1976D2)
private val CanvasLogDebugDark = Color(0xFF64B5F6)
private val CanvasLogInfoLight = Color(0xFF2E7D32)
private val CanvasLogInfoDark = Color(0xFF81C784)
private val CanvasLogWarnLight = Color(0xFFEF6C00)
private val CanvasLogWarnDark = Color(0xFFFFB74D)
private val CanvasLogErrorLight = Color(0xFFC62828)
private val CanvasLogErrorDark = Color(0xFFEF5350)
private val CanvasLogFatalLight = Color(0xFF9C27B0)
private val CanvasLogFatalDark = Color(0xFFCE93D8)

// Span kinds, for waterfall bars. A categorical palette lives here rather than in the renderer for
// the same reason the log levels do: composables stay token-only, and light/dark pairs are chosen
// side by side instead of one at a time.
//
// Five hues rather than reusing the Material accent roles, of which there are three. Squeezing five
// kinds into primary/secondary/tertiary makes CLIENT and PRODUCER identical, and telling an outbound
// call from a queue publish is one of the few things a waterfall is actually read for. Kept clear of
// the error red below, which overrides kind entirely.
private val CanvasSpanInternalLight = Color(0xFF6366F1)
private val CanvasSpanInternalDark = Color(0xFF818CF8)
private val CanvasSpanServerLight = Color(0xFF0E7490)
private val CanvasSpanServerDark = Color(0xFF22D3EE)
private val CanvasSpanClientLight = Color(0xFF7C3AED)
private val CanvasSpanClientDark = Color(0xFFA78BFA)
private val CanvasSpanProducerLight = Color(0xFF047857)
private val CanvasSpanProducerDark = Color(0xFF34D399)
private val CanvasSpanConsumerLight = Color(0xFFB45309)
private val CanvasSpanConsumerDark = Color(0xFFFBBF24)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val CanvasLightColorScheme = lightColorScheme(

    primary = Color(0xFF6366F1),              // electric indigo — the single catchy accent
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEEF2FF),
    onPrimaryContainer = Color(0xFF3730A3),

    secondary = Color(0xFF52525B),            // zinc gray — neutral metadata
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF4F4F5),
    onSecondaryContainer = Color(0xFF18181B),

    tertiary = Color(0xFF059669),             // emerald — success / active state
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFECFDF5),
    onTertiaryContainer = Color(0xFF065F46),

    background = Color(0xFFFAFAF8),           // warm off-white
    onBackground = Color(0xFF0A0A0A),

    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0A0A0A),

    surfaceVariant = Color(0xFFF5F4F1),       // warm light gray
    onSurfaceVariant = Color(0xFF52525B),

    outline = Color(0xFFE4E2DC),              // warm border
    outlineVariant = Color(0xFFD4D2CC),

    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEF2F2),
    onErrorContainer = Color(0xFF7F1D1D),

    inverseSurface = Color(0xFF1C1B1F),
    inverseOnSurface = Color(0xFFF5F4F1),
    inversePrimary = Color(0xFFA5B4FC),

    scrim = Color(0x66000000)
)

val CanvasDarkColorScheme = darkColorScheme(

    primary = Color(0xFFF5F5F5),
    onPrimary = Color(0xFF111111),

    secondary = Color(0xFFA1A1A1),
    onSecondary = Color(0xFF000000),

    tertiary = Color(0xFF1DB954),             // keep semantic consistency
    onTertiary = Color(0xFF000000),

    background = Color(0xFF0E0E0E),
    onBackground = Color(0xFFF5F5F5),

    surface = Color(0xFF161616),
    onSurface = Color(0xFFF5F5F5),

    surfaceVariant = Color(0xFF1F1F1F),
    onSurfaceVariant = Color(0xFFA1A1A1),

    outline = Color(0xFF2A2A2A),
    outlineVariant = Color(0xFF3A3A3A),

    error = Color(0xFFE5484D),
    onError = Color(0xFF000000),

    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),

    inverseSurface = Color(0xFFEDEDE9),
    inverseOnSurface = Color(0xFF111111),

    scrim = Color(0x99000000)
)

private val ColorScheme.isDarkPalette: Boolean
    get() = background == CanvasDarkColorScheme.background

val ColorScheme.success: Color
    get() = CanvasSuccessGreen

val ColorScheme.brand: Color
    get() = if (isDarkPalette) CanvasBrandIndigoDark else CanvasBrandIndigoLight

val ColorScheme.warning: Color
    get() = if (isDarkPalette) CanvasWarningDark else CanvasWarningLight

val ColorScheme.muted: Color
    get() = if (isDarkPalette) CanvasMutedDark else CanvasMutedLight

val ColorScheme.mutedContainer: Color
    get() = if (isDarkPalette) CanvasMutedContainerDark else CanvasMutedContainerLight

val ColorScheme.subtleSurface: Color
    get() = if (isDarkPalette) CanvasSubtleSurfaceDark else CanvasSubtleSurfaceLight

val ColorScheme.subtleSurfaceAlt: Color
    get() = if (isDarkPalette) CanvasSubtleSurfaceAltDark else CanvasSubtleSurfaceAltLight

val ColorScheme.panelBorder: Color
    get() = if (isDarkPalette) CanvasPanelBorderDark else CanvasPanelBorderLight

val ColorScheme.logVerbose: Color
    get() = if (isDarkPalette) CanvasLogVerboseDark else CanvasLogVerboseLight

val ColorScheme.logDebug: Color
    get() = if (isDarkPalette) CanvasLogDebugDark else CanvasLogDebugLight

val ColorScheme.logInfo: Color
    get() = if (isDarkPalette) CanvasLogInfoDark else CanvasLogInfoLight

val ColorScheme.logWarn: Color
    get() = if (isDarkPalette) CanvasLogWarnDark else CanvasLogWarnLight

val ColorScheme.logError: Color
    get() = if (isDarkPalette) CanvasLogErrorDark else CanvasLogErrorLight

val ColorScheme.logFatal: Color
    get() = if (isDarkPalette) CanvasLogFatalDark else CanvasLogFatalLight

val ColorScheme.spanInternal: Color
    get() = if (isDarkPalette) CanvasSpanInternalDark else CanvasSpanInternalLight

val ColorScheme.spanServer: Color
    get() = if (isDarkPalette) CanvasSpanServerDark else CanvasSpanServerLight

val ColorScheme.spanClient: Color
    get() = if (isDarkPalette) CanvasSpanClientDark else CanvasSpanClientLight

val ColorScheme.spanProducer: Color
    get() = if (isDarkPalette) CanvasSpanProducerDark else CanvasSpanProducerLight

val ColorScheme.spanConsumer: Color
    get() = if (isDarkPalette) CanvasSpanConsumerDark else CanvasSpanConsumerLight

val ColorScheme.querySuccessContainer: Color
    get() = success.copy(alpha = 0.12f)

val ColorScheme.queryErrorContainer: Color
    get() = error.copy(alpha = 0.12f)
