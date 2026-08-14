package io.github.yashkasera.alohomora.device

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.Overview

/**
 * Waiting helpers for state the Compose clock cannot see.
 *
 * `waitForIdle()` settles composition, animations and the test clock — and nothing else. Three of
 * the console's screens read through a `Dispatchers.IO` flow (Room for Overview's attention items,
 * raw SQLite for the Database inspector), so composition goes idle a long way before the data
 * lands. Asserting straight after `launchConsole` therefore races, and the race is one-sided: it
 * passes on a fast device and fails on a loaded one.
 *
 * The default timeout is generous on purpose. These open real database files off the device's
 * filesystem, which is slower under an instrumentation run than anything a unit test measures.
 */
internal const val AWAIT_TIMEOUT_MILLIS: Long = 5_000L

internal fun ComposeContentTestRule.awaitTag(
    tag: String,
    timeoutMillis: Long = AWAIT_TIMEOUT_MILLIS,
) {
    waitUntil(timeoutMillis) {
        onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }
}

internal fun ComposeContentTestRule.awaitText(
    text: String,
    timeoutMillis: Long = AWAIT_TIMEOUT_MILLIS,
) {
    waitUntil(timeoutMillis) {
        onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
    }
}

/**
 * The editable node inside a tagged [io.github.yashkasera.alohomora.ui.components.AlohomoraTextField].
 *
 * The caller's modifier — and therefore the test tag — lands on the wrapping `Column` that also
 * carries the optional label, while the `SetText` action belongs to the `BasicTextField` beneath
 * it. Compose does not merge an editable field's semantics into its parent, so
 * `onNodeWithTag(tag).performTextInput(...)` addresses a node that has no text action and throws.
 *
 * Every search box, the SQL editor and the DevTools port field share that shape, so the workaround
 * lives here once instead of in each test.
 */
internal fun ComposeContentTestRule.onTextFieldIn(tag: String): SemanticsNodeInteraction =
    onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(tag)), useUnmergedTree = true)

/**
 * Brings an Overview card into view.
 *
 * Ten module cards plus the status card do not fit one screen, and the grid is lazy — an
 * off-screen card is not merely undisplayed, it does not exist in the semantics tree at all.
 */
internal fun ComposeContentTestRule.scrollGridTo(tag: String) {
    onNodeWithTag(Overview.GRID).performScrollToNode(hasTestTag(tag))
}

/** [routeKey] is the module's route simple name, e.g. `Traffic`, `GitHistory`. */
internal fun ComposeContentTestRule.openModule(routeKey: String) {
    scrollGridTo(Overview.moduleCard(routeKey))
    onNodeWithTag(Overview.moduleCard(routeKey)).performClick()
    waitForIdle()
}

/**
 * Route simple names for every built-in module, in grid order.
 *
 * `Error`, not `Errors`: the tag follows `Routes.Error::class.simpleName`, which is what the grid
 * keys its cards by — not the screen's title.
 */
internal val builtInModuleKeys: List<String> = listOf(
    "Traffic",
    "Traces",
    "Database",
    "Error",
    "Cache",
    "Events",
    "FeatureFlags",
    "Config",
    "GitHistory",
)
