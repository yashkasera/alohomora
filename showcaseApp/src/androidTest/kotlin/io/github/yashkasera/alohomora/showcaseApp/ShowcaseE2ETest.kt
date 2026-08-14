package io.github.yashkasera.alohomora.showcaseApp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.DevToolsActivity
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.Overview
import org.junit.Rule
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Base for every showcase end-to-end test: real app, real console, real network.
 *
 * **Why [createEmptyComposeRule] and not `createComposeRule()`.** These tests drive two activities
 * that no activity-scoped rule can span. `AppActivity` is `launchMode="singleInstance"` and
 * `DevToolsActivity` is `singleTask` with its own `taskAffinity`, so they live in separate tasks and
 * neither is a host the other can be launched into. `createComposeRule()` would additionally launch
 * `ui-test-manifest`'s bare `ComponentActivity` — a third activity, hosting nothing this suite
 * cares about. The empty rule launches nothing; the activities are started with plain intents and
 * the rule supplies only the semantics interaction and the waiting primitives. That works because
 * Compose registers *every* composition root in the process, so `onNodeWithTag` resolves across the
 * task boundary once the console has focus.
 *
 * **Why this module and not `:alohomora`.** `androidTest` compiles against the tested debug variant,
 * which binds the real `:alohomora` and — because the Alohomora Gradle plugin is applied to
 * `showcaseApp` for the `debug` variant — a generated `AlohomoraConfig` reachable through
 * `ServiceLoader`. `:alohomora` applies no plugin to itself, so its own device tests always see a
 * null config. Config and Git History can therefore only be covered from here. The trade-off is
 * that only Alohomora's **public** API is visible: `Routes`, `AlohomoraApp` and the repositories are
 * `internal`, so the console is driven by intent and by tapping Overview's module cards, never by a
 * start destination.
 *
 * **The process is not restarted between tests.** `startKoin` in [AndroidApp] runs once, the five
 * feature flags it records are recorded once, and Alohomora's Room database, `FeatureFlagStore` and
 * preference scan are all process-wide. Tests must tolerate what earlier tests left behind: assert
 * that a thing is present, not that it is the only thing present.
 */
abstract class ShowcaseE2ETest {

    /**
     * `AppActivity.onCreate` asks for `POST_NOTIFICATIONS` on API 33+, and the system dialog it
     * raises sits over the app and swallows the first taps. Granting up front skips the request
     * entirely. Empty below API 33, where the permission does not exist and `pm grant` would fail.
     */
    @get:Rule(order = 0)
    val notifications: GrantPermissionRule = GrantPermissionRule.grant(*NOTIFICATION_PERMISSION)

    @get:Rule(order = 1)
    val session: ShowcaseSessionRule = ShowcaseSessionRule()

    @get:Rule(order = 2)
    val compose: ComposeTestRule = createEmptyComposeRule()

    private companion object {
        val NOTIFICATION_PERMISSION: Array<String> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                emptyArray()
            }
    }
}

/**
 * Returns the process to a known state before each test.
 *
 * Both activities outlive the test that launched them, and both are launch modes that *reuse* an
 * existing instance rather than recreating it — a second `startActivity` on a live
 * `DevToolsActivity` delivers `onNewIntent`, which updates its start-destination field but cannot
 * recompose a `setContent` that already ran. So a test would inherit whatever screen its predecessor
 * left open. Finishing first is the only way the next launch lands on Overview.
 *
 * Done on the way in, not in `@After`: a test that fails mid-way skips its own cleanup, and the
 * next test would then inherit exactly the state the failure produced.
 */
class ShowcaseSessionRule : TestRule {

    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                finishLingeringActivities()
                // Installed unconditionally at init; a device jostled mid-run would otherwise
                // launch DevToolsActivity over whatever the test was asserting against.
                Alohomora.setShakeToOpenEnabled(false)
                base.evaluate()
            }
        }
}

internal const val UI_TIMEOUT_MILLIS: Long = 10_000L

/**
 * Ceiling for anything that waits on `https://jsonplaceholder.typicode.com`.
 *
 * These tests deliberately do not stub the network — capturing a real request is the thing under
 * test — so they need connectivity, and the budget covers a slow round trip plus the write that
 * follows it. Every `Alohomora.recordX` is fire-and-forget on `Dispatchers.Default`, so even a local
 * call is not visible when the call that produced it returns.
 */
internal const val NETWORK_TIMEOUT_MILLIS: Long = 45_000L

private const val POLL_INTERVAL_MILLIS: Long = 100L

private val LIVE_STAGES = listOf(
    Stage.CREATED,
    Stage.STARTED,
    Stage.RESUMED,
    Stage.PAUSED,
    Stage.STOPPED,
    Stage.RESTARTED,
)

internal fun ComposeTestRule.awaitTag(tag: String, timeoutMillis: Long = UI_TIMEOUT_MILLIS) {
    awaitCondition(timeoutMillis) { onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
}

internal fun ComposeTestRule.awaitNoTag(tag: String, timeoutMillis: Long = UI_TIMEOUT_MILLIS) {
    awaitCondition(timeoutMillis) { onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty() }
}

internal fun ComposeTestRule.awaitText(text: String, timeoutMillis: Long = UI_TIMEOUT_MILLIS) {
    awaitCondition(timeoutMillis) {
        onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
    }
}

internal fun ComposeTestRule.awaitNode(
    matcher: SemanticsMatcher,
    timeoutMillis: Long = UI_TIMEOUT_MILLIS,
    useUnmergedTree: Boolean = false,
) {
    awaitCondition(timeoutMillis) {
        onAllNodes(matcher, useUnmergedTree).fetchSemanticsNodes().isNotEmpty()
    }
}

/**
 * The `runCatching` is load-bearing, not defensive.
 *
 * With an empty compose rule there is a window between `startActivity` and the activity's compose
 * view attaching during which the process has no composition root at all, and `fetchSemanticsNodes`
 * throws rather than returning nothing. Treating that as "not yet" is what lets the very first wait
 * after a launch be a poll instead of a race.
 */
private fun ComposeTestRule.awaitCondition(timeoutMillis: Long, condition: () -> Boolean) {
    waitUntil(timeoutMillis) { runCatching(condition).getOrDefault(false) }
}

internal fun ComposeTestRule.launchShowcaseApp() {
    startActivity(AppActivity::class.java)
    awaitTag(ShowcaseTestTags.POSTS_LIST)
}

/**
 * Opens the console on Overview.
 *
 * `DevToolsActivity.newIntent(context, trafficId)` is the only public deep link into the console,
 * and it lands on a traffic detail screen for an id these tests cannot know in advance —
 * `TrafficNotificationHelper.EXTRA_START_DESTINATION` is `internal` to `:alohomora`. So every
 * screen is reached the way a user reaches it: from Overview's module grid.
 */
internal fun ComposeTestRule.openConsole() {
    startActivity(DevToolsActivity::class.java)
    awaitTag(Overview.GRID)
}

/**
 * Taps Refresh and returns as soon as the request is in flight.
 *
 * `PostsViewModel.init` already fires one refresh, so this is the app's *second* fetch. Nothing here
 * waits for it to finish: the request, the spans and the database write all land asynchronously, and
 * the assertion that follows is what waits for the one it cares about.
 */
internal fun ComposeTestRule.refreshPosts() {
    onNodeWithTag(ShowcaseTestTags.REFRESH).performClick()
    waitForIdle()
}

/** [routeKey] is the module's route simple name, e.g. `Traffic`, `Error`, `GitHistory`. */
internal fun ComposeTestRule.openModule(routeKey: String) {
    val card = Overview.moduleCard(routeKey)
    // The grid is lazy and does not fit one screen: an off-screen card is absent from the semantics
    // tree, not merely undisplayed.
    onNodeWithTag(Overview.GRID).performScrollToNode(hasTestTag(card))
    onNodeWithTag(card).performClick()
    waitForIdle()
}

private fun startActivity(activity: Class<out Activity>) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    context.startActivity(
        Intent(context, activity).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

/**
 * The editable node inside a tagged `AlohomoraTextField`.
 *
 * The caller's modifier — and so the test tag — lands on the wrapping `Column`, while the `SetText`
 * action belongs to the `BasicTextField` beneath it, and Compose does not merge an editable field's
 * semantics into its parent. The console's search boxes and the SQL editor all share that shape.
 * The showcase app's own fields are plain Material 3 `TextField`s tagged directly, so they take
 * `onNodeWithTag(...).performTextInput(...)` without this.
 */
internal fun ComposeTestRule.onTextFieldIn(tag: String): SemanticsNodeInteraction =
    onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(tag)), useUnmergedTree = true)

private fun finishLingeringActivities() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val deadline = SystemClock.uptimeMillis() + UI_TIMEOUT_MILLIS
    while (true) {
        val live = liveActivities()
        if (live.isEmpty()) return
        instrumentation.runOnMainSync { live.forEach(Activity::finish) }
        instrumentation.waitForIdleSync()
        // Returns rather than throwing on timeout: a lingering activity degrades the next test's
        // starting point, but failing here would report the previous test's problem against this one.
        if (SystemClock.uptimeMillis() > deadline) return
        Thread.sleep(POLL_INTERVAL_MILLIS)
    }
}

private fun liveActivities(): List<Activity> {
    var activities: List<Activity> = emptyList()
    // getActivitiesInStage is main-thread only.
    InstrumentationRegistry.getInstrumentation().runOnMainSync {
        val monitor = ActivityLifecycleMonitorRegistry.getInstance()
        activities = LIVE_STAGES.flatMap { monitor.getActivitiesInStage(it) }.distinct()
    }
    return activities
}
