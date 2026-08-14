package io.github.yashkasera.alohomora.device

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.device.Seed.seedTraffic
import io.github.yashkasera.alohomora.device.Seed.traffic
import io.github.yashkasera.alohomora.presentation.navigation.Routes
import io.github.yashkasera.alohomora.replay.ReplayOutcome
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.Chrome
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.TrafficDetails
import org.junit.Rule
import org.junit.Test

class TrafficDetailsScreenTest {

    @get:Rule(order = 0)
    val console = ConsoleTestRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    @Test
    fun overviewTabShowsTheRequestSummary() {
        console.seedTraffic(traffic(id = "a", host = "api.example.com", path = "/v1/posts"))

        compose.launchConsole(Routes.TrafficDetails("a"))

        compose.onNodeWithTag(TrafficDetails.ROOT).assertIsDisplayed()
        compose.onNodeWithText("HOST").assertIsDisplayed()
        compose.onNodeWithText("api.example.com").assertIsDisplayed()
    }

    @Test
    fun requestTabShowsRequestHeadersAndBody() {
        console.seedTraffic(
            traffic(id = "a", method = "POST", requestBody = """{"title":"hello"}"""),
        )

        compose.launchConsole(Routes.TrafficDetails("a"))
        compose.onNodeWithText("REQUEST").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("REQUEST HEADERS").assertIsDisplayed()
        compose.onNodeWithText("Accept: application/json").assertIsDisplayed()
        // The pager composes the neighbouring page, so existence alone would pass without the tab
        // switch above — but the body sits below the fold on a phone, so only its existence is
        // assertable here. The header assertions above are what prove the tab actually changed.
        compose.onNodeWithText("""{"title":"hello"}""").assertExists()
    }

    @Test
    fun responseTabShowsResponseHeaders() {
        console.seedTraffic(traffic(id = "a"))

        compose.launchConsole(Routes.TrafficDetails("a"))
        compose.onNodeWithText("RESPONSE").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("RESPONSE HEADERS").assertIsDisplayed()
        // Response headers are collapsed until the section header is tapped.
        compose.onNodeWithText("RESPONSE HEADERS").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Content-Type: application/json").assertIsDisplayed()
    }

    @Test
    fun replayActionIsHiddenWithoutARegisteredHandler() {
        console.seedTraffic(traffic(id = "a"))

        compose.launchConsole(Routes.TrafficDetails("a"))

        compose.onNodeWithTag(TrafficDetails.REPLAY).assertDoesNotExist()
    }

    @Test
    fun replayActionAppearsOnceAHandlerIsRegistered() {
        console.seedTraffic(traffic(id = "a"))
        // Registered before the screen is hosted: `canReplay` reads `Alohomora.isReplaySupported`
        // through a plain getter, which is not observable state, so a later registration would not
        // recompose the top bar.
        Alohomora.registerReplayHandler { ReplayOutcome.Sent() }

        compose.launchConsole(Routes.TrafficDetails("a"))

        compose.onNodeWithTag(TrafficDetails.REPLAY).assertIsDisplayed()
    }

    @Test
    fun replayActionIsHiddenForATruncatedRequestBody() {
        // A truncated body still looks like a body, so replaying it would send silently corrupted
        // data. `replayBlockedReason()` refuses it and the action is dropped rather than disabled.
        console.seedTraffic(traffic(id = "a", method = "POST", requestBodyTruncated = true))
        Alohomora.registerReplayHandler { ReplayOutcome.Sent() }

        compose.launchConsole(Routes.TrafficDetails("a"))

        compose.onNodeWithTag(TrafficDetails.REPLAY).assertDoesNotExist()
    }

    @Test
    fun replayResultBannerIsShownOnTheSourceEntry() {
        console.seedTraffic(
            traffic(id = "source", path = "/v1/posts", index = 0),
            traffic(id = "result", path = "/v1/replayed", index = 1, replayOf = "source"),
        )

        compose.launchConsole(Routes.TrafficDetails("source"))

        compose.onNodeWithTag(TrafficDetails.REPLAY_RESULT_BANNER).assertIsDisplayed()
    }

    @Test
    fun replayResultBannerOpensTheReplayedEntry() {
        console.seedTraffic(
            traffic(id = "source", path = "/v1/posts", index = 0),
            traffic(id = "result", path = "/v1/replayed", index = 1, replayOf = "source"),
        )

        compose.launchConsole(Routes.TrafficDetails("source"))
        compose.onNodeWithTag(TrafficDetails.REPLAY_RESULT_BANNER).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(Chrome.TOP_BAR_SUBTITLE).assertTextContains(
            "/v1/replayed",
            substring = true,
        )
    }
}
