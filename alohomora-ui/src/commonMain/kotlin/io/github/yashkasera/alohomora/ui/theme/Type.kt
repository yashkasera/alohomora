package io.github.yashkasera.alohomora.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.yashkasera.alohomora_ui.generated.resources.InstrumentSerif_Regular
import io.github.yashkasera.alohomora_ui.generated.resources.JetBrainsMono_Regular
import io.github.yashkasera.alohomora_ui.generated.resources.Newsreader_Regular
import io.github.yashkasera.alohomora_ui.generated.resources.Res
import org.jetbrains.compose.resources.Font

@Composable
private fun InstrumentalSerifFontFamily() = FontFamily(
    Font(Res.font.InstrumentSerif_Regular, weight = FontWeight.Normal),
)

@Composable
private fun NewsreaderFontFamily() = FontFamily(
    Font(Res.font.Newsreader_Regular, weight = FontWeight.Normal),
)

@Composable
private fun JetbrainsMonoFontFamily() = FontFamily(
    Font(Res.font.JetBrainsMono_Regular, weight = FontWeight.Normal),
)

/**
 * Binds a style to a family and to the one weight this console ships.
 *
 * The weight pin is the load-bearing half. Only `*-Regular` faces are bundled, and Material's own
 * scale asks for `FontWeight.Medium` on `labelLarge`, `labelMedium`, `labelSmall`, `titleMedium` and
 * `titleSmall` — the styles used on every chip, button label and list row. A weight with no face
 * behind it is *synthesised*: Skia widens the strokes algorithmically, and it does so differently on
 * Android, iOS and Desktop. The scale was therefore requesting a fake weight almost everywhere,
 * before a single call site had overridden anything.
 *
 * Pinning to [FontWeight.Normal] means every glyph on screen comes from a real bundled face.
 * Hierarchy is carried by size and colour instead — the `secondary`/`onSurfaceVariant` roles and the
 * serif display face — which is the trade this console makes deliberately.
 */
private fun TextStyle.inFamily(family: FontFamily): TextStyle =
    copy(fontFamily = family, fontWeight = FontWeight.Normal)

@Composable
internal fun AlohomoraTypography() = Typography().run {
    val serif = InstrumentalSerifFontFamily()
    val newsreader = NewsreaderFontFamily()
    val mono = JetbrainsMonoFontFamily()
    copy(
        displayLarge = displayLarge.inFamily(serif),
        displayMedium = displayMedium.inFamily(serif),
        displaySmall = displaySmall.inFamily(serif),
        headlineLarge = headlineLarge.inFamily(serif),
        headlineMedium = headlineMedium.inFamily(serif),
        headlineSmall = headlineSmall.inFamily(serif),
        titleLarge = titleLarge.inFamily(newsreader),
        titleMedium = titleMedium.inFamily(newsreader),
        titleSmall = titleSmall.inFamily(newsreader),
        bodyLarge = bodyLarge.inFamily(mono),
        bodyMedium = bodyMedium.inFamily(mono),
        bodySmall = bodySmall.inFamily(mono),
        // labelLarge carries the primary button label, and it is the one label style Material tracks
        // at 0.1sp while its siblings get 0.5sp. Uppercased and stripped of its faux-Medium weight it
        // reads cramped, so it joins the others rather than inventing a new number.
        labelLarge = labelLarge.inFamily(mono).copy(letterSpacing = 0.5.sp),
        labelMedium = labelMedium.inFamily(mono),
        labelSmall = labelSmall.inFamily(mono),
        // The Emphasized roles are pinned too, and that is the point rather than an oversight.
        //
        // They default to Medium and Bold, so leaving them alone would keep exactly the synthetic
        // rendering this scale exists to remove. Worse, the 15-argument `copy` overload silently
        // skips them, which is what used to leave them on the *system* font family — a trap armed for
        // whoever first reached for `labelSmallEmphasized`. Passing all thirty selects the overload
        // that covers them.
        //
        // The consequence is that an Emphasized style currently renders identically to its base. That
        // is honest: the console has one weight. Reach for a larger size or a stronger colour role.
        displayLargeEmphasized = displayLargeEmphasized.inFamily(serif),
        displayMediumEmphasized = displayMediumEmphasized.inFamily(serif),
        displaySmallEmphasized = displaySmallEmphasized.inFamily(serif),
        headlineLargeEmphasized = headlineLargeEmphasized.inFamily(serif),
        headlineMediumEmphasized = headlineMediumEmphasized.inFamily(serif),
        headlineSmallEmphasized = headlineSmallEmphasized.inFamily(serif),
        titleLargeEmphasized = titleLargeEmphasized.inFamily(newsreader),
        titleMediumEmphasized = titleMediumEmphasized.inFamily(newsreader),
        titleSmallEmphasized = titleSmallEmphasized.inFamily(newsreader),
        bodyLargeEmphasized = bodyLargeEmphasized.inFamily(mono),
        bodyMediumEmphasized = bodyMediumEmphasized.inFamily(mono),
        bodySmallEmphasized = bodySmallEmphasized.inFamily(mono),
        labelLargeEmphasized = labelLargeEmphasized.inFamily(mono),
        labelMediumEmphasized = labelMediumEmphasized.inFamily(mono),
        labelSmallEmphasized = labelSmallEmphasized.inFamily(mono),
    )
}
