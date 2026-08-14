package io.github.yashkasera.alohomora.showcaseApp

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.Traces
import org.junit.Test
import org.junit.runner.RunWith

/**
 * OpenTelemetry spans reaching the Traces console through the app's own exporter.
 *
 * Nothing here calls `Alohomora.recordSpan` directly, and that is the point: `PostRepositoryImpl`
 * instruments the refresh with the app's `Tracer`, `showcaseTracerProvider` registers
 * `AlohomoraSpanExporter` as a `BatchSpanProcessor`, and `recordSpan` is reached only from inside
 * that adapter. Driving the refresh therefore covers the whole boundary the library is designed
 * around — the epoch-nanosecond contract, the zeroed parent id OpenTelemetry emits for a root, and
 * the verbatim `kind`/`statusCode` strings — rather than just the ingestion API.
 *
 * The processor's schedule delay is one second, so a trace lands a beat after the request it
 * describes. Needs network.
 */
@RunWith(AndroidJUnit4::class)
class SpanCaptureE2ETest : ShowcaseE2ETest() {

    @Test
    fun aRefreshProducesOneTraceRootedAtPostsRefresh() {
        openTracesAfterARefresh()

        // The row is titled by its root span and counts the whole group, per `toTraceSummaries()`.
        // Five is what one refresh emits: posts.refresh, posts.fetch_remote, GET /posts,
        // GET /posts/{id}/author and db.replace_all.
        compose.awaitNode(refreshTrace(), NETWORK_TIMEOUT_MILLIS)
    }

    /**
     * The author lookup 404s and `PostRepositoryImpl` marks its span `StatusCode.ERROR` by hand —
     * Ktor returned normally, so only the caller knows it failed. The filter finding this trace is
     * what proves the status survived the exporter as the string `"ERROR"`, which is the value both
     * consoles style as a failure.
     */
    @Test
    fun theErrorFilterKeepsTheRefreshTraceBecauseTheAuthorSpanFailed() {
        openTracesAfterARefresh()
        compose.awaitNode(refreshTrace(), NETWORK_TIMEOUT_MILLIS)

        compose.onNodeWithTag(Traces.ERROR_FILTER).performClick()
        compose.waitForIdle()

        compose.awaitNode(refreshTrace())
    }

    private fun openTracesAfterARefresh() {
        compose.launchShowcaseApp()
        compose.refreshPosts()

        compose.openConsole()
        compose.openModule("Traces")
    }

    private fun refreshTrace() =
        hasText("posts.refresh", substring = true) and
            hasText("5 spans", substring = true) and
            hasAnyAncestor(hasTestTag(Traces.LIST))
}
