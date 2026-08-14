package io.github.yashkasera.alohomora.device

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import io.github.yashkasera.alohomora.DevToolsActivity
import io.github.yashkasera.alohomora.device.Seed.seedTraffic
import io.github.yashkasera.alohomora.device.Seed.traffic
import io.github.yashkasera.alohomora.traffic.TrafficNotificationHelper
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.Chrome
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.Overview
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.TrafficDetails
import org.junit.Rule
import org.junit.Test

/**
 * The real [DevToolsActivity], and the only `ActivityScenario` user in this source set.
 *
 * Every other console test hosts `AlohomoraApp` inside `ui-test-manifest`'s bare `ComponentActivity`
 * via [launchConsole], because `DevToolsActivity` is declared `launchMode="singleTask"` with its own
 * `taskAffinity` — a combination `ActivityScenario` drives poorly. What that hosting cannot reach is
 * the one thing this class covers: `updateStartDestination`, the mapping from intent extras to the
 * screen the console opens on.
 *
 * `createEmptyComposeRule()`, not `createComposeRule()`: the latter launches an activity of its own,
 * which is exactly what must not happen here.
 *
 * **If this proves flaky on device, delete it and unit-test `updateStartDestination` instead.** The
 * mapping is three branches of pure logic and deserves a cheaper test than a task-affinity dance.
 * Spreading `ActivityScenario` to the other classes to "make it consistent" is the wrong repair —
 * they are fast and stable precisely because they avoid it.
 */
class DevToolsActivityTest {

    @get:Rule(order = 0)
    val console = ConsoleTestRule()

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun launchingWithNoExtrasLandsOnOverview() {
        val scenario = ActivityScenario.launch(DevToolsActivity::class.java)
        try {
            compose.waitForIdle()
            compose.onNodeWithTag(Overview.GRID).assertIsDisplayed()
        } finally {
            scenario.close()
        }
    }

    @Test
    fun newIntentLandsOnTheTrafficDetailsForThatEntry() {
        // Seeded first: `updateStartDestination` maps the id to `Routes.TrafficDetails` regardless,
        // but the screen only renders a subtitle once the row exists.
        console.seedTraffic(traffic(id = "notified", path = "/v1/posts", index = 0))

        val scenario = ActivityScenario.launch<DevToolsActivity>(
            DevToolsActivity.newIntent(context, "notified"),
        )
        try {
            compose.waitForIdle()
            compose.onNodeWithTag(TrafficDetails.ROOT).assertIsDisplayed()
            compose.onNodeWithTag(Chrome.TOP_BAR_SUBTITLE)
                .assertTextContains("/v1/posts", substring = true)
        } finally {
            scenario.close()
        }
    }

    @Test
    fun theStartDestinationExtraAloneLandsOnTheTrafficList() {
        // What the traffic notification's content intent carries: a destination and no id. The
        // extra's value is the string "traffic" — the constant, not a `Routes` name.
        val intent = Intent(context, DevToolsActivity::class.java)
            .putExtra(
                TrafficNotificationHelper.EXTRA_START_DESTINATION,
                TrafficNotificationHelper.DESTINATION_TRACE,
            )

        val scenario = ActivityScenario.launch<DevToolsActivity>(intent)
        try {
            compose.waitForIdle()
            compose.onNodeWithTag(Chrome.TOP_BAR_TITLE).assertTextContains("Traffic")
        } finally {
            scenario.close()
        }
    }
}
