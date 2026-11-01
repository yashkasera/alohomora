package io.github.yashkasera.alohomora.presentation.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/** How many items short of the end a list asks for the next page. */
private const val LOAD_MORE_THRESHOLD = 5

/**
 * Calls [onLoadMore] when the last visible item comes within [LOAD_MORE_THRESHOLD] of the end.
 *
 * Emits the item count rather than a near-the-end boolean. A boolean would fire once on arrival and
 * then stay true if the next page still fits on screen — a tablet, or a filter that matches few
 * rows — so the list would stop growing with the user parked at its end. Keying on the count fires
 * again each time the list grows while they are still there. Callers must therefore tolerate extra
 * calls; `LiveWindow.loadMore` ignores any that arrive before the last page has.
 */
@Composable
internal fun LoadMoreNearEnd(listState: LazyListState, onLoadMore: () -> Unit) {
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)
    LaunchedEffect(listState) {
        snapshotFlow {
            val layout = listState.layoutInfo
            val lastVisible = layout.visibleItemsInfo.lastOrNull()?.index ?: return@snapshotFlow -1
            val nearEnd = lastVisible >= layout.totalItemsCount - 1 - LOAD_MORE_THRESHOLD
            if (nearEnd) layout.totalItemsCount else -1
        }
            .distinctUntilChanged()
            .filter { count -> count > 0 }
            .collect { currentOnLoadMore() }
    }
}
