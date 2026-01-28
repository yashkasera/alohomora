package io.github.yashkasera.alohomora.presentation.theme

import io.github.yashkasera.alohomora.generated.resources.InstrumentSerif_Regular
import io.github.yashkasera.alohomora.generated.resources.Inter_Regular
import io.github.yashkasera.alohomora.generated.resources.JetBrainsMono_Regular
import io.github.yashkasera.alohomora.generated.resources.Newsreader_Regular
import io.github.yashkasera.alohomora.generated.resources.Res
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font

@Composable
private fun InstrumentalSerifFontFamily() = FontFamily(
    Font(Res.font.InstrumentSerif_Regular, weight = FontWeight.Normal),
)

@Composable
private fun InterFontFamily() = FontFamily(
    Font(Res.font.Inter_Regular, weight = FontWeight.Normal),
)

@Composable
private fun NewsreaderFontFamily() = FontFamily(
    Font(Res.font.Newsreader_Regular, weight = FontWeight.Normal),
)

@Composable
private fun JetbrainsMonoFontFamily() = FontFamily(
    Font(Res.font.JetBrainsMono_Regular, weight = FontWeight.Normal),
)


@Composable
internal fun AlohomoraTypography() = Typography().run {
    val instrumentalSerifFontFamily = InstrumentalSerifFontFamily()
    val interFontFamily = InterFontFamily()
    val newsreaderFontFamily = NewsreaderFontFamily()
    val jetbrainsMonoFontFamily = JetbrainsMonoFontFamily()
    copy(
        displayLarge = displayLarge.copy(fontFamily = instrumentalSerifFontFamily),
        displayMedium = displayMedium.copy(fontFamily = instrumentalSerifFontFamily),
        displaySmall = displaySmall.copy(fontFamily = instrumentalSerifFontFamily),
        headlineLarge = headlineLarge.copy(fontFamily = instrumentalSerifFontFamily),
        headlineMedium = headlineMedium.copy(fontFamily = instrumentalSerifFontFamily),
        headlineSmall = headlineSmall.copy(fontFamily = instrumentalSerifFontFamily),
        titleLarge = titleLarge.copy(fontFamily = newsreaderFontFamily),
        titleMedium = titleMedium.copy(fontFamily = newsreaderFontFamily),
        titleSmall = titleSmall.copy(fontFamily = newsreaderFontFamily),
        bodyLarge = bodyLarge.copy(fontFamily = jetbrainsMonoFontFamily),
        bodyMedium = bodyMedium.copy(fontFamily = jetbrainsMonoFontFamily),
        bodySmall = bodySmall.copy(fontFamily = jetbrainsMonoFontFamily),
        labelLarge = labelLarge.copy(fontFamily = interFontFamily),
        labelMedium = labelMedium.copy(fontFamily = interFontFamily),
        labelSmall = labelSmall.copy(fontFamily = jetbrainsMonoFontFamily)
    )
}
