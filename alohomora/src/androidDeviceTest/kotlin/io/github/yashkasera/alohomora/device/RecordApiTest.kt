package io.github.yashkasera.alohomora.device

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.common.Error
import io.github.yashkasera.alohomora.device.Seed.BASE_MILLIS
import io.github.yashkasera.alohomora.device.Seed.BASE_NANOS
import io.github.yashkasera.alohomora.device.Seed.flag
import io.github.yashkasera.alohomora.domain.repository.ErrorRepository
import io.github.yashkasera.alohomora.presentation.navigation.Routes
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.Chrome
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.FeatureFlags
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.Traces
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.Traffic
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

private const val POLL_TIMEOUT_MILLIS = 10_000L
private const val POLL_INTERVAL_MILLIS = 50L

/** 32 hex characters, the width a real tracer emits. `recordSpan` lowercases whatever it gets. */
private const val TRACE_ID = "aaaabbbbccccdddd1111222233334444"
private const val ROOT_SPAN_ID = "1111222233334444"

/**
 * The public ingestion API — the calls a consumer actually writes — rather than the repositories
 * `Seed` writes through.
 *
 * **Every assertion here polls, and that is specific to this file.** `recordTraffic`, `recordEvent`,
 * `recordError`, `recordSpan` and `recordFeatureFlag` are fire-and-forget: each launches onto
 * `Alohomora`'s own `Dispatchers.Default` scope and returns before anything is written. The Compose
 * test clock knows nothing about that scope, so `waitForIdle()` returns while the row does not yet
 * exist. Elsewhere the fixtures write inside `runBlocking` and the row is committed before the test
 * touches the UI, which is why no other class needs this. A bare sleep would trade the race for a
 * slower race.
 *
 * **Nothing here throws an uncaught exception, deliberately.** `installCrashHandler` chains to ART's
 * default handler rather than swallowing, so a genuinely uncaught throw would kill the process and
 * take the rest of the instrumentation run with it. The chaining contract is covered off-device by
 * `androidHostTest`'s `CrashHandlerTest`; this file only exercises the explicit `recordError` calls.
 */
class RecordApiTest {

    @get:Rule(order = 0)
    val console = ConsoleTestRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    @Test
    fun recordTrafficReachesTheTrafficScreen() {
        Alohomora.recordTraffic(
            id = "recorded",
            status = 200,
            url = "https://api.example.com/v1/posts",
            method = "GET",
            scheme = "https",
            host = "api.example.com",
            path = "/v1/posts",
            time = BASE_MILLIS,
            duration = 42L,
        )

        compose.launchConsole(Routes.Traffic)

        compose.awaitTag(Traffic.item("recorded"))
        compose.onNodeWithTag(Traffic.item("recorded")).assertIsDisplayed()
    }

    @Test
    fun recordEventReachesTheEventsScreen() {
        Alohomora.recordEvent("checkout.started")

        compose.launchConsole(Routes.Events)

        compose.awaitText("checkout.started")
    }

    @Test
    fun recordEventCarriesItsProperties() {
        Alohomora.recordEvent("checkout.started", mapOf("step" to "cart"))

        compose.launchConsole(Routes.Events)

        // The properties toggle defaults to on, so the row renders the properties through
        // `prettyProperties()` — indented JSON with a space after the colon.
        compose.awaitText("\"step\": \"cart\"")
    }

    @Test
    fun recordErrorFromAThrowableReachesTheErrorsScreen() {
        Alohomora.recordError(IllegalStateException("boom"), place = "CheckoutViewModel")

        compose.launchConsole(Routes.Error)

        compose.awaitText("IllegalStateException")
    }

    @Test
    fun recordErrorFormatsReasonAsSimpleNameAndMessage() {
        Alohomora.recordError(IllegalStateException("boom"), place = "CheckoutViewModel")

        // Asserted against the store, not the screen: the list only renders the type half, and the
        // exact `SimpleName: message` shape is what both consoles parse back out.
        val error = awaitError { it.place == "CheckoutViewModel" }

        assertEquals("IllegalStateException: boom", error.reason)
    }

    @Test
    fun recordErrorFromValuesReachesTheErrorsScreen() {
        // The Swift-facing overload: a Swift `Error` is not a `KotlinThrowable`, so iOS callers
        // have no other way in.
        Alohomora.recordError(
            reason = "DecodingError: keyNotFound(\"id\")",
            stackTrace = "AlohomoraShowcase.decode(Payload.swift:22)",
            place = "PayloadDecoder",
        )

        compose.launchConsole(Routes.Error)

        compose.awaitText("DecodingError")
    }

    @Test
    fun recordSpanWithATraceContextReachesTheTracesScreen() {
        Alohomora.recordSpan(
            traceId = TRACE_ID,
            spanId = ROOT_SPAN_ID,
            name = "GET /v1/posts",
            // Epoch NANOSECONDS. Milliseconds here would render every span as a 1970 date.
            startEpochNanos = BASE_NANOS,
            endEpochNanos = BASE_NANOS + 5_000_000L,
            statusCode = "OK",
        )

        compose.launchConsole(Routes.Traces)

        compose.awaitTag(Traces.item(TRACE_ID))
        compose.onNodeWithTag(Traces.item(TRACE_ID)).assertIsDisplayed()
    }

    @Test
    fun recordStandaloneSpanReachesTheTracesScreen() {
        Alohomora.recordSpan(name = "image decode", durationNanos = 2_500_000L)

        compose.launchConsole(Routes.Traces)

        compose.awaitText("image decode")
    }

    @Test
    fun recordFeatureFlagReachesTheFeatureFlagsScreen() {
        Alohomora.recordFeatureFlag(
            key = "checkout_v2",
            value = "true",
            source = "Firebase Remote Config",
        )

        compose.launchConsole(Routes.FeatureFlags)

        compose.awaitTag(FeatureFlags.item("checkout_v2"))
        compose.onNodeWithTag(FeatureFlags.item("checkout_v2")).assertIsDisplayed()
    }

    @Test
    fun setFeatureFlagsReplacesTheFlagsForItsSource() {
        Alohomora.recordFeatureFlag(key = "stale", value = "true", source = "Remote")
        compose.launchConsole(Routes.FeatureFlags)
        compose.awaitTag(FeatureFlags.item("stale"))

        Alohomora.setFeatureFlags(listOf(flag(key = "fresh", source = "Remote")), source = "Remote")

        compose.awaitTag(FeatureFlags.item("fresh"))
        compose.onNodeWithTag(FeatureFlags.item("stale")).assertDoesNotExist()
    }

    @Test
    fun clearFeatureFlagsEmptiesTheScreen() {
        Alohomora.recordFeatureFlag(key = "checkout_v2", value = "true")
        compose.launchConsole(Routes.FeatureFlags)
        compose.awaitTag(FeatureFlags.item("checkout_v2"))

        Alohomora.clearFeatureFlags()

        compose.awaitTag(Chrome.EMPTY_STATE)
    }

    /** Waits for a node carrying [tag] to exist. See the class doc for why this cannot be `waitForIdle`. */
    private fun ComposeContentTestRule.awaitTag(tag: String) {
        waitUntil(timeoutMillis = POLL_TIMEOUT_MILLIS) {
            onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun ComposeContentTestRule.awaitText(text: String) {
        waitUntil(timeoutMillis = POLL_TIMEOUT_MILLIS) {
            onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Polls the error store, since this assertion has no UI to hang a Compose wait off. */
    private fun awaitError(predicate: (Error) -> Boolean): Error {
        val deadline = System.currentTimeMillis() + POLL_TIMEOUT_MILLIS
        var seen: List<Error> = emptyList()
        while (System.currentTimeMillis() < deadline) {
            seen = runBlocking {
                console.koin.get<ErrorRepository>().list("", 0, 50).first()
            }
            seen.firstOrNull(predicate)?.let { return it }
            Thread.sleep(POLL_INTERVAL_MILLIS)
        }
        throw AssertionError(
            "no matching error after ${POLL_TIMEOUT_MILLIS}ms; saw ${seen.map { it.reason }}",
        )
    }
}
