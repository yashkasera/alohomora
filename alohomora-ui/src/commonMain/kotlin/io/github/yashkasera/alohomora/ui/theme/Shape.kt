package io.github.yashkasera.alohomora.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * The console's corner scale, and the only one.
 *
 * `AppTheme` used to supply `colorScheme` and `typography` but not `shapes`, so every
 * `MaterialTheme.shapes.*` read — 60 of them — silently resolved to Material's stock defaults while a
 * second scale (`AlohomoraDimens.Corner`) covered ten more sites under different names for the same
 * radii. Two vocabularies for one property is how the two consoles drift apart.
 *
 * These values are Material's defaults **on purpose**. Supplying them changes nothing today, which is
 * the point: it moves the radii under this file's control without restyling 60 sites in the same
 * commit. `AlohomoraShapesTest` pins that equality, and is the one test to delete on the day the scale
 * deliberately diverges.
 */
internal val AlohomoraShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * A bottom sheet's shape, which the scale cannot express.
 *
 * [Shapes] roles are symmetric; a sheet rounds its top corners only. It lives here rather than in
 * `AlohomoraBottomSheetDefaults` so that the radius it borrows from [AlohomoraShapes] stays visibly
 * tied to the scale.
 */
internal val AlohomoraBottomSheetShape: Shape =
    RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)

/**
 * The permanent navigation drawer's shape, which the scale cannot express.
 *
 * A drawer flush to the window's left edge rounds its right corners only. Like
 * [AlohomoraBottomSheetShape] it borrows its radius from the `large` role of [AlohomoraShapes] so the
 * 16.dp stays visibly tied to the scale rather than floating as a literal at the call site.
 *
 * `public` rather than `internal` because the only consumer is the desktop `PermanentNavigationDrawer`
 * in a separate module, where an `internal` symbol would be invisible. Mobile never renders it.
 */
val AlohomoraDrawerShape: Shape =
    RoundedCornerShape(topStart = 0.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 0.dp)
