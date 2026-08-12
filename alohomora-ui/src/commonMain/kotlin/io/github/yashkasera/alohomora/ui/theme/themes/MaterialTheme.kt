package io.github.yashkasera.alohomora.ui.theme.themes

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import io.github.yashkasera.alohomora.ui.theme.AlohomoraColorTheme

val MaterialLightTheme = AlohomoraColorTheme(
    id = "material",
    displayName = "Material",
    isDark = false,
    materialColorScheme = lightColorScheme(),
    accent = Color(0xFF6366F1),
    success = Color(0xFF059669),
    successContainer = Color(0x1F059669),
    warning = Color(0xFFD97706),
    info = Color(0xFF1976D2),
    fatal = Color(0xFF9C27B0),
)

val MaterialDarkTheme = AlohomoraColorTheme(
    id = "material",
    displayName = "Material",
    isDark = true,
    materialColorScheme = darkColorScheme(),
    accent = Color(0xFF8394E2),
    success = Color(0xFF34D399),
    successContainer = Color(0x1F34D399),
    warning = Color(0xFFFFC857),
    info = Color(0xFF64B5F6),
    fatal = Color(0xFFCE93D8),
)
