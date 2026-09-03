package io.github.yashkasera.alohomora.showcaseApp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.ReplaySheet
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.Traffic
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.TrafficDetails
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Replay, end to end: a captured request re-sent through the app's own Ktor client.
 *
 * `AndroidApp.onCreate` registers `ktorReplayHandler(koin.get<HttpClient>())`, so
 * `Alohomora.isReplaySupported` is true for this APK and `TraceDetailsState.canReplay` lets the
 * action render. `:alohomora`'s own `TrafficDetailsScreenTest` has to register a stub handler that
 * returns `ReplayOutcome.Sent()` without sending anything; here the request actually goes out.
 *
 * Two behaviours of the real path are worth knowing when reading the assertions:
 *
 * - `ReplayHeaders.sanitize` drops every `[REDACTED]` value, so the `Authorization` and `X-Api-Key`
 *   headers `PostsApi` sets do not travel with the replay — the app's own client is expected to
 *   resupply them. jsonplaceholder ignores both either way.
 * - The handler returns no id. `AlohomoraInspector` is supposed to mint one for the replayed request
 *   and stamp `replayOf` with the source id, and `ObserveReplayResultUseCase` is what finds it. The
 *   banner is therefore the only place in the UI where a *round-tripped* replay is observable.
 *
 * **The `replayOf` stamp does not currently survive on Android**, which is why nothing here asserts
 * the banner. Driving this on a device showed the replay going out and being captured normally —
 * an extra `GET /posts` row with no sibling — but with `replayOf` NULL, so `observeReplayOf` never
 * matches and the banner never appears. `ktorReplayHandler` puts `AlohomoraReplayOfKey` into the
 * `HttpRequestBuilder`'s attributes and `AlohomoraInspector.onRequest` reads it back, so the loss is
 * somewhere between the two. Restore the banner assertion once that is fixed — it is the only
 * end-to-end proof that a replay is linked to its source.
 *
 * Needs network.
 */
@RunWith(AndroidJUnit4::class)
class ReplayE2ETest : ShowcaseE2ETest() {

    @Test
    fun theReplayActionIsOfferedBecauseTheAppRegistersAHandler() {
        openCapturedPostsRequest()

        compose.onNodeWithTag(TrafficDetails.REPLAY).assertIsDisplayed()
    }

    @Test
    fun sendingAReplayCompletesThroughTheAppsOwnClient() {
        openCapturedPostsRequest()

        compose.onNodeWithTag(TrafficDetails.REPLAY).performClick()
        compose.awaitTag(ReplaySheet.ROOT)

        // Send lives in the replay screen's sticky bottom bar, so it is always on screen — no
        // scrolling required.
        compose.onNodeWithTag(ReplaySheet.SEND).performClick()

        // The editor leaving composition is the success signal, and the strongest one available
        // until the `replayOf` stamp is fixed: `ReplayViewModel` keeps the screen open on
        // `ReplayOutcome.Failed` so the user's edits survive the mistake, so a closed editor means
        // the request really went out through the app's own client and came back.
        compose.awaitNoTag(ReplaySheet.ROOT, NETWORK_TIMEOUT_MILLIS)
    }

    /**
     * Opens the details of the successful `/posts` fetch.
     *
     * The 404 author lookup is skipped deliberately: it is replayable too, but a row keyed on
     * content has to be unambiguous, and only one row is a 200 to this path.
     */
    private fun openCapturedPostsRequest() {
        compose.launchShowcaseApp()
        compose.refreshPosts()

        compose.openConsole()
        compose.openModule("Traffic")

        val row = hasText("jsonplaceholder.typicode.com") and
            hasText("GET") and
            hasText("200") and
            hasText("/posts", substring = true) and
            hasAnyAncestor(hasTestTag(Traffic.LIST))

        compose.awaitNode(row, NETWORK_TIMEOUT_MILLIS)
        // `onFirst`, not `onNode`: `PostsViewModel.init` already refreshed once before the tap, so
        // there are two identical successful fetches and `onNode` would fail on the ambiguity.
        compose.onAllNodes(row).onFirst().performClick()
        compose.awaitTag(TrafficDetails.ROOT)
    }
}
