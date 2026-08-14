package io.github.yashkasera.alohomora.showcaseApp

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.Errors
import org.junit.Test
import org.junit.runner.RunWith

/**
 * `Alohomora.recordError` from the app, read back on the console's Errors screen.
 *
 * Only [ShowcaseTestTags.RECORD_ERROR] is ever tapped. `ShowcaseTestTags.CRASH` throws uncaught on
 * the main thread, and the installed crash handler chains to the one it displaced — ART's — which
 * kills the process and takes the rest of the test class with it. The uncaught path is covered by
 * `CrashHandlerTest` in `:alohomora`'s `androidHostTest`, where the handler can be exercised without
 * a live process to lose.
 *
 * No network.
 */
@RunWith(AndroidJUnit4::class)
class ErrorCaptureE2ETest : ShowcaseE2ETest() {

    @Test
    fun aRecordedErrorAppearsOnTheErrorsScreen() {
        recordAnError()
        openErrors()

        compose.awaitNode(
            hasText("IllegalStateException", substring = true) and inErrorList(),
        )
    }

    /**
     * `PostsScreen` passes `place = "PostsScreen"` explicitly. Without it `toError` would fall back
     * to the throwable's top stack frame, so this pins the caller-supplied value rather than the
     * derived one.
     */
    @Test
    fun theErrorCarriesThePlaceTheAppReported() {
        recordAnError()
        openErrors()

        compose.awaitNode(hasText("PostsScreen", substring = true) and inErrorList())
    }

    private fun recordAnError() {
        compose.launchShowcaseApp()
        compose.onNodeWithTag(ShowcaseTestTags.RECORD_ERROR).performClick()
        compose.waitForIdle()
    }

    /** `Routes.Error`, not `Errors` — the grid keys its cards by the route's simple name. */
    private fun openErrors() {
        compose.openConsole()
        compose.openModule("Error")
        compose.awaitTag(Errors.LIST)
    }

    private fun inErrorList() = hasAnyAncestor(hasTestTag(Errors.LIST))
}
