package io.github.yashkasera.alohomora.desktop.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.desktop.presentation.ui.theme.AlohomoraMotion
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.icons.ArrowLeft
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.X
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
    val host = LocalSideSheetHost.current
    val token = remember { Any() }
    val latestDismiss by rememberUpdatedState(onDismiss)
    DisposableEffect(host, visible) {
        if (visible) host.register(token) { latestDismiss() } else host.unregister(token)
        onDispose { host.unregister(token) }
    }

    // Corner morph lives outside both AnimatedVisibility blocks so it survives the visible→false
    // transition and can actually animate: it settles wider while closed and eases to the resting
    // `large` (16.dp) as the sheet arrives. The surface is detached from the window edges by a small
    // margin (below), so all four corners round rather than just the inner two. Driven off `visible`
    // with a spatial spring; kept local as this is a desktop surface.
    val startCorner by animateDpAsState(
        targetValue = if (visible) 16.dp else 40.dp,
        animationSpec = AlohomoraMotion.shapeMorph,
        label = "sideSheetCorner",
    )

    // Scrim animates on the effects spec — alpha must not overshoot.
    AnimatedVisibility(
        visible,
        enter = fadeIn(AlohomoraMotion.scrimFade),
        exit = fadeOut(AlohomoraMotion.scrimFade),
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
    // Surface slides on the spatial spring (the expressive settle) with a light effects fade so the
    // edge does not hard-pop. Exit uses the faster spec to keep the host's pop-vs-visible window tight.
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(AlohomoraMotion.scrimFade) + slideInHorizontally(AlohomoraMotion.sheetEnter) { it },
        exit = fadeOut(AlohomoraMotion.scrimFade) + slideOutHorizontally(AlohomoraMotion.sheetExit) { it },
    ) {
        Row(
            modifier = modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.End,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(widthFraction)
                    .fillMaxHeight()
                    .padding(
                        end = MaterialTheme.dimens.margin.md,
                        top = MaterialTheme.dimens.margin.md,
                        bottom = MaterialTheme.dimens.margin.md,
                    ),
                shape = RoundedCornerShape(
                    topStart = startCorner,
                    bottomStart = startCorner,
                    topEnd = startCorner,
                    bottomEnd = startCorner
                ),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                tonalElevation = 2.dp,
            ) {
                Box {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header renders in neutral content colours (see AlohomoraSideSheetHeader),
                        // matching the M3 side-sheet spec — no primary tint on the whole header.
                        header()
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

/**
 * The standard M3 side-sheet header: an optional leading back arrow, a headline (with optional
 * subtitle), a trailing actions cluster, and a trailing close.
 *
 * Neutral by design — headline in `onSurface`, glyphs in the icon button's default content colour —
 * so it reads like the Material side-sheet spec rather than a primary-tinted banner. Pass [onBack]
 * only on a sheet that has somewhere to go back *to* (a detail opened from a master list); standalone
 * detail sheets leave it null and show just the headline and close. Extra header actions go in
 * [actions], laid out between the headline and the close.
 */
@Composable
fun AlohomoraSideSheetHeader(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = if (onBack != null) MaterialTheme.dimens.margin.sm else MaterialTheme.dimens.margin.xl,
                end = MaterialTheme.dimens.margin.sm,
                top = MaterialTheme.dimens.margin.md,
                bottom = MaterialTheme.dimens.margin.md,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
    ) {
        onBack?.let { back ->
            AlohomoraIconButton(onClick = back) {
                Icon(
                    imageVector = Icons.ArrowLeft,
                    contentDescription = "Back",
                    modifier = Modifier.size(MaterialTheme.dimens.icon.lg),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        actions()
        AlohomoraIconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.X,
                contentDescription = "Close",
                modifier = Modifier.size(MaterialTheme.dimens.icon.lg),
            )
        }
    }
}

private const val SCRIM_ALPHA = 0.6f
