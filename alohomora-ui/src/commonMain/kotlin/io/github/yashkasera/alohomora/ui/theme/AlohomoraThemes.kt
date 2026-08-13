package io.github.yashkasera.alohomora.ui.theme

import io.github.yashkasera.alohomora.ui.theme.themes.MonochromeDarkTheme
import io.github.yashkasera.alohomora.ui.theme.themes.MonochromeLightTheme
import io.github.yashkasera.alohomora.ui.theme.themes.DraculaDarkTheme
import io.github.yashkasera.alohomora.ui.theme.themes.DraculaLightTheme
import io.github.yashkasera.alohomora.ui.theme.themes.MaterialDarkTheme
import io.github.yashkasera.alohomora.ui.theme.themes.MaterialLightTheme
import io.github.yashkasera.alohomora.ui.theme.themes.NordDarkTheme
import io.github.yashkasera.alohomora.ui.theme.themes.NordLightTheme
import io.github.yashkasera.alohomora.ui.theme.themes.SolarizedDarkTheme
import io.github.yashkasera.alohomora.ui.theme.themes.SolarizedLightTheme

object AlohomoraThemes {

    private val registry: Map<String, Pair<AlohomoraColorTheme, AlohomoraColorTheme>> = mapOf(
        "material" to (MaterialLightTheme to MaterialDarkTheme),
        "monochrome" to (MonochromeLightTheme to MonochromeDarkTheme),
        "dracula" to (DraculaLightTheme to DraculaDarkTheme),
        "nord" to (NordLightTheme to NordDarkTheme),
        "solarized" to (SolarizedLightTheme to SolarizedDarkTheme),
    )

    val ids: List<String> = registry.keys.toList()

    val all: List<AlohomoraColorTheme> =
        registry.values.flatMap { (light, dark) -> listOf(light, dark) }

    fun forId(id: String, isDark: Boolean): AlohomoraColorTheme {
        val (light, dark) = registry[id] ?: registry.getValue("material")
        return if (isDark) dark else light
    }

    fun lightPreview(id: String): AlohomoraColorTheme =
        registry[id]?.first ?: MaterialLightTheme

    fun darkPreview(id: String): AlohomoraColorTheme =
        registry[id]?.second ?: MaterialDarkTheme
}
