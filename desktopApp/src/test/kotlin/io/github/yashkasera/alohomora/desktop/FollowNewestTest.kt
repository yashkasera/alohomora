package io.github.yashkasera.alohomora.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.ui.components.FollowNewest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Behavioural tests for auto-scroll.
 *
 * Written after shipping a version that did nothing at all: `snapshotFlow { itemCount }` over a
 * plain Int never re-emits, so the effect ran once at composition and never again. It compiled and
 * read correctly — only driving a real composition catches that.
 *
 * The second bug these cover is subtler: a LazyColumn with `key` anchors scroll to the visible
 * item's key, so a prepend shifts `firstVisibleItemIndex` off zero. Any implementation that asks
 * "am I at the top?" *after* the insert gets the wrong answer precisely when it matters.
 */
@OptIn(ExperimentalTestApi::class)
class FollowNewestTest {

    /** Harness mirroring the real stores: newest-first, growing at the head. */
    private class Harness {
        var items by mutableStateOf((1..30).map { "item-$it" }.asReversed())
        lateinit var listState: LazyListState
        private var next = 30

        fun prepend() {
            next += 1
            items = listOf("item-$next") + items
        }
    }

    private fun androidx.compose.ui.test.ComposeUiTest.start(): Harness {
        val harness = Harness()
        setContent {
            val state = rememberLazyListState()
            harness.listState = state
            FollowNewest(state, harness.items.size)
            Box(Modifier.fillMaxSize()) {
                LazyColumn(state = state, modifier = Modifier.testTag(LIST)) {
                    items(harness.items, key = { it }) { item ->
                        Text(item, modifier = Modifier.height(40.dp))
                    }
                }
            }
        }
        waitForIdle()
        return harness
    }

    @Test
    fun `a new item at the top scrolls the list back to it`() = runComposeUiTest {
        val harness = start()

        harness.prepend()
        waitForIdle()

        // Before the fix this stayed pinned to the previous head — the effect never re-ran.
        assertEquals(0, harness.listState.firstVisibleItemIndex)
        assertEquals(0, harness.listState.firstVisibleItemScrollOffset)
    }

    @Test
    fun `a burst of arrivals does not leave the list drifting`() = runComposeUiTest {
        val harness = start()

        repeat(5) {
            harness.prepend()
            waitForIdle()
        }

        assertEquals(0, harness.listState.firstVisibleItemIndex)
    }

    @Test
    fun `a scrolled-away reader is not yanked to the top`() = runComposeUiTest {
        val harness = start()
        onNodeWithTag(LIST).performTouchInput { swipeUp() }
        waitForIdle()
        assertTrue(harness.listState.firstVisibleItemIndex > 0, "precondition: scrolled away")

        harness.prepend()
        waitForIdle()

        // The entire point of following conditionally: reading is never interrupted.
        assertTrue(
            harness.listState.firstVisibleItemIndex > 0,
            "new arrivals must not drag a scrolled-away user to the top",
        )
    }

    @Test
    fun `scrolling back to the top re-arms following`() = runComposeUiTest {
        val harness = start()
        onNodeWithTag(LIST).performScrollToIndex(20)
        waitForIdle()

        onNodeWithTag(LIST).performScrollToIndex(0)
        waitForIdle()
        harness.prepend()
        waitForIdle()

        assertEquals(
            0,
            harness.listState.firstVisibleItemIndex,
            "returning to the top should resume following",
        )
    }

    private companion object {
        const val LIST = "list"
    }
}
