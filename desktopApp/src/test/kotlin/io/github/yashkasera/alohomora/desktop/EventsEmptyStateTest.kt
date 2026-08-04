package io.github.yashkasera.alohomora.desktop

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import io.github.yashkasera.alohomora.desktop.presentation.model.EventsFilterState
import io.github.yashkasera.alohomora.desktop.presentation.model.EventsTimeWindow
import io.github.yashkasera.alohomora.desktop.presentation.model.EventsUiState
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.EventsEmptyState
import io.github.yashkasera.alohomora.ui.theme.AlohomoraTheme
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The three reasons the Events panel can be empty.
 *
 * Worth a real composition because each branch offers a *different* action, and the wrong branch sends
 * the reader looking for the wrong problem. The muted case exists at all because mutes are the one
 * filter that survives a restart: a generic "no events match" against a set muted days ago reads as a
 * dead connection, which is exactly the failure that keeps every other filter out of `Preferences`.
 */
@OptIn(ExperimentalTestApi::class)
class EventsEmptyStateTest {

    @Test
    fun `nothing captured yet points at the app rather than the filters`() = runComposeUiTest {
        setContent {
            AlohomoraTheme {
                EventsEmptyState(state = EventsUiState(), onUnmuteAll = {}, onClearFilters = {})
            }
        }

        onNodeWithText("No events yet").assertIsDisplayed()
        assertTrue(
            onAllNodesWithText("filter", substring = true, ignoreCase = true)
                .fetchSemanticsNodes().isEmpty(),
            "blamed the filters when nothing had been captured",
        )
    }

    @Test
    fun `an all-muted list names the mute count and offers an unmute`() = runComposeUiTest {
        var unmuted = false

        setContent {
            AlohomoraTheme {
                EventsEmptyState(
                    state = EventsUiState(
                        totalCount = 120,
                        filters = EventsFilterState(mutedNames = setOf("Screen.View", "Api.Call")),
                    ),
                    onUnmuteAll = { unmuted = true },
                    onClearFilters = {},
                )
            }
        }

        onNodeWithText("Every event is muted").assertIsDisplayed()
        // Both numbers matter: how many names are muted, and how much they are hiding.
        onNodeWithText("2 names are muted", substring = true).assertIsDisplayed()
        onNodeWithText("120", substring = true).assertIsDisplayed()

        onNodeWithText("Unmute all", ignoreCase = true).performClick()
        waitForIdle()
        assertTrue(unmuted)
    }

    @Test
    fun `a transient filter takes precedence over the mute message`() = runComposeUiTest {
        var cleared = false

        setContent {
            AlohomoraTheme {
                EventsEmptyState(
                    // Mutes AND a query. The query is what the user just typed, so that is the filter
                    // they will think to clear first.
                    state = EventsUiState(
                        totalCount = 120,
                        filters = EventsFilterState(
                            query = "checkout",
                            mutedNames = setOf("Screen.View"),
                            window = EventsTimeWindow.LastMinute,
                        ),
                    ),
                    onUnmuteAll = {},
                    onClearFilters = { cleared = true },
                )
            }
        }

        onNodeWithText("No events match").assertIsDisplayed()
        onNodeWithText("Clear filters", ignoreCase = true).performClick()
        waitForIdle()
        assertTrue(cleared)
    }
}
