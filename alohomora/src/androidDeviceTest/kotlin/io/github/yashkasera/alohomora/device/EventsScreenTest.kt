package io.github.yashkasera.alohomora.device

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.github.yashkasera.alohomora.common.Event
import io.github.yashkasera.alohomora.device.Seed.event
import io.github.yashkasera.alohomora.device.Seed.seedEvents
import io.github.yashkasera.alohomora.domain.repository.EventsRepository
import io.github.yashkasera.alohomora.presentation.navigation.Routes
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.Chrome
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.Events
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class EventsScreenTest {

    @get:Rule(order = 0)
    val console = ConsoleTestRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    @Test
    fun emptyStateWhenNothingCaptured() {
        compose.launchConsole(Routes.Events)

        compose.onNodeWithTag(Chrome.EMPTY_STATE).assertIsDisplayed()
        compose.onNodeWithTag(Chrome.EMPTY_STATE_TITLE).assertTextContains("No Events Yet")
    }

    @Test
    fun seededEventsAreListed() {
        console.seedEvents(
            event(name = "Checkout.Started", index = 0),
            event(name = "Checkout.Completed", index = 1),
        )
        val byName = savedEventIdsByName()

        compose.launchConsole(Routes.Events)

        compose.onNodeWithTag(Events.item(byName.getValue("Checkout.Started")))
            .assertTextContains("Checkout.Started", substring = true)
        compose.onNodeWithTag(Events.item(byName.getValue("Checkout.Completed")))
            .assertTextContains("Checkout.Completed", substring = true)
    }

    @Test
    fun searchNarrowsByEventName() {
        console.seedEvents(
            event(name = "Checkout.Started", index = 0),
            event(name = "Session.Resumed", index = 1),
        )
        val byName = savedEventIdsByName()

        compose.launchConsole(Routes.Events)
        compose.onTextFieldIn(Chrome.SEARCH).performTextInput("Session")
        compose.waitForIdle()

        compose.onNodeWithTag(Events.item(byName.getValue("Session.Resumed"))).assertIsDisplayed()
        compose.onNodeWithTag(Events.item(byName.getValue("Checkout.Started"))).assertDoesNotExist()
    }

    @Test
    fun propertiesPreviewIsShownByDefault() {
        console.seedEvents(
            event(name = "Checkout.Started", properties = mapOf("cartId" to "c-77")),
        )

        compose.launchConsole(Routes.Events)

        compose.onNodeWithText("cartId", substring = true, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun propertiesPreviewIsHiddenOnceTheEyeIsToggled() {
        console.seedEvents(
            event(name = "Checkout.Started", properties = mapOf("cartId" to "c-77")),
        )

        compose.launchConsole(Routes.Events)
        compose.onNodeWithTag(Events.PROPERTIES_TOGGLE).performClick()
        compose.waitForIdle()

        compose.onNodeWithText("cartId", substring = true, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun tappingAnEventOpensItsDetailsSheet() {
        console.seedEvents(event(name = "Checkout.Started"))
        val byName = savedEventIdsByName()

        compose.launchConsole(Routes.Events)
        compose.onNodeWithTag(Events.item(byName.getValue("Checkout.Started"))).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(Events.DETAILS_SHEET).assertIsDisplayed()
    }

    @Test
    fun clearAllEmptiesTheListOnceConfirmed() {
        console.seedEvents(
            event(name = "Checkout.Started"),
            event(name = "Session.Resumed", index = 1),
        )

        compose.launchConsole(Routes.Events)
        compose.onNodeWithTag(Chrome.CLEAR_ALL).performClick()
        compose.onNodeWithTag(Chrome.CONFIRM_ACCEPT).performClick()

        // `awaitTag`, not `waitForIdle()`: clearing deletes through Room on the IO dispatcher and
        // the list only empties when the query flow re-emits, which the Compose clock cannot see.
        compose.awaitTag(Chrome.EMPTY_STATE)
    }

    @Test
    fun cancellingClearAllKeepsTheEvents() {
        console.seedEvents(event(name = "Checkout.Started"))
        val byName = savedEventIdsByName()

        compose.launchConsole(Routes.Events)
        compose.onNodeWithTag(Chrome.CLEAR_ALL).performClick()
        compose.onNodeWithTag(Chrome.CONFIRM_DISMISS).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(Events.item(byName.getValue("Checkout.Started"))).assertIsDisplayed()
    }

    /**
     * Row tags are keyed by [Event.id], which is `autoGenerate` — and `EventsRepositoryImpl.save`
     * returns `item.id`, i.e. the pre-insert `0`, not the row id SQLite assigned. Seeded ids are
     * therefore unknowable up front and are *not* 1, 2, 3: the database file outlives the process,
     * so the sequence keeps climbing across runs. Read them back instead.
     */
    private fun savedEventIdsByName(): Map<String, Long> = runBlocking {
        console.koin.get<EventsRepository>()
            .list(query = "", page = 0, pageSize = 50)
            .first()
            .associate { it.name to it.id }
    }
}
