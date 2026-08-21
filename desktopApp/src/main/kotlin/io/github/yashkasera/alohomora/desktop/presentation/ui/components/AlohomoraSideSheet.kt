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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.theme.dimens

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
    floatingActionButton: (@Composable () -> Unit)? = null,
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
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    bottomStart = 16.dp,
                ),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                tonalElevation = 2.dp,
            ) {
                Box {
                    Column(modifier = Modifier.fillMaxSize()) {
                        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
                            header()
                        }
                        AlohomoraHorizontalDivider()
                        content()
                    }
                    floatingActionButton?.let { floatingActionButton ->
                        Box(
                            modifier = Modifier
                                .padding(MaterialTheme.dimens.margin.xl)
                                .align(Alignment.BottomEnd),
                        ) {
                            floatingActionButton.invoke()
                        }
                    }
                }
            }
        }
    }
}

private const val SCRIM_ALPHA = 0.6f
