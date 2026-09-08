package io.github.yashkasera.alohomora.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.yashkasera.alohomora_ui.generated.resources.InstrumentSerif_Regular
import io.github.yashkasera.alohomora_ui.generated.resources.JetBrainsMono_Medium
import io.github.yashkasera.alohomora_ui.generated.resources.JetBrainsMono_Regular
import io.github.yashkasera.alohomora_ui.generated.resources.Newsreader_Medium
import io.github.yashkasera.alohomora_ui.generated.resources.Newsreader_Regular
import io.github.yashkasera.alohomora_ui.generated.resources.Res
import org.jetbrains.compose.resources.Font

@Composable
private fun InstrumentalSerifFontFamily() = FontFamily(
    Font(Res.font.InstrumentSerif_Regular, weight = FontWeight.Normal),
)

// Newsreader and JetBrains Mono now ship a real Medium face alongside Regular, so the Emphasized type
// roles can carry genuine weight instead of a Skia-synthesised fake. Instrument Serif has no bold face
// at all, so it stays Regular-only — its emphasis is size and colour, as before.
@Composable
private fun NewsreaderFontFamily() = FontFamily(
    Font(Res.font.Newsreader_Regular, weight = FontWeight.Normal),
    Font(Res.font.Newsreader_Medium, weight = FontWeight.Medium),
)

@Composable
private fun JetbrainsMonoFontFamily() = FontFamily(
    Font(Res.font.JetBrainsMono_Regular, weight = FontWeight.Normal),
    Font(Res.font.JetBrainsMono_Medium, weight = FontWeight.Medium),
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

/**
 * Binds a style to a family at [FontWeight.Medium] — for the Emphasized roles only.
 *
 * Use this exclusively with Newsreader and JetBrains Mono, which now bundle a real Medium face. Called
 * with Instrument Serif it would reintroduce exactly the synthetic widening [inFamily] exists to
 * avoid, since the serif ships a single Regular face; the display/headline Emphasized roles therefore
 * keep using [inFamily]. The base (non-Emphasized) roles stay on [inFamily] too, so the console's
 * everyday weight is unchanged — emphasis is opt-in, reached only through a `*Emphasized` style.
 */
private fun TextStyle.inFamilyEmphasized(family: FontFamily): TextStyle =
    copy(fontFamily = family, fontWeight = FontWeight.Medium)

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
        // The Emphasized roles are where this console now carries real weight.
        //
        // All thirty arguments are still passed on purpose: the 15-argument `copy` overload silently
        // skips the Emphasized styles, which is what used to strand them on the *system* font family.
        // Passing every one selects the overload that covers them.
        //
        // Newsreader (title) and JetBrains Mono (body/label) bundle a genuine Medium face, so their
        // Emphasized roles step up to `FontWeight.Medium` via [inFamilyEmphasized] — a real face, not
        // a Skia-synthesised widen. Instrument Serif (display/headline) has no bold face, so those keep
        // [inFamily] at Normal and lean on size and colour, exactly as before. The base roles are
        // untouched, so an Emphasized style is now a deliberate step up rather than a synonym.
        displayLargeEmphasized = displayLargeEmphasized.inFamily(serif),
        displayMediumEmphasized = displayMediumEmphasized.inFamily(serif),
        displaySmallEmphasized = displaySmallEmphasized.inFamily(serif),
        headlineLargeEmphasized = headlineLargeEmphasized.inFamily(serif),
        headlineMediumEmphasized = headlineMediumEmphasized.inFamily(serif),
        headlineSmallEmphasized = headlineSmallEmphasized.inFamily(serif),
        titleLargeEmphasized = titleLargeEmphasized.inFamilyEmphasized(newsreader),
        titleMediumEmphasized = titleMediumEmphasized.inFamilyEmphasized(newsreader),
        titleSmallEmphasized = titleSmallEmphasized.inFamilyEmphasized(newsreader),
        bodyLargeEmphasized = bodyLargeEmphasized.inFamilyEmphasized(mono),
        bodyMediumEmphasized = bodyMediumEmphasized.inFamilyEmphasized(mono),
        bodySmallEmphasized = bodySmallEmphasized.inFamilyEmphasized(mono),
        labelLargeEmphasized = labelLargeEmphasized.inFamilyEmphasized(mono).copy(letterSpacing = 0.5.sp),
        labelMediumEmphasized = labelMediumEmphasized.inFamilyEmphasized(mono),
        labelSmallEmphasized = labelSmallEmphasized.inFamilyEmphasized(mono),
    )
}
