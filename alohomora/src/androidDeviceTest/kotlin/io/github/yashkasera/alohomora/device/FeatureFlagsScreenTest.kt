package io.github.yashkasera.alohomora.device

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.device.Seed.flag
import io.github.yashkasera.alohomora.device.Seed.seedFeatureFlags
import io.github.yashkasera.alohomora.presentation.navigation.Routes
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.Chrome
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.FeatureFlags
import org.junit.Rule
import org.junit.Test

class FeatureFlagsScreenTest {

    @get:Rule(order = 0)
    val console = ConsoleTestRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    @Test
    fun emptyStateWhenNoFlagsRecorded() {
        compose.launchConsole(Routes.FeatureFlags)

        compose.onNodeWithTag(Chrome.EMPTY_STATE).assertIsDisplayed()
        compose.onNodeWithTag(Chrome.EMPTY_STATE_TITLE).assertTextContains("No Feature Flags")
    }

    @Test
    fun seededFlagsAreListed() {
        console.seedFeatureFlags(
            flag(key = "checkout_v2", value = "true"),
            flag(key = "dark_mode", value = "false"),
        )

        compose.launchConsole(Routes.FeatureFlags)

        compose.onNodeWithTag(FeatureFlags.item("checkout_v2")).assertIsDisplayed()
        compose.onNodeWithTag(FeatureFlags.item("dark_mode")).assertIsDisplayed()
    }

    @Test
    fun theSourceFilterNarrowsTheList() {
        seedTwoSources()

        compose.launchConsole(Routes.FeatureFlags)
        compose.onNodeWithTag(FeatureFlags.sourceFilter(LOCAL)).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(FeatureFlags.item("local_only")).assertIsDisplayed()
        compose.onNodeWithTag(FeatureFlags.item("remote_only")).assertDoesNotExist()
    }

    @Test
    fun tappingTheSelectedSourceFilterClearsIt() {
        seedTwoSources()

        compose.launchConsole(Routes.FeatureFlags)
        compose.onNodeWithTag(FeatureFlags.sourceFilter(LOCAL)).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(FeatureFlags.sourceFilter(LOCAL)).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(FeatureFlags.item("local_only")).assertIsDisplayed()
        compose.onNodeWithTag(FeatureFlags.item("remote_only")).assertIsDisplayed()
    }

    @Test
    fun searchNarrowsByKey() {
        console.seedFeatureFlags(
            flag(key = "checkout_v2"),
            flag(key = "dark_mode"),
        )

        compose.launchConsole(Routes.FeatureFlags)
        compose.onTextFieldIn(Chrome.SEARCH).performTextInput("dark")
        compose.waitForIdle()

        compose.onNodeWithTag(FeatureFlags.item("dark_mode")).assertIsDisplayed()
        compose.onNodeWithTag(FeatureFlags.item("checkout_v2")).assertDoesNotExist()
    }

    @Test
    fun theFooterCountsFlagsAndSources() {
        seedTwoSources()

        compose.launchConsole(Routes.FeatureFlags)

        // The tag is on the footer `Row`, which merges nothing, so the count has to be matched on
        // the `Text` beneath it rather than read off the tagged node.
        compose.onNode(
            hasText("2 flags from 2 sources") and
                hasAnyAncestor(hasTestTag(FeatureFlags.FOOTER)),
            useUnmergedTree = true,
        ).assertExists()
    }

    @Test
    fun theFooterReportsTheFilteredCountSeparately() {
        seedTwoSources()

        compose.launchConsole(Routes.FeatureFlags)
        compose.onNodeWithTag(FeatureFlags.sourceFilter(LOCAL)).performClick()
        compose.waitForIdle()

        compose.onNode(
            hasText("1 of 2 flags from 2 sources") and
                hasAnyAncestor(hasTestTag(FeatureFlags.FOOTER)),
            useUnmergedTree = true,
        ).assertExists()
    }

    @Test
    fun clearingFeatureFlagsEmptiesTheList() {
        console.seedFeatureFlags(flag(key = "checkout_v2"))

        compose.launchConsole(Routes.FeatureFlags)
        compose.onNodeWithTag(FeatureFlags.item("checkout_v2")).assertIsDisplayed()

        // Unlike the seeding helpers this goes through the public API, which launches onto
        // Alohomora's own `Dispatchers.Default` scope — so the clear is not visible when the call
        // returns and the assertion has to wait for it rather than for composition.
        Alohomora.clearFeatureFlags()
        compose.awaitTag(Chrome.EMPTY_STATE)

        compose.onNodeWithTag(FeatureFlags.item("checkout_v2")).assertDoesNotExist()
    }

    /** The source chip row only renders above one source, so every filter test needs two. */
    private fun seedTwoSources() {
        console.seedFeatureFlags(
            flag(key = "remote_only", source = REMOTE),
            flag(key = "local_only", source = LOCAL),
        )
    }

    private companion object {
        const val REMOTE = "Firebase Remote Config"
        const val LOCAL = "Local Overrides"
    }
}
