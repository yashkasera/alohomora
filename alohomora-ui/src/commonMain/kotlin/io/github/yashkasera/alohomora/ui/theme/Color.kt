package io.github.yashkasera.alohomora.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

val CanvasSuccessGreen = Color(0xFF059669)

private val CanvasBrandIndigoLight = Color(0xFF6366F1)
private val CanvasBrandIndigoDark = Color(0xFFA5B4FC)
private val CanvasWarningLight = Color(0xFFD97706)
private val CanvasWarningDark = Color(0xFFFFC857)
private val CanvasMutedLight = Color(0xFF71717A)
private val CanvasMutedDark = Color(0xFFB0B0B0)
private val CanvasMutedContainerLight = Color(0xFFE4E4E7)
private val CanvasMutedContainerDark = Color(0xFF2D2D2D)
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

private val ColorScheme.isDarkPalette: Boolean
    @ReadOnlyComposable
    @Composable
    get() = LocalThemeIsDark.current.value

val ColorScheme.success: Color

    @ReadOnlyComposable
    @Composable
    get() = CanvasSuccessGreen

val ColorScheme.brand: Color
    @ReadOnlyComposable
    @Composable
    get() = if (isDarkPalette) CanvasBrandIndigoDark else CanvasBrandIndigoLight

val ColorScheme.warning: Color

    @ReadOnlyComposable
    @Composable
    get() = if (isDarkPalette) CanvasWarningDark else CanvasWarningLight

val ColorScheme.muted: Color

    @ReadOnlyComposable
    @Composable
    get() = if (isDarkPalette) CanvasMutedDark else CanvasMutedLight

val ColorScheme.mutedContainer: Color

    @ReadOnlyComposable
    @Composable
    get() = if (isDarkPalette) CanvasMutedContainerDark else CanvasMutedContainerLight

val ColorScheme.subtleSurfaceAlt: Color

    @ReadOnlyComposable
    @Composable
    get() = if (isDarkPalette) CanvasSubtleSurfaceAltDark else CanvasSubtleSurfaceAltLight

val ColorScheme.panelBorder: Color

    @ReadOnlyComposable
    @Composable
    get() = if (isDarkPalette) CanvasPanelBorderDark else CanvasPanelBorderLight

val ColorScheme.logVerbose: Color

    @ReadOnlyComposable
    @Composable
    get() = if (isDarkPalette) CanvasLogVerboseDark else CanvasLogVerboseLight

val ColorScheme.logDebug: Color

    @ReadOnlyComposable
    @Composable
    get() = if (isDarkPalette) CanvasLogDebugDark else CanvasLogDebugLight

val ColorScheme.logInfo: Color

    @ReadOnlyComposable
    @Composable
    get() = if (isDarkPalette) CanvasLogInfoDark else CanvasLogInfoLight

val ColorScheme.logWarn: Color

    @ReadOnlyComposable
    @Composable
    get() = if (isDarkPalette) CanvasLogWarnDark else CanvasLogWarnLight

val ColorScheme.logError: Color

    @ReadOnlyComposable
    @Composable
    get() = if (isDarkPalette) CanvasLogErrorDark else CanvasLogErrorLight

val ColorScheme.logFatal: Color

    @ReadOnlyComposable
    @Composable
    get() = if (isDarkPalette) CanvasLogFatalDark else CanvasLogFatalLight

val ColorScheme.spanInternal: Color

    @ReadOnlyComposable
    @Composable
    get() = if (isDarkPalette) CanvasSpanInternalDark else CanvasSpanInternalLight

val ColorScheme.spanServer: Color

    @ReadOnlyComposable
    @Composable
    get() = if (isDarkPalette) CanvasSpanServerDark else CanvasSpanServerLight

val ColorScheme.spanClient: Color

    @ReadOnlyComposable
    @Composable
    get() = if (isDarkPalette) CanvasSpanClientDark else CanvasSpanClientLight

val ColorScheme.spanProducer: Color

    @ReadOnlyComposable
    @Composable
    get() = if (isDarkPalette) CanvasSpanProducerDark else CanvasSpanProducerLight

val ColorScheme.spanConsumer: Color

    @ReadOnlyComposable
    @Composable
    get() = if (isDarkPalette) CanvasSpanConsumerDark else CanvasSpanConsumerLight

val ColorScheme.querySuccessContainer: Color

    @ReadOnlyComposable
    @Composable
    get() = success.copy(alpha = 0.12f)

val ColorScheme.queryErrorContainer: Color

    @ReadOnlyComposable
    @Composable
    get() = error.copy(alpha = 0.12f)

// Mobile theme palette — used by AppTheme's lightScheme / darkScheme in Theme.kt.
// Kept private: no external consumer should reference these directly.

internal val primaryLight = Color(0xFF000000)
internal val onPrimaryLight = Color(0xFFFFFFFF)
internal val primaryContainerLight = Color(0xFF1B1B1B)
internal val onPrimaryContainerLight = Color(0xFF848484)
internal val secondaryLight = Color(0xFF5E5E5E)
internal val onSecondaryLight = Color(0xFFFFFFFF)
internal val secondaryContainerLight = Color(0xFFE2E2E2)
internal val onSecondaryContainerLight = Color(0xFF646464)
internal val tertiaryLight = Color(0xFF000000)
internal val onTertiaryLight = Color(0xFFFFFFFF)
internal val tertiaryContainerLight = Color(0xFF1B1B1B)
internal val onTertiaryContainerLight = Color(0xFF848484)
internal val errorLight = Color(0xFFE53935)
internal val onErrorLight = Color(0xFFFFFFFF)
internal val errorContainerLight = Color(0xFFFFDAD6)
internal val onErrorContainerLight = Color(0xFF93000A)
internal val backgroundLight = Color(0xFFF9F9F9)
internal val onBackgroundLight = Color(0xFF1B1B1B)
internal val surfaceLight = Color(0xFFF9F9F9)
internal val onSurfaceLight = Color(0xFF1B1B1B)
internal val surfaceVariantLight = Color(0xFFEBE0E1)
internal val onSurfaceVariantLight = Color(0xFF4C4546)
internal val outlineLight = Color(0xFF7E7576)
internal val outlineVariantLight = Color(0xFFCFC4C5)
internal val scrimLight = Color(0xFF000000)
internal val inverseSurfaceLight = Color(0xFF303030)
internal val inverseOnSurfaceLight = Color(0xFFF1F1F1)
internal val inversePrimaryLight = Color(0xFFC6C6C6)
internal val surfaceDimLight = Color(0xFFDADADA)
internal val surfaceBrightLight = Color(0xFFF9F9F9)
internal val surfaceContainerLowestLight = Color(0xFFFFFFFF)
internal val surfaceContainerLowLight = Color(0xFFF3F3F3)
internal val surfaceContainerLight = Color(0xFFEEEEEE)
internal val surfaceContainerHighLight = Color(0xFFE8E8E8)
internal val surfaceContainerHighestLight = Color(0xFFE2E2E2)

internal val primaryDark = Color(0xFFE0E0E0)
internal val onPrimaryDark = Color(0xFF303030)
internal val primaryContainerDark = Color(0xFF121212)
internal val onPrimaryContainerDark = Color(0xFF757575)
internal val secondaryDark = Color(0xFFB0B0B0)
internal val onSecondaryDark = Color(0xFF303030)
internal val secondaryContainerDark = Color(0xFF1E1E1E)
internal val onSecondaryContainerDark = Color(0xFFB5B5B5)
internal val tertiaryDark = Color(0xFF6E6E6E)
internal val onTertiaryDark = Color(0xFF303030)
internal val tertiaryContainerDark = Color(0xFF000000)
internal val onTertiaryContainerDark = Color(0xFF757575)
internal val errorDark = Color(0xFFFFB4AB)
internal val onErrorDark = Color(0xFF690005)
internal val errorContainerDark = Color(0xFF410002)
internal val onErrorContainerDark = Color(0xFFFFDAD6)
internal val backgroundDark = Color(0xFF131313)
internal val onBackgroundDark = Color(0xFFE2E2E2)
internal val surfaceDark = Color(0xFF131313)
internal val onSurfaceDark = Color(0xFFE2E2E2)
internal val surfaceVariantDark = Color(0xFF4C4546)
internal val onSurfaceVariantDark = Color(0xFFCFC4C5)
internal val outlineDark = Color(0xFF888888)
internal val outlineVariantDark = Color(0xFF4C4546)
internal val scrimDark = Color(0xFF000000)
internal val inverseSurfaceDark = Color(0xFFE2E2E2)
internal val inverseOnSurfaceDark = Color(0xFF303030)
internal val inversePrimaryDark = Color(0xFF5E5E5E)
internal val surfaceDimDark = Color(0xFF131313)
internal val surfaceBrightDark = Color(0xFF393939)
internal val surfaceContainerLowestDark = Color(0xFF0E0E0E)
internal val surfaceContainerLowDark = Color(0xFF1B1B1B)
internal val surfaceContainerDark = Color(0xFF1F1F1F)
internal val surfaceContainerHighDark = Color(0xFF2A2A2A)
internal val surfaceContainerHighestDark = Color(0xFF353535)
