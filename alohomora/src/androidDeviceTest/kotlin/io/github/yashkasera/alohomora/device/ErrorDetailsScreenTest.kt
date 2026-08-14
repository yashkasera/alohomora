package io.github.yashkasera.alohomora.device

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import io.github.yashkasera.alohomora.common.Error
import io.github.yashkasera.alohomora.device.Seed.error
import io.github.yashkasera.alohomora.device.Seed.seedErrors
import io.github.yashkasera.alohomora.domain.repository.ErrorRepository
import io.github.yashkasera.alohomora.presentation.navigation.Routes
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.Chrome
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.ErrorDetails
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class ErrorDetailsScreenTest {

    @get:Rule(order = 0)
    val console = ConsoleTestRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    @Test
    fun stackTraceRenders() {
        val id = seedOne(
            error(
                reason = "java.lang.IllegalStateException: boom",
                stackTrace = TRACE,
            ),
        )

        compose.launchDetails(id)

        compose.onNodeWithTag(ErrorDetails.STACK_TRACE).assertIsDisplayed()
    }

    @Test
    fun topBarTitleShowsTheExceptionType() {
        val id = seedOne(
            error(reason = "java.lang.IllegalStateException: boom", stackTrace = TRACE),
        )

        compose.launchDetails(id)

        compose.onNodeWithTag(Chrome.TOP_BAR_TITLE).assertTextEquals("IllegalStateException")
    }

    /**
     * Regression. `exceptionTypeName()` is `substringBefore(":")` *then* `substringAfterLast(".")`;
     * the inline version the two error screens used to carry had those the other way round, so a
     * period anywhere in the message — "1.5" here — reduced the title to a fragment of the message
     * or blanked it outright. Any reordering of those two calls fails this test and only this test.
     */
    @Test
    fun titleSurvivesAPeriodInTheExceptionMessage() {
        val id = seedOne(
            error(
                reason = "IllegalStateException: config value 1.5 is out of range",
                stackTrace = TRACE,
            ),
        )

        compose.launchDetails(id)

        compose.onNodeWithTag(Chrome.TOP_BAR_TITLE).assertTextEquals("IllegalStateException")
    }

    @Test
    fun copyActionIsAvailable() {
        val id = seedOne(
            error(reason = "java.lang.IllegalStateException: boom", stackTrace = TRACE),
        )

        compose.launchDetails(id)

        compose.onNodeWithTag(ErrorDetails.COPY).assertIsDisplayed().assertHasClickAction()
    }

    /**
     * Seeds one error and returns the id SQLite assigned. `ErrorRepositoryImpl.save` returns the
     * pre-insert `id` (`0`), and the database file outlives the process so the sequence never
     * restarts — the route argument has to come from a read-back, not a guess.
     */
    private fun seedOne(error: Error): Long {
        console.seedErrors(error)
        return runBlocking {
            console.koin.get<ErrorRepository>()
                .list(query = "", page = 0, pageSize = 1)
                .first()
                .single()
                .id
        }
    }

    /** The screen shows a spinner until its Room flow emits; every assertion here needs the row. */
    private fun ComposeContentTestRule.launchDetails(errorId: Long) {
        launchConsole(Routes.ErrorDetails(errorId))
        waitUntil(WAIT_MILLIS) {
            onAllNodesWithTag(ErrorDetails.ROOT).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        const val WAIT_MILLIS = 5_000L
        const val TRACE =
            "java.lang.IllegalStateException: boom\n\tat com.example.Checkout.submit(Checkout.kt:31)"
    }
}
