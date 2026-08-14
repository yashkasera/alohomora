package io.github.yashkasera.alohomora.device

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import io.github.yashkasera.alohomora.device.Seed.error
import io.github.yashkasera.alohomora.device.Seed.seedErrors
import io.github.yashkasera.alohomora.device.Seed.seedTraffic
import io.github.yashkasera.alohomora.device.Seed.traffic
import io.github.yashkasera.alohomora.devtools.DevToolsDefaults
import io.github.yashkasera.alohomora.presentation.navigation.Routes
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.Chrome
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.Overview
import org.junit.Rule
import org.junit.Test

class OverviewScreenTest {

    @get:Rule(order = 0)
    val console = ConsoleTestRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    @Test
    fun everyBuiltInModuleHasACard() {
        compose.launchConsole(Routes.Overview)

        builtInModuleKeys.forEach { key ->
            compose.scrollGridTo(Overview.moduleCard(key))
            compose.onNodeWithTag(Overview.moduleCard(key)).assertIsDisplayed()
        }
    }

    @Test
    fun tappingTheTrafficCardOpensTraffic() {
        compose.launchConsole(Routes.Overview)
        compose.openModule("Traffic")

        compose.onNodeWithTag(Chrome.TOP_BAR_TITLE).assertTextEquals("Traffic")
    }

    @Test
    fun tappingTheEventsCardOpensEvents() {
        compose.launchConsole(Routes.Overview)
        compose.openModule("Events")

        compose.onNodeWithTag(Chrome.TOP_BAR_TITLE).assertTextEquals("Events")
    }

    @Test
    fun tappingTheFeatureFlagsCardOpensFeatureFlags() {
        compose.launchConsole(Routes.Overview)
        compose.openModule("FeatureFlags")

        compose.onNodeWithTag(Chrome.TOP_BAR_TITLE).assertTextEquals("Feature Flags")
    }

    @Test
    fun theStatusCardIsRendered() {
        compose.launchConsole(Routes.Overview)

        compose.onNodeWithTag(Overview.STATUS_CARD).assertIsDisplayed()
    }

    @Test
    fun thePortFieldStartsOnTheDefaultPort() {
        compose.launchConsole(Routes.Overview)

        compose.onTextFieldIn(Overview.STATUS_PORT_FIELD)
            .assertTextContains(DevToolsDefaults.DEFAULT_PORT.toString())
    }

    @Test
    fun needsAttentionIsAbsentWhenNothingIsUnviewed() {
        compose.launchConsole(Routes.Overview)

        compose.onNodeWithTag(Overview.NEEDS_ATTENTION).assertDoesNotExist()
    }

    @Test
    fun needsAttentionIsAbsentForASuccessfulRequest() {
        // `observeUnviewedFailed` filters on `status NOT BETWEEN 200 AND 299`, so an unviewed 200
        // is not an attention item. This is the half of that query a failing fixture cannot show.
        console.seedTraffic(traffic(id = "ok", status = 200))

        compose.launchConsole(Routes.Overview)

        compose.onNodeWithTag(Overview.NEEDS_ATTENTION).assertDoesNotExist()
    }

    @Test
    fun needsAttentionAppearsForAnUnviewedFailedRequest() {
        console.seedTraffic(traffic(id = "boom", status = 500, path = "/v1/checkout"))

        compose.launchConsole(Routes.Overview)
        compose.awaitTag(Overview.NEEDS_ATTENTION)

        compose.onNodeWithTag(Overview.NEEDS_ATTENTION).assertIsDisplayed()
    }

    @Test
    fun needsAttentionAppearsForAnUnviewedError() {
        console.seedErrors(error(reason = "java.lang.IllegalStateException: boom"))

        compose.launchConsole(Routes.Overview)
        compose.awaitTag(Overview.NEEDS_ATTENTION)

        compose.onNodeWithTag(Overview.NEEDS_ATTENTION).assertIsDisplayed()
    }
}
