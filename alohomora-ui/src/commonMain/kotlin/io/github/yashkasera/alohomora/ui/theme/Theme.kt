package io.github.yashkasera.alohomora.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import io.github.yashkasera.alohomora.ui.backgroundDark
import io.github.yashkasera.alohomora.ui.backgroundLight
import io.github.yashkasera.alohomora.ui.errorContainerDark
import io.github.yashkasera.alohomora.ui.errorContainerLight
import io.github.yashkasera.alohomora.ui.errorDark
import io.github.yashkasera.alohomora.ui.errorLight
import io.github.yashkasera.alohomora.ui.inverseOnSurfaceDark
import io.github.yashkasera.alohomora.ui.inverseOnSurfaceLight
import io.github.yashkasera.alohomora.ui.inversePrimaryDark
import io.github.yashkasera.alohomora.ui.inversePrimaryLight
import io.github.yashkasera.alohomora.ui.inverseSurfaceDark
import io.github.yashkasera.alohomora.ui.inverseSurfaceLight
import io.github.yashkasera.alohomora.ui.onBackgroundDark
import io.github.yashkasera.alohomora.ui.onBackgroundLight
import io.github.yashkasera.alohomora.ui.onErrorContainerDark
import io.github.yashkasera.alohomora.ui.onErrorContainerLight
import io.github.yashkasera.alohomora.ui.onErrorDark
import io.github.yashkasera.alohomora.ui.onErrorLight
import io.github.yashkasera.alohomora.ui.onPrimaryContainerDark
import io.github.yashkasera.alohomora.ui.onPrimaryContainerLight
import io.github.yashkasera.alohomora.ui.onPrimaryDark
import io.github.yashkasera.alohomora.ui.onPrimaryLight
import io.github.yashkasera.alohomora.ui.onSecondaryContainerDark
import io.github.yashkasera.alohomora.ui.onSecondaryContainerLight
import io.github.yashkasera.alohomora.ui.onSecondaryDark
import io.github.yashkasera.alohomora.ui.onSecondaryLight
import io.github.yashkasera.alohomora.ui.onSurfaceDark
import io.github.yashkasera.alohomora.ui.onSurfaceLight
import io.github.yashkasera.alohomora.ui.onSurfaceVariantDark
import io.github.yashkasera.alohomora.ui.onSurfaceVariantLight
import io.github.yashkasera.alohomora.ui.onTertiaryContainerDark
import io.github.yashkasera.alohomora.ui.onTertiaryContainerLight
import io.github.yashkasera.alohomora.ui.onTertiaryDark
import io.github.yashkasera.alohomora.ui.onTertiaryLight
import io.github.yashkasera.alohomora.ui.outlineDark
import io.github.yashkasera.alohomora.ui.outlineLight
import io.github.yashkasera.alohomora.ui.outlineVariantDark
import io.github.yashkasera.alohomora.ui.outlineVariantLight
import io.github.yashkasera.alohomora.ui.primaryContainerDark
import io.github.yashkasera.alohomora.ui.primaryContainerLight
import io.github.yashkasera.alohomora.ui.primaryDark
import io.github.yashkasera.alohomora.ui.primaryLight
import io.github.yashkasera.alohomora.ui.scrimDark
import io.github.yashkasera.alohomora.ui.scrimLight
import io.github.yashkasera.alohomora.ui.secondaryContainerDark
import io.github.yashkasera.alohomora.ui.secondaryContainerLight
import io.github.yashkasera.alohomora.ui.secondaryDark
import io.github.yashkasera.alohomora.ui.secondaryLight
import io.github.yashkasera.alohomora.ui.surfaceBrightDark
import io.github.yashkasera.alohomora.ui.surfaceBrightLight
import io.github.yashkasera.alohomora.ui.surfaceContainerDark
import io.github.yashkasera.alohomora.ui.surfaceContainerHighDark
import io.github.yashkasera.alohomora.ui.surfaceContainerHighLight
import io.github.yashkasera.alohomora.ui.surfaceContainerHighestDark
import io.github.yashkasera.alohomora.ui.surfaceContainerHighestLight
import io.github.yashkasera.alohomora.ui.surfaceContainerLight
import io.github.yashkasera.alohomora.ui.surfaceContainerLowDark
import io.github.yashkasera.alohomora.ui.surfaceContainerLowLight
import io.github.yashkasera.alohomora.ui.surfaceContainerLowestDark
import io.github.yashkasera.alohomora.ui.surfaceContainerLowestLight
import io.github.yashkasera.alohomora.ui.surfaceDark
import io.github.yashkasera.alohomora.ui.surfaceDimDark
import io.github.yashkasera.alohomora.ui.surfaceDimLight
import io.github.yashkasera.alohomora.ui.surfaceLight
import io.github.yashkasera.alohomora.ui.surfaceVariantDark
import io.github.yashkasera.alohomora.ui.surfaceVariantLight
import io.github.yashkasera.alohomora.ui.tertiaryContainerDark
import io.github.yashkasera.alohomora.ui.tertiaryContainerLight
import io.github.yashkasera.alohomora.ui.tertiaryDark
import io.github.yashkasera.alohomora.ui.tertiaryLight

private val lightScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
)

private val darkScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
)

@Immutable
data class ColorFamily(
    val color: Color,
    val onColor: Color,
    val colorContainer: Color,
    val onColorContainer: Color,
)

val unspecified_scheme = ColorFamily(
    Color.Unspecified, Color.Unspecified, Color.Unspecified, Color.Unspecified,
)

@Composable
fun AppTheme(
    systemIsDark: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    content: @Composable() () -> Unit,
) {
    val isDarkState = remember(systemIsDark) { mutableStateOf(false) }
    CompositionLocalProvider(
        LocalThemeIsDark provides isDarkState,
    ) {
        val isDark by isDarkState


        MaterialTheme(
            colorScheme = if (isDark) darkColorScheme() else lightColorScheme(),
            typography = AlohomoraTypography(),
            content = content,
        )
    }
}

