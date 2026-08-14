package io.github.yashkasera.alohomora.device

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import io.github.yashkasera.alohomora.presentation.navigation.Routes
import io.github.yashkasera.alohomora.presentation.ui.AlohomoraApp

/**
 * Hosts the console inside the test's own activity.
 *
 * `createComposeRule()` launches `ui-test-manifest`'s bare `ComponentActivity`, which sidesteps
 * the real [io.github.yashkasera.alohomora.DevToolsActivity] — declared `launchMode="singleTask"`
 * with its own `taskAffinity`, a combination `ActivityScenario` drives poorly. `DevToolsActivity`
 * itself is still covered, by exactly one test (`DevToolsActivityTest`), which is enough for what
 * that class actually adds: `enableEdgeToEdge` and the intent-extra → start-destination mapping.
 *
 * [AlohomoraApp] and [Routes] are `internal`, and reachable here only because AGP puts the main
 * compilation's classes.jar on the device-test compilation's friend paths. That is what lets a test
 * open a detail screen directly with `Routes.TrafficDetails(id)` instead of clicking its way in
 * from Overview — a list test and a detail test then fail independently.
 *
 * `internal` because [Routes] is: friend access lets this source set *see* an internal type, not
 * re-export one from a public signature.
 */
internal fun ComposeContentTestRule.launchConsole(startDestination: Routes = Routes.Overview) {
    setContent {
        AlohomoraApp(startDestination = startDestination)
    }
    waitForIdle()
}
