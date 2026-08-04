package io.github.yashkasera.alohomora.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.common.Span
import io.github.yashkasera.alohomora.common.trace.toTraceRows
import io.github.yashkasera.alohomora.common.trace.traceWindow
import io.github.yashkasera.alohomora.ui.components.waterfall.TraceWaterfall
import io.github.yashkasera.alohomora.ui.components.waterfall.WaterfallBarTestTagPrefix
import io.github.yashkasera.alohomora.ui.theme.AlohomoraTheme
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Behavioural tests for the waterfall, deliberately few.
 *
 * Only invariants that the pure arithmetic in `:alohomora-common` cannot reach are worth a real
 * composition here — the tick maths, tree assembly and time scaling are covered by fast unit tests in
 * `TraceTimeScaleTest` and `TraceTreeTest`. Colour, tick label positions and hover are not tested:
 * brittle, and the numbers behind them are already asserted.
 */
@OptIn(ExperimentalTestApi::class)
class TraceWaterfallTest {

    private fun span(
        id: String,
        parent: String? = null,
        start: Long,
        end: Long,
        name: String = "span-$id",
    ) = Span(
        traceId = "0af7651916cd43dd8448eb211c80319c",
        spanId = id,
        parentSpanId = parent,
        name = name,
        startEpochNanos = start,
        endEpochNanos = end,
    )

    /**
     * An instantaneous span must still get a row and a track to draw in.
     *
     * The minimum-width *arithmetic* is asserted in `TraceTimeScaleTest.barGeometry` — the bar is
     * painted in `drawBehind` and so has no semantics node of its own, which is exactly why that
     * clamping was moved out of the draw scope and into a pure function. What this test adds is the
     * half that arithmetic cannot cover: that a zero-duration span is still laid out at all, rather
     * than being filtered out somewhere between the tree flatten and the row.
     */
    @Test
    fun `an instantaneous span still gets a row and a track`() = runComposeUiTest {
        // 1ns beside a 1s sibling: the ratio is 1e-9, far below one pixel of the track.
        val spans = listOf(
            span("root", start = 0, end = 1_000_000_000),
            span("instant", parent = "root", start = 500_000_000, end = 500_000_000),
        )

        setContent {
            AlohomoraTheme {
                TraceWaterfall(
                    rows = spans.toTraceRows(),
                    window = traceWindow(spans),
                    selectedSpanId = null,
                    nameFraction = 0.4f,
                    onNameFractionChange = {},
                    onToggleCollapse = {},
                    onSelectSpan = {},
                    modifier = Modifier.size(width = 900.dp, height = 400.dp),
                )
            }
        }

        onNodeWithText("span-instant").assertIsDisplayed()
        // useUnmergedTree: the row's `clickable` merges its descendants' semantics, so the track's
        // test tag is only visible in the unmerged tree.
        assertTrue(
            onNodeWithTag("${WaterfallBarTestTagPrefix}instant", useUnmergedTree = true)
                .fetchSemanticsNode().size.width > 0,
            "the track must have width for the min-width clamp to draw into",
        )
    }

    @Test
    fun `collapsing a parent removes its descendants and keeps the parent`() = runComposeUiTest {
        val spans = listOf(
            span("root", start = 0, end = 1_000, name = "root-span"),
            span("child", parent = "root", start = 100, end = 200, name = "child-span"),
            span("grandchild", parent = "child", start = 120, end = 180, name = "grandchild-span"),
        )
        var collapsed by mutableStateOf(emptySet<String>())

        setContent {
            AlohomoraTheme {
                TraceWaterfall(
                    rows = spans.toTraceRows(collapsed),
                    window = traceWindow(spans),
                    selectedSpanId = null,
                    nameFraction = 0.5f,
                    onNameFractionChange = {},
                    onToggleCollapse = { id ->
                        collapsed = if (id in collapsed) collapsed - id else collapsed + id
                    },
                    onSelectSpan = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        onNodeWithText("grandchild-span").assertIsDisplayed()

        collapsed = setOf("child")
        waitForIdle()

        onNodeWithText("root-span").assertIsDisplayed()
        onNodeWithText("child-span").assertIsDisplayed()
        assertTrue(
            onAllNodesWithText("grandchild-span").fetchSemanticsNodes().isEmpty(),
            "a collapsed parent must hide its whole subtree",
        )
    }

    @Test
    fun `clicking a row reports the span it selected`() = runComposeUiTest {
        val spans = listOf(
            span("root", start = 0, end = 1_000, name = "root-span"),
            span("child", parent = "root", start = 100, end = 200, name = "child-span"),
        )
        var selected: String? = null

        setContent {
            AlohomoraTheme {
                TraceWaterfall(
                    rows = spans.toTraceRows(),
                    window = traceWindow(spans),
                    selectedSpanId = selected,
                    nameFraction = 0.5f,
                    onNameFractionChange = {},
                    onToggleCollapse = {},
                    onSelectSpan = { selected = it },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        onNodeWithText("child-span").performClick()
        waitForIdle()

        assertTrue(selected == "child", "expected 'child', got $selected")
    }
}
