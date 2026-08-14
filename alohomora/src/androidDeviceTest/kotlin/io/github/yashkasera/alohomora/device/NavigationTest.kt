package io.github.yashkasera.alohomora.device

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import io.github.yashkasera.alohomora.presentation.navigation.Routes
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.Chrome
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.Overview
import org.junit.Rule
import org.junit.Test

/**
 * Overview → module → back, once per built-in module.
 *
 * One `@Test` each rather than a single loop over a table: a loop reports only the first module
 * that breaks and hides the rest behind it, and the failure names the *assertion* rather than the
 * screen. Nine near-identical tests are the price of a failure that reads "tracesRoundTripsToOverview".
 * The shared body lives in [assertRoundTrip], so the duplication is the name and nothing else.
 *
 * The titles are asserted, not the routes, because the top bar is the only thing a user can see
 * that distinguishes one destination from another — and two of them do not match the card that
 * opened them (`Database` opens a screen titled "Vault"; `Error` opens one titled "Errors").
 */
class NavigationTest {

    @get:Rule(order = 0)
    val console = ConsoleTestRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    @Test
    fun trafficRoundTripsToOverview() = assertRoundTrip("Traffic", "Traffic")

    @Test
    fun tracesRoundTripsToOverview() = assertRoundTrip("Traces", "Traces")

    @Test
    fun databaseRoundTripsToOverview() = assertRoundTrip("Database", "Vault")

    @Test
    fun errorsRoundTripsToOverview() = assertRoundTrip("Error", "Errors")

    @Test
    fun cacheRoundTripsToOverview() = assertRoundTrip("Cache", "Cache")

    @Test
    fun eventsRoundTripsToOverview() = assertRoundTrip("Events", "Events")

    @Test
    fun featureFlagsRoundTripsToOverview() = assertRoundTrip("FeatureFlags", "Feature Flags")

    @Test
    fun configRoundTripsToOverview() = assertRoundTrip("Config", "Config")

    @Test
    fun gitHistoryRoundTripsToOverview() = assertRoundTrip("GitHistory", "Git History")

    /**
     * [routeKey] is the module's route simple name, [expectedTitle] the destination's top bar title.
     */
    private fun assertRoundTrip(routeKey: String, expectedTitle: String) {
        compose.launchConsole(Routes.Overview)

        compose.openModule(routeKey)
        compose.onNodeWithTag(Chrome.TOP_BAR_TITLE).assertTextEquals(expectedTitle)

        compose.onNodeWithTag(Chrome.BACK).performClick()
        compose.waitForIdle()

        // Overview has no top bar title — its header is the wordmark — so the grid is what
        // identifies it.
        compose.onNodeWithTag(Overview.GRID).assertIsDisplayed()
    }
}
