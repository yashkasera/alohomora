package io.github.yashkasera.alohomora.device

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.github.yashkasera.alohomora.device.Seed.seedTraffic
import io.github.yashkasera.alohomora.device.Seed.traffic
import io.github.yashkasera.alohomora.presentation.navigation.Routes
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.Chrome
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.Traffic
import org.junit.Rule
import org.junit.Test

class TrafficScreenTest {

    @get:Rule(order = 0)
    val console = ConsoleTestRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    @Test
    fun emptyStateWhenNothingCaptured() {
        compose.launchConsole(Routes.Traffic)

        compose.onNodeWithTag(Chrome.EMPTY_STATE).assertIsDisplayed()
        compose.onNodeWithTag(Chrome.EMPTY_STATE_TITLE)
            .assertTextContains("No Network Requests")
    }

    @Test
    fun seededRequestsAreListed() {
        console.seedTraffic(
            traffic(id = "a", method = "GET", path = "/v1/posts", index = 0),
            traffic(id = "b", method = "POST", path = "/v1/login", index = 1),
        )

        compose.launchConsole(Routes.Traffic)

        // The row is a clickable Card, so its semantics subtree is merged and the method, status
        // and path are all readable off the row's own tag.
        compose.onNodeWithTag(Traffic.item("a")).assertTextContains("GET", substring = true)
        compose.onNodeWithTag(Traffic.item("b")).assertTextContains("POST", substring = true)
    }

    @Test
    fun requestCountIsPublishedInTheSubtitle() {
        console.seedTraffic(
            traffic(id = "a", index = 0),
            traffic(id = "b", index = 1),
            traffic(id = "c", index = 2),
        )

        compose.launchConsole(Routes.Traffic)

        compose.onNodeWithTag(Chrome.TOP_BAR_SUBTITLE).assertTextContains("3 REQUESTS")
    }

    @Test
    fun methodFilterNarrowsTheList() {
        console.seedTraffic(
            traffic(id = "get-one", method = "GET", index = 0),
            traffic(id = "post-one", method = "POST", index = 1),
        )

        compose.launchConsole(Routes.Traffic)
        compose.onNodeWithTag(Traffic.methodFilter("POST")).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(Traffic.item("post-one")).assertIsDisplayed()
        compose.onNodeWithTag(Traffic.item("get-one")).assertDoesNotExist()
    }

    @Test
    fun tappingTheSelectedMethodFilterClearsIt() {
        console.seedTraffic(
            traffic(id = "get-one", method = "GET", index = 0),
            traffic(id = "post-one", method = "POST", index = 1),
        )

        compose.launchConsole(Routes.Traffic)
        compose.onNodeWithTag(Traffic.methodFilter("POST")).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(Traffic.methodFilter("POST")).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(Traffic.item("get-one")).assertIsDisplayed()
        compose.onNodeWithTag(Traffic.item("post-one")).assertIsDisplayed()
    }

    @Test
    fun searchFiltersByEndpoint() {
        console.seedTraffic(
            traffic(id = "posts", path = "/v1/posts", index = 0),
            traffic(id = "login", path = "/v1/login", index = 1),
        )

        compose.launchConsole(Routes.Traffic)
        compose.onTextFieldIn(Chrome.SEARCH).performTextInput("login")
        compose.waitForIdle()

        compose.onNodeWithTag(Traffic.item("login")).assertIsDisplayed()
        compose.onNodeWithTag(Traffic.item("posts")).assertDoesNotExist()
    }

    @Test
    fun clearAllRemovesEveryRequestOnceConfirmed() {
        console.seedTraffic(traffic(id = "a", index = 0), traffic(id = "b", index = 1))

        compose.launchConsole(Routes.Traffic)
        compose.onNodeWithTag(Chrome.CLEAR_ALL).performClick()
        compose.onNodeWithTag(Chrome.CONFIRM_ACCEPT).performClick()

        // `awaitTag`, not `waitForIdle()`: clearing deletes through Room on the IO dispatcher and
        // the list only empties when the query flow re-emits, which the Compose clock cannot see.
        compose.awaitTag(Chrome.EMPTY_STATE)
    }

    @Test
    fun cancellingClearAllKeepsTheRequests() {
        console.seedTraffic(traffic(id = "a", index = 0))

        compose.launchConsole(Routes.Traffic)
        compose.onNodeWithTag(Chrome.CLEAR_ALL).performClick()
        compose.onNodeWithTag(Chrome.CONFIRM_DISMISS).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(Traffic.item("a")).assertIsDisplayed()
    }

    @Test
    fun clearAllActionIsHiddenWhenThereIsNothingToClear() {
        compose.launchConsole(Routes.Traffic)

        compose.onNodeWithTag(Chrome.CLEAR_ALL).assertDoesNotExist()
    }

    @Test
    fun tappingARequestOpensItsDetails() {
        console.seedTraffic(traffic(id = "a", path = "/v1/posts", index = 0))

        compose.launchConsole(Routes.Traffic)
        compose.onNodeWithTag(Traffic.item("a")).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(Chrome.TOP_BAR_SUBTITLE).assertTextContains(
            "/v1/posts",
            substring = true,
        )
    }
}
