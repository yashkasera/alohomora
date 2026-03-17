package io.github.yashkasera.alohomora.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val CanvasBlack = Color(0xFF000000)
val CanvasWhite = Color(0xFFFFFFFF)
val CanvasDarkGray = Color(0xFF333333) // For borders/accents
val CanvasLightGray = Color(0xFFF5F5F5) // For surfaces
val CanvasError = Color(0xFF000000)
val CanvasAlertRed = Color(0xFFD00000)
val CanvasSuccessGreen = Color(0xFF059669)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val CanvasLightColorScheme = lightColorScheme(
    primary = CanvasBlack,
    onPrimary = CanvasWhite,
    primaryContainer = CanvasLightGray,
    onPrimaryContainer = CanvasBlack,
    secondary = CanvasBlack,
    onSecondary = CanvasWhite,
    secondaryContainer = CanvasLightGray,
    onSecondaryContainer = CanvasBlack,
    tertiary = CanvasDarkGray,
    onTertiary = CanvasWhite,
    tertiaryContainer = CanvasLightGray,
    onTertiaryContainer = CanvasBlack,
    error = CanvasAlertRed,
    onError = CanvasWhite,
    background = CanvasWhite,
    onBackground = CanvasBlack,
    surface = CanvasWhite,
    onSurface = CanvasBlack,
    surfaceVariant = CanvasLightGray,
    onSurfaceVariant = CanvasBlack,
    outline = CanvasBlack,
)

val CanvasDarkColorScheme = darkColorScheme(
    primary = CanvasWhite,
    onPrimary = CanvasBlack,
    primaryContainer = CanvasDarkGray,
    onPrimaryContainer = CanvasWhite,
    secondary = CanvasWhite,
    onSecondary = CanvasBlack,
    secondaryContainer = CanvasDarkGray,
    onSecondaryContainer = CanvasWhite,
    tertiary = CanvasDarkGray,
    onTertiary = CanvasBlack,
    tertiaryContainer = CanvasDarkGray,
    onTertiaryContainer = CanvasWhite,
    error = CanvasAlertRed,
    onError = CanvasBlack,
    background = CanvasBlack,
    onBackground = CanvasWhite,
    surface = CanvasBlack,
    onSurface = CanvasWhite,
    surfaceVariant = CanvasDarkGray,
    onSurfaceVariant = CanvasWhite,
    outline = CanvasWhite
)

val ColorScheme.success: Color
    get() = CanvasSuccessGreen
