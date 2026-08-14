package io.github.yashkasera.alohomora.showcaseApp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.FeatureFlags
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The five flags `AndroidApp.onCreate` records for the whole process.
 *
 * Nothing in this class seeds anything. `recordFeatureFlag` is called once per process at
 * application start, before any test runs, and no test clears the store — so the fixture is the
 * app's own startup and the assertions have to be written for state that is already there.
 *
 * No network.
 */
@RunWith(AndroidJUnit4::class)
class FeatureFlagsE2ETest : ShowcaseE2ETest() {

    @Test
    fun everyFlagRecordedAtStartupIsListed() {
        openFeatureFlags()

        listOf(DARK_MODE, CHECKOUT, MAX_CART_ITEMS, ONBOARDING, SEARCH_V3).forEach { key ->
            scrollToFlag(key)
            compose.onNodeWithTag(FeatureFlags.item(key)).assertIsDisplayed()
        }
    }

    @Test
    fun bothNamedSourcesAppearAsFilterChips() {
        openFeatureFlags()

        compose.onNodeWithTag(FeatureFlags.sourceFilter(FIREBASE)).assertIsDisplayed()
        compose.onNodeWithTag(FeatureFlags.sourceFilter(LAUNCH_DARKLY)).assertIsDisplayed()
    }

    @Test
    fun filteringByLaunchDarklyNarrowsToItsFlags() {
        openFeatureFlags()

        compose.onNodeWithTag(FeatureFlags.sourceFilter(LAUNCH_DARKLY)).performClick()
        compose.waitForIdle()

        scrollToFlag(MAX_CART_ITEMS)
        compose.onNodeWithTag(FeatureFlags.item(MAX_CART_ITEMS)).assertIsDisplayed()
        compose.onNodeWithTag(FeatureFlags.item(ONBOARDING)).assertExists()
        compose.onNodeWithTag(FeatureFlags.item(DARK_MODE)).assertDoesNotExist()
        compose.onNodeWithTag(FeatureFlags.item(SEARCH_V3)).assertDoesNotExist()
    }

    /**
     * `enable_search_v3` is recorded without a `source`, and `FeatureFlagsState.sources` is built
     * with `mapNotNull { it.source }` — so the flag is listed but adds no chip, and the footer
     * counts five flags across two sources rather than three.
     */
    @Test
    fun theFlagWithoutASourceIsListedButAddsNoSourceChip() {
        openFeatureFlags()

        scrollToFlag(SEARCH_V3)
        compose.onNodeWithTag(FeatureFlags.item(SEARCH_V3)).assertIsDisplayed()

        // The tag is on the footer `Row`, which merges nothing, so the count is matched on the
        // `Text` beneath it rather than read off the tagged node.
        compose.onNode(
            hasText("5 flags from 2 sources") and hasAnyAncestor(hasTestTag(FeatureFlags.FOOTER)),
            useUnmergedTree = true,
        ).assertExists()
    }

    private fun openFeatureFlags() {
        compose.openConsole()
        compose.openModule("FeatureFlags")
        compose.awaitTag(FeatureFlags.LIST)
    }

    /** The list is lazy: an off-screen row is absent from the semantics tree, not undisplayed. */
    private fun scrollToFlag(key: String) {
        compose.onNodeWithTag(FeatureFlags.LIST)
            .performScrollToNode(hasTestTag(FeatureFlags.item(key)))
    }

    private companion object {
        const val DARK_MODE = "dark_mode_v2"
        const val CHECKOUT = "checkout_redesign"
        const val MAX_CART_ITEMS = "max_cart_items"
        const val ONBOARDING = "onboarding_flow"
        const val SEARCH_V3 = "enable_search_v3"

        const val FIREBASE = "Firebase Remote Config"
        const val LAUNCH_DARKLY = "LaunchDarkly"
    }
}
