package io.github.yashkasera.alohomora.desktop.presentation.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * The desktop console's one motion vocabulary.
 *
 * Every accessor delegates to `MaterialTheme.motionScheme` (wired to [MotionScheme.expressive] in
 * `AppTheme`), so tuning lives in exactly one place and the whole app moves alike. The split is the
 * Material one, and it is load-bearing:
 *
 * - **Spatial** specs are springs and *overshoot* — correct for anything that moves, resizes or morphs
 *   (a sheet sliding in, a corner radius settling). Reach for these when a pixel changes position.
 * - **Effects** specs are flat, no overshoot — correct for colour, alpha and elevation, which must not
 *   bounce. A scrim that overshot its alpha would flicker past opaque.
 *
 * Accessors are `@Composable @ReadOnlyComposable` because the scheme comes from a composition local;
 * they cannot be eager `val`s. This mirrors how `MaterialTheme.dimens`/`.shapes` are read.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object AlohomoraMotion {

    /** Side-sheet surface sliding in. Spatial spring — the expressive settle. */
    val sheetEnter: FiniteAnimationSpec<IntOffset>
        @Composable @ReadOnlyComposable get() = MaterialTheme.motionScheme.defaultSpatialSpec()

    /**
     * Side-sheet surface sliding out. Deliberately the *fast* spec so the exit is shorter than the
     * enter: the host pops its dismiss-stack entry synchronously, and a snappier exit keeps the window
     * where the entry is gone but the surface still animates as tight as possible.
     */
    val sheetExit: FiniteAnimationSpec<IntOffset>
        @Composable @ReadOnlyComposable get() = MaterialTheme.motionScheme.fastSpatialSpec()

    /** Scrim (and any pure fade). Effects spec — alpha never overshoots. */
    val scrimFade: FiniteAnimationSpec<Float>
        @Composable @ReadOnlyComposable get() = MaterialTheme.motionScheme.defaultEffectsSpec()

    /** Section panel content sliding in on a nav change. */
    val sectionEnter: FiniteAnimationSpec<IntOffset>
        @Composable @ReadOnlyComposable get() = MaterialTheme.motionScheme.defaultSpatialSpec()

    /** Outgoing section panel — fast, so the incoming one leads. */
    val sectionExit: FiniteAnimationSpec<IntOffset>
        @Composable @ReadOnlyComposable get() = MaterialTheme.motionScheme.fastSpatialSpec()

    /** Expand/collapse of badges, banners and disclosure rows. */
    val expandCollapse: FiniteAnimationSpec<IntSize>
        @Composable @ReadOnlyComposable get() = MaterialTheme.motionScheme.fastSpatialSpec()

    /** Corner-radius morph on sheets and selected cards. Spatial — it is a size that settles. */
    val shapeMorph: FiniteAnimationSpec<Dp>
        @Composable @ReadOnlyComposable get() = MaterialTheme.motionScheme.defaultSpatialSpec()

    /**
     * Scale/alpha on nav badges and selection indicators. A hand-tuned bounce rather than a scheme
     * accessor because the shortcut badges want a touch more overshoot than the default spatial spring
     * gives — the full-expressive pop the user asked for.
     */
    val selectionIndicator: FiniteAnimationSpec<Float>
        get() = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)
}
