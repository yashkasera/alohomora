package io.github.yashkasera.alohomora.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class AlohomoraDimens(
    val stroke: Stroke = Stroke(),
    val margin: Margin = Margin(),
    val icon: Icon = Icon(),
    val corner: Corner = Corner(),
) {
    @Immutable
    data class Stroke(
        val thin: Dp = 0.5.dp,   // subtle dividers, list-item separators
        val small: Dp = 1.dp,    // standard borders, field outlines
        val medium: Dp = 2.dp,   // emphasis borders (e.g. EmptyState icon ring)
    )

    @Immutable
    data class Margin(
        val xs: Dp = 4.dp,       // chip/badge internal gap, label bottom gap
        val sm: Dp = 8.dp,       // tight row gap, icon-label spacing
        val md: Dp = 12.dp,      // compact section gap
        val lg: Dp = 16.dp,      // standard card/list-item padding
        val xl: Dp = 20.dp,      // screen horizontal edge padding
        val xxl: Dp = 24.dp,     // section container padding
        val xxxl: Dp = 32.dp,    // section separator, empty-state outer padding
        val huge: Dp = 48.dp,    // tall section spacers
    )

    @Immutable
    data class Icon(
        val xs: Dp = 12.dp,            // tiny dot/indicator
        val sm: Dp = 14.dp,            // metadata row icons (clock, hdd)
        val md: Dp = 16.dp,            // small action icons, search field icon
        val lg: Dp = 20.dp,            // standard trailing/leading icons
        val standard: Dp = 24.dp,      // primary nav / toolbar icons
        val xl: Dp = 36.dp,            // empty-state icon glyph
        val illustration: Dp = 80.dp,  // empty-state icon container
    )

    @Immutable
    data class Corner(
        val small: Dp = 4.dp,   // badge chips, small cards
        val medium: Dp = 8.dp,  // medium cards, dropdowns
        val full: Dp = 50.dp,   // pill / full-round filter chips
    )
}

val LocalAlohomoraDimens = staticCompositionLocalOf { AlohomoraDimens() }

val MaterialTheme.dimens: AlohomoraDimens
    @Composable
    @ReadOnlyComposable
    get() = LocalAlohomoraDimens.current
