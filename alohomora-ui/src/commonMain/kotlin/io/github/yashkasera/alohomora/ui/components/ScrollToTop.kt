package io.github.yashkasera.alohomora.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags
import io.github.yashkasera.alohomora.ui.icons.ArrowLeft
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.theme.dimens
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/**
 * Scroll affordances shared by the mobile console and the desktop app.
 *
 * Lists are ordered newest-first, so "scroll to top" and "jump to latest" are the same gesture —
 * which is why one control covers both a live stream and a static list.
 */

/** Index past which the jump control is worth showing. */
private const val REVEAL_AFTER_ITEMS = 2

/**
 * Keeps a streaming list pinned to the newest entry — but only while the user is already there.
 *
 * Following unconditionally is the obvious implementation and the wrong one: on a busy trace
 * stream it yanks the list away mid-read every time a request lands. So the moment the user
 * scrolls off the top this quietly stops, and [ScrollToTopButton] gives them the way back.
 *
 * Two things here are easy to get wrong, and the first version got both:
 *
 * 1. The trigger must be `LaunchedEffect(itemCount)`. Wrapping it in `snapshotFlow { itemCount }`
 *    looks equivalent but is inert — snapshotFlow re-emits only when *snapshot state* read in its
 *    block changes, and a plain Int parameter registers no dependency, so it fires once at
 *    composition and never again.
 *
 * 2. "Am I at the top?" cannot be answered *after* the insert. A LazyColumn with `key` anchors
 *    scroll to the visible item's key, so prepending N items moves `firstVisibleItemIndex` to N —
 *    the check reads false at exactly the moment it should read true. So follow state is tracked
 *    from settled user scrolls instead, before any insert can shift it.
 *
 * @param itemCount what drives the effect. Passing the list itself would re-run on every in-place
 *   update (a trace completing, say) and fight a user reading that very trace.
 */
@Composable
fun FollowNewest(listState: LazyListState, itemCount: Int) {
    val following = rememberFollowState(
        isAtTop = { listState.firstVisibleItemIndex == 0 },
        isScrolling = { listState.isScrollInProgress },
    )
    LaunchedEffect(itemCount) {
        if (following.value && itemCount > 0) listState.animateScrollToItem(0)
    }
}

/**
 * Whether the list should keep following the newest item.
 *
 * Recomputed only when a scroll *settles*, which is what makes it survive an insert: by the time
 * new items shift the anchor, the answer was already recorded. Re-settling at the top after the
 * follow animation naturally re-arms it.
 */
@Composable
private fun rememberFollowState(
    isAtTop: () -> Boolean,
    isScrolling: () -> Boolean,
): State<Boolean> {
    val following = remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        snapshotFlow { isScrolling() }
            .distinctUntilChanged()
            .filter { scrolling -> !scrolling }
            .collect { following.value = isAtTop() }
    }
    return following
}

/**
 * A floating control that returns the user to the top of [listState].
 *
 * Place inside a [Box] that also holds the list, aligned bottom-end.
 */
@Composable
fun BoxScope.ScrollToTopButton(listState: LazyListState, modifier: Modifier = Modifier) {
    // derivedStateOf so recomposition is driven by the boolean flipping, not by every pixel of
    // scroll — this reads a value that changes on every frame of a fling.
    val visible by remember(listState) {
        derivedStateOf { listState.firstVisibleItemIndex > REVEAL_AFTER_ITEMS }
    }
    val scope = rememberCoroutineScope()
    ScrollToTopButton(
        visible = visible,
        onClick = { scope.launch { listState.animateScrollToItem(0) } },
        modifier = modifier,
    )
}

/** [ScrollToTopButton] for a plain `verticalScroll` container. */
@Composable
fun BoxScope.ScrollToTopButton(scrollState: ScrollState, modifier: Modifier = Modifier) {
    val visible by remember(scrollState) {
        derivedStateOf { scrollState.value > SCROLL_REVEAL_PX }
    }
    val scope = rememberCoroutineScope()
    ScrollToTopButton(
        visible = visible,
        onClick = { scope.launch { scrollState.animateScrollTo(0) } },
        modifier = modifier,
    )
}

/** Roughly a screenful before the control appears on a non-lazy container. */
private const val SCROLL_REVEAL_PX = 600

/**
 * Jump-to-latest for a list that grows downwards, i.e. logcat.
 *
 * Logs stay oldest-first because that is the terminal convention every developer already reads
 * them in; forcing them newest-first for consistency with the trace list would be change for its
 * own sake. So here "latest" is the bottom, and the arrow points the other way.
 */
@Composable
fun BoxScope.ScrollToBottomButton(
    listState: LazyListState,
    itemCount: Int,
    modifier: Modifier = Modifier,
) {
    val visible by remember(listState) {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible < total - 1 - REVEAL_AFTER_ITEMS
        }
    }
    val scope = rememberCoroutineScope()
    ScrollToTopButton(
        visible = visible,
        onClick = { scope.launch { listState.animateScrollToItem((itemCount - 1).coerceAtLeast(0)) } },
        modifier = modifier,
        rotation = -90f,
    )
}

@Composable
private fun BoxScope.ScrollToTopButton(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    rotation: Float = 90f,
) {
    // One tag on the AnimatedVisibility, not on each branch below: which of the two controls a
    // device renders depends on its width, and no test should have to care which it got.
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.8f),
        modifier = modifier
            .align(Alignment.BottomEnd)
            .padding(MaterialTheme.dimens.margin.xxl)
            .testTag(AlohomoraTestTags.Chrome.SCROLL_TO_TOP),
    ) {
        BoxWithConstraints {
            if (maxWidth > 400.dp) {
                FloatingActionButton(
                    onClick = onClick,
                ) {
                    Icon(
                        imageVector = Icons.ArrowLeft,
                        contentDescription = "Scroll to top",
                        modifier = Modifier.rotate(rotation),
                    )
                }
            } else {
                AlohomoraIconButton(
                    onClick = onClick,
                    style = AlohomoraIconButtonStyle.TONAL
                ) {
                    Icon(
                        imageVector = Icons.ArrowLeft,
                        contentDescription = "Scroll to top",
                        modifier = Modifier.rotate(rotation),
                    )
                }
            }
        }
    }
}
