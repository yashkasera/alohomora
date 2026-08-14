package io.github.yashkasera.alohomora.device

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.github.yashkasera.alohomora.device.Seed.seedSpans
import io.github.yashkasera.alohomora.device.Seed.span
import io.github.yashkasera.alohomora.presentation.navigation.Routes
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.Chrome
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.Traces
import org.junit.Rule
import org.junit.Test

class TracesScreenTest {

    @get:Rule(order = 0)
    val console = ConsoleTestRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    @Test
    fun emptyStateWhenNoSpansCaptured() {
        compose.launchConsole(Routes.Traces)

        compose.onNodeWithTag(Chrome.EMPTY_STATE).assertIsDisplayed()
        compose.onNodeWithTag(Chrome.EMPTY_STATE_TITLE).assertTextContains("No traces yet")
    }

    @Test
    fun spansSharingATraceIdCollapseIntoOneRow() {
        console.seedSpans(
            span(traceId = "trace-a", spanId = "root", name = "GET /orders"),
            span(traceId = "trace-a", spanId = "child-1", name = "db.query", parentSpanId = "root"),
            span(
                traceId = "trace-a",
                spanId = "child-2",
                name = "cache.get",
                parentSpanId = "root",
            ),
        )

        compose.launchConsole(Routes.Traces)

        compose.onNodeWithTag(Traces.item("trace-a"))
            .assertTextContains("GET /orders", substring = true)
        compose.onNodeWithTag(Traces.item("trace-a"))
            .assertTextContains("3 spans", substring = true)
    }

    @Test
    fun twoTraceIdsGiveTwoRows() {
        console.seedSpans(
            span(traceId = "trace-a", spanId = "root-a", name = "GET /orders"),
            span(traceId = "trace-b", spanId = "root-b", name = "POST /login"),
        )

        compose.launchConsole(Routes.Traces)

        compose.onNodeWithTag(Traces.item("trace-a")).assertIsDisplayed()
        compose.onNodeWithTag(Traces.item("trace-b")).assertIsDisplayed()
    }

    @Test
    fun errorFilterNarrowsToTracesContainingAnErrorSpan() {
        console.seedSpans(
            span(traceId = "trace-ok", spanId = "root-ok", name = "GET /orders"),
            span(
                traceId = "trace-bad",
                spanId = "root-bad",
                name = "POST /login",
                statusCode = "ERROR",
            ),
        )

        compose.launchConsole(Routes.Traces)
        compose.onNodeWithTag(Traces.ERROR_FILTER).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(Traces.item("trace-bad")).assertIsDisplayed()
        compose.onNodeWithTag(Traces.item("trace-ok")).assertDoesNotExist()
    }

    @Test
    fun searchNarrowsByRootSpanName() {
        console.seedSpans(
            span(traceId = "trace-a", spanId = "root-a", name = "GET /orders"),
            span(traceId = "trace-b", spanId = "root-b", name = "POST /login"),
        )

        compose.launchConsole(Routes.Traces)
        compose.onTextFieldIn(Chrome.SEARCH).performTextInput("orders")
        compose.waitForIdle()

        compose.onNodeWithTag(Traces.item("trace-a")).assertIsDisplayed()
        compose.onNodeWithTag(Traces.item("trace-b")).assertDoesNotExist()
    }

    @Test
    fun clearAllEmptiesTheList() {
        console.seedSpans(
            span(traceId = "trace-a", spanId = "root-a", name = "GET /orders"),
            span(traceId = "trace-b", spanId = "root-b", name = "POST /login"),
        )

        compose.launchConsole(Routes.Traces)
        // No confirmation sheet here, unlike Traffic: this screen clears on the first tap.
        compose.onNodeWithTag(Chrome.CLEAR_ALL).performClick()

        // `awaitTag`, not `waitForIdle()`: clearing deletes through Room on the IO dispatcher and
        // the list only empties when the query flow re-emits, which the Compose clock cannot see.
        compose.awaitTag(Chrome.EMPTY_STATE)
    }

    @Test
    fun tappingATraceOpensItsDetails() {
        console.seedSpans(
            span(traceId = "trace-a", spanId = "root-a", name = "GET /orders"),
        )

        compose.launchConsole(Routes.Traces)
        compose.onNodeWithTag(Traces.item("trace-a")).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(Chrome.TOP_BAR_TITLE).assertTextContains("GET /orders")
    }
}
