package io.github.yashkasera.alohomora.device

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.yashkasera.alohomora.device.Seed.seedSpans
import io.github.yashkasera.alohomora.device.Seed.span
import io.github.yashkasera.alohomora.presentation.navigation.Routes
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.TraceDetails
import org.junit.Rule
import org.junit.Test

class TraceDetailsScreenTest {

    @get:Rule(order = 0)
    val console = ConsoleTestRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    @Test
    fun parentAndChildSpansBothRender() {
        console.seedSpans(
            span(traceId = "trace-a", spanId = "root", name = "GET /orders"),
            span(
                traceId = "trace-a",
                spanId = "child",
                name = "db.query",
                parentSpanId = "root",
                startOffsetNanos = 1_000_000L,
                durationNanos = 2_000_000L,
            ),
        )

        compose.launchConsole(Routes.TraceDetails("trace-a"))

        compose.onNodeWithTag(TraceDetails.span("root")).assertIsDisplayed()
        compose.onNodeWithTag(TraceDetails.span("child")).assertIsDisplayed()
    }

    @Test
    fun collapsingAParentHidesItsSubtree() {
        console.seedSpans(
            span(traceId = "trace-a", spanId = "root", name = "GET /orders"),
            span(
                traceId = "trace-a",
                spanId = "child",
                name = "db.query",
                parentSpanId = "root",
                startOffsetNanos = 1_000_000L,
                durationNanos = 2_000_000L,
            ),
        )

        compose.launchConsole(Routes.TraceDetails("trace-a"))
        // The chevron is its own clickable inside the row's merging clickable, so it is absorbed
        // into the row in the merged tree and only addressable unmerged.
        compose.onNodeWithTag(TraceDetails.spanCollapse("root"), useUnmergedTree = true)
            .performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(TraceDetails.span("root")).assertIsDisplayed()
        compose.onNodeWithTag(TraceDetails.span("child")).assertDoesNotExist()
    }

    @Test
    fun anOrphanIsPromotedToARootAndFlagged() {
        // The parent named here is never seeded — the normal state of a trace still in flight, since
        // a parent ends after its children. `buildTraceTree` promotes the child to a root rather
        // than dropping it, so the trace is not empty while it is being watched.
        console.seedSpans(
            span(
                traceId = "trace-a",
                spanId = "child",
                name = "db.query",
                parentSpanId = "never-arrived",
            ),
        )

        compose.launchConsole(Routes.TraceDetails("trace-a"))

        compose.onNodeWithTag(TraceDetails.span("child"))
            .assertTextContains("ORPHAN", substring = true)
    }

    @Test
    fun aZeroDurationSpanStillLaysOut() {
        // The minimum-bar-width clamp in `TraceWindow.barGeometry` is what keeps an instantaneous
        // span from collapsing to nothing.
        console.seedSpans(
            span(traceId = "trace-a", spanId = "instant", name = "cache.hit", durationNanos = 0L),
        )

        compose.launchConsole(Routes.TraceDetails("trace-a"))

        compose.onNodeWithTag(TraceDetails.span("instant")).assertIsDisplayed()
    }

    @Test
    fun tappingASpanOpensTheSpanDetailSheet() {
        console.seedSpans(
            span(traceId = "trace-a", spanId = "root", name = "GET /orders"),
        )

        compose.launchConsole(Routes.TraceDetails("trace-a"))
        compose.onNodeWithTag(TraceDetails.span("root")).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(TraceDetails.SPAN_SHEET).assertIsDisplayed()
    }

    @Test
    fun viewToggleSwitchesBetweenTheListAndTheWaterfall() {
        console.seedSpans(
            span(traceId = "trace-a", spanId = "root", name = "GET /orders"),
        )

        compose.launchConsole(Routes.TraceDetails("trace-a"))
        compose.onNodeWithTag(TraceDetails.SPAN_LIST).assertIsDisplayed()

        compose.onNodeWithTag(TraceDetails.VIEW_TOGGLE).performClick()
        compose.waitForIdle()

        // The waterfall's own rows carry no span tags, so the container is the assertion.
        compose.onNodeWithTag(TraceDetails.WATERFALL).assertIsDisplayed()
        compose.onNodeWithTag(TraceDetails.SPAN_LIST).assertDoesNotExist()

        compose.onNodeWithTag(TraceDetails.VIEW_TOGGLE).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(TraceDetails.SPAN_LIST).assertIsDisplayed()
    }

    @Test
    fun waterfallFlagsAClockSkewedSpan() {
        // A negative duration puts the reported end before the start. Surfaced with a chip and drawn
        // as instantaneous, never reordered — correcting it would hide the bug it exists to show.
        // The chip lives on the waterfall row only; the phone span list has no skew indicator.
        console.seedSpans(
            span(
                traceId = "trace-a",
                spanId = "skewed",
                name = "GET /orders",
                durationNanos = -2_000_000L,
            ),
        )

        compose.launchConsole(Routes.TraceDetails("trace-a"))
        compose.onNodeWithTag(TraceDetails.VIEW_TOGGLE).performClick()
        compose.waitForIdle()

        // The waterfall is wider than the viewport and scrolls horizontally, so existence rather
        // than display is what can be asserted without driving a scroll.
        compose.onNodeWithText("SKEW", useUnmergedTree = true).assertExists()
    }
}
