package io.github.yashkasera.alohomora.device

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.github.yashkasera.alohomora.common.Error
import io.github.yashkasera.alohomora.device.Seed.error
import io.github.yashkasera.alohomora.device.Seed.seedErrors
import io.github.yashkasera.alohomora.domain.repository.ErrorRepository
import io.github.yashkasera.alohomora.presentation.navigation.Routes
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.Chrome
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.ErrorDetails
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.Errors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class ErrorScreenTest {

    @get:Rule(order = 0)
    val console = ConsoleTestRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    @Test
    fun emptyStateWhenNothingRecorded() {
        compose.launchConsole(Routes.Error)

        compose.onNodeWithTag(Chrome.EMPTY_STATE).assertIsDisplayed()
        compose.onNodeWithTag(Chrome.EMPTY_STATE_TITLE).assertTextContains("No Errors Recorded")
    }

    @Test
    fun seededErrorsShowTheExceptionTypeAndPlace() {
        console.seedErrors(
            error(
                reason = "java.lang.IllegalStateException: boom",
                place = "com.example.Checkout.submit(Checkout.kt:31)",
            ),
        )
        val id = savedErrorIds().single()

        compose.launchConsole(Routes.Error)

        compose.onNodeWithTag(Errors.item(id))
            .assertTextContains("IllegalStateException", substring = true)
        compose.onNodeWithTag(Errors.item(id))
            .assertTextContains("com.example.Checkout.submit(Checkout.kt:31)", substring = true)
    }

    /**
     * `ErrorDao.list` filters across `reason`, `place` and `stackTrace`. The fixtures differ in
     * their place so a search by package name narrows the list.
     */
    @Test
    fun searchFiltersAcrossReasonPlaceAndStackTrace() {
        console.seedErrors(
            error(
                reason = "java.lang.IllegalStateException: boom",
                place = "com.example.Checkout.submit(Checkout.kt:31)",
                index = 0,
                stackTrace = "java.lang.IllegalStateException: boom\n\tat com.example.Checkout.submit(Checkout.kt:31)",
            ),
            error(
                reason = "java.io.IOException: offline",
                place = "com.example.Sync.push(Sync.kt:12)",
                index = 1,
                stackTrace = "java.io.IOException: offline\n\tat com.example.Sync.push(Sync.kt:12)",
            ),
        )
        val ids = savedErrorIdsByReason()

        compose.launchConsole(Routes.Error)
        compose.onTextFieldIn(Chrome.SEARCH).performTextInput("Checkout")
        compose.waitForIdle()

        compose.onNodeWithTag(Errors.item(ids.getValue("java.lang.IllegalStateException: boom")))
            .assertIsDisplayed()
        compose.onNodeWithTag(Errors.item(ids.getValue("java.io.IOException: offline")))
            .assertDoesNotExist()
    }

    /**
     * The FATAL chip is unconditional in `ErrorListItem` — [Error] carries no fatality flag and
     * nothing in the row derives one, so every row gets the badge whether it came from the crash
     * handler or from `recordError`. Asserted as it is, not as it reads.
     */
    @Test
    fun everyErrorRowCarriesTheFatalBadge() {
        console.seedErrors(error(reason = "java.lang.IllegalStateException: boom"))
        val id = savedErrorIds().single()

        compose.launchConsole(Routes.Error)

        compose.onNodeWithTag(Errors.item(id)).assertTextContains("FATAL", substring = true)
    }

    /** Unlike Traffic and Events, this screen clears immediately — there is no confirmation sheet. */
    @Test
    fun clearAllEmptiesTheListWithoutConfirming() {
        console.seedErrors(
            error(reason = "java.lang.IllegalStateException: boom", index = 0),
            error(reason = "java.io.IOException: offline", index = 1),
        )

        compose.launchConsole(Routes.Error)
        compose.onNodeWithTag(Chrome.CLEAR_ALL).performClick()

        // `awaitTag`, not `waitForIdle()`: clearing deletes through Room on the IO dispatcher and
        // the list only empties when the query flow re-emits, which the Compose clock cannot see.
        compose.awaitTag(Chrome.EMPTY_STATE)
    }

    @Test
    fun tappingAnErrorOpensItsDetails() {
        console.seedErrors(
            error(
                reason = "java.lang.IllegalStateException: boom",
                place = "com.example.Checkout.submit(Checkout.kt:31)",
            ),
        )
        val id = savedErrorIds().single()

        compose.launchConsole(Routes.Error)
        compose.onNodeWithTag(Errors.item(id)).performClick()
        // The details screen shows a spinner until its Room flow emits, and only then does the top
        // bar have a subtitle to assert against. `waitForIdle` alone can win that race.
        compose.waitUntil(WAIT_MILLIS) {
            compose.onAllNodesWithTag(ErrorDetails.STACK_TRACE).fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithTag(Chrome.TOP_BAR_SUBTITLE)
            .assertTextContains("com.example.Checkout.submit(Checkout.kt:31)", substring = true)
    }

    /**
     * `ErrorRepositoryImpl.save` returns the pre-insert `id` (`0`), never the one SQLite assigned,
     * and the database file survives the process — so ids are neither knowable up front nor
     * restarted at 1. Read them back.
     */
    private fun savedErrorIds(): List<Long> = runBlocking {
        listErrors().map { it.id }
    }

    private fun savedErrorIdsByReason(): Map<String, Long> = runBlocking {
        listErrors().associate { it.reason.orEmpty() to it.id }
    }

    private suspend fun listErrors(): List<Error> =
        console.koin.get<ErrorRepository>()
            .list(query = "", page = 0, pageSize = 50)
            .first()

    private companion object {
        const val WAIT_MILLIS = 5_000L
    }
}
