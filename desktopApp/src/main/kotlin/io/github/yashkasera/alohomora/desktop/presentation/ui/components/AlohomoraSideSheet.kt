package io.github.yashkasera.alohomora.desktop.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider

/**
 * The desktop's one right-hand detail sheet: scrim on the left, sliding surface on the right.
 *
 * Extracted from the bespoke block that shipped inside `TrafficPanel`, which is the version that
 * actually worked. It replaced an `AlohomoraSideDrawerOverlay` that was dead code carrying a real bug
 * (`2 * offsetX` doubled the animated travel, so "closed" parked the drawer two widths off-screen) and
 * put `detectDragGestures` on the whole content surface, where it fought any scrollable inside it —
 * fatal for a waterfall.
 *
 * Kept as one component because two sheets in the same window animating differently is the kind of
 * divergence users notice and nobody gets round to fixing.
 *
 * @param widthFraction fraction of the window the sheet occupies. A fraction rather than a fixed `Dp`
 *   so a larger monitor buys content width instead of empty margin — which matters for the waterfall,
 *   where horizontal space *is* time resolution.
 */
@Composable
fun AlohomoraSideSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    header: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    widthFraction: Float = 0.5f,
    content: @Composable ColumnScope.() -> Unit,
) {

    AnimatedVisibility(
        visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA))
                .clickable(
                    indication = null,
                    interactionSource = null,
                    onClick = onDismiss,
                ),
        )
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInHorizontally { it },
        exit = fadeOut() + slideOutHorizontally { it },
    ) {
        Row(
            modifier = modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.End,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(widthFraction)
                    .fillMaxHeight(),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    header()
                    AlohomoraHorizontalDivider()
                    content()
                }
            }
        }
    }
}

private const val SCRIM_ALPHA = 0.30f
