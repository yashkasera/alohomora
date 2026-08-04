package io.github.yashkasera.alohomora.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import io.github.yashkasera.alohomora.desktop.presentation.model.EventsFilterState
import io.github.yashkasera.alohomora.desktop.presentation.model.EventsUiState
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.EventsFilters
import io.github.yashkasera.alohomora.ui.theme.AlohomoraTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Behavioural tests for the Events filter row, deliberately few.
 *
 * The predicate, the mute algebra and the time arithmetic are all covered by fast unit tests in
 * `presentation/model` — these cover only what a real composition can show: that the controls needed to
 * *undo* a filter actually render. That is the half a pure test cannot reach, and the half where a
 * mistake strands the user with a list they cannot get back.
 *
 * No `EventsPanel` test: it takes an `EventsViewModel`, which takes a `DevToolsRepository` — a fake of
 * roughly thirty members for coverage the pure tests already have.
 */
@OptIn(ExperimentalTestApi::class)
class EventsFiltersTest {

    private fun state(
        filters: EventsFilterState = EventsFilterState(),
        nameCounts: Map<String, Int> = emptyMap(),
    ) = EventsUiState(
        totalCount = nameCounts.values.sum(),
        nameCounts = nameCounts,
        names = nameCounts.entries.sortedByDescending { it.value }.map { it.key },
        filters = filters,
    )

    /**
     * A muted name must keep its chip.
     *
     * The chip is the only per-name way back, so deriving this row from the *filtered* list — the
     * obvious implementation — would delete the control the user needs precisely when they need it.
     */
    @Test
    fun `a muted name keeps its chip so it can be unmuted`() = runComposeUiTest {
        var toggled: String? = null

        setContent {
            AlohomoraTheme {
                EventsFilters(
                    state = state(
                        filters = EventsFilterState(mutedNames = setOf("Screen.View")),
                        nameCounts = mapOf("Screen.View" to 40, "App.Start" to 1),
                    ),
                    onQueryChange = {},
                    onUnreadOnlyChange = {},
                    onWindowChange = {},
                    onMark = {},
                    onClearMark = {},
                    onToggleMute = { toggled = it },
                    onUnmuteAll = {},
                    onClearFilters = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // Pre-mute count: the number that decides whether muting a name helps is how much of the stream
        // it is, not how much survived the filters.
        onNodeWithText("Screen.View · 40").assertIsDisplayed()

        onNodeWithText("Screen.View · 40").performClick()
        waitForIdle()

        assertEquals("Screen.View", toggled)
    }

    @Test
    fun `the unmute all affordance appears only when something is muted`() = runComposeUiTest {
        val muted = mutableListOf<Boolean>()

        setContent {
            AlohomoraTheme {
                EventsFilters(
                    state = state(nameCounts = mapOf("App.Start" to 2)),
                    onQueryChange = {},
                    onUnreadOnlyChange = {},
                    onWindowChange = {},
                    onMark = {},
                    onClearMark = {},
                    onToggleMute = {},
                    onUnmuteAll = { muted += true },
                    onClearFilters = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        assertTrue(
            onAllNodesWithText("unmute all", substring = true).fetchSemanticsNodes().isEmpty(),
            "offered an unmute with nothing muted",
        )
        assertTrue(muted.isEmpty())
    }

    @Test
    fun `a muted set is reported with a one-click way out`() = runComposeUiTest {
        var unmutedAll = false

        setContent {
            AlohomoraTheme {
                EventsFilters(
                    state = state(
                        filters = EventsFilterState(mutedNames = setOf("Screen.View", "Api.Call")),
                        nameCounts = mapOf("Screen.View" to 4, "Api.Call" to 2),
                    ),
                    onQueryChange = {},
                    onUnreadOnlyChange = {},
                    onWindowChange = {},
                    onMark = {},
                    onClearMark = {},
                    onToggleMute = {},
                    onUnmuteAll = { unmutedAll = true },
                    onClearFilters = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        onNodeWithText("2 muted · unmute all").performClick()
        waitForIdle()

        assertTrue(unmutedAll, "the muted count must be its own way out")
    }

    /**
     * Pinning a floor has to look reversible.
     *
     * "Hide older" sits one row below a Trash that really does delete on the device, so the state it
     * produces must read as a dismissible filter rather than as something that happened to the data.
     */
    @Test
    fun `a pinned floor replaces the button with a dismissible chip`() = runComposeUiTest {
        var cleared = false

        setContent {
            AlohomoraTheme {
                EventsFilters(
                    // 1970-01-01T00:00:10Z, so the rendered time is stable regardless of the host clock.
                    state = state(filters = EventsFilterState(markFloorMillis = 10_000)),
                    onQueryChange = {},
                    onUnreadOnlyChange = {},
                    onWindowChange = {},
                    onMark = {},
                    onClearMark = { cleared = true },
                    onToggleMute = {},
                    onUnmuteAll = {},
                    onClearFilters = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        assertTrue(
            onAllNodesWithText("Hide older", substring = true, ignoreCase = true)
                .fetchSemanticsNodes().isEmpty(),
            "the button must give way to the chip once a floor is pinned",
        )

        val chip = onAllNodesWithText("Since ", substring = true).fetchSemanticsNodes()
        assertTrue(chip.isNotEmpty(), "a pinned floor must say when it was pinned")

        onAllNodesWithText("Since ", substring = true)[0].performClick()
        waitForIdle()
        assertTrue(cleared, "the chip must clear the floor it reports")
    }

    @Test
    fun `the chip row is absent until an event has been seen`() = runComposeUiTest {
        setContent {
            AlohomoraTheme {
                EventsFilters(
                    state = state(),
                    onQueryChange = {},
                    onUnreadOnlyChange = {},
                    onWindowChange = {},
                    onMark = {},
                    onClearMark = {},
                    onToggleMute = {},
                    onUnmuteAll = {},
                    onClearFilters = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // The window toggle group is always there; the per-name row is not. Not uppercased, because
        // "30s" would otherwise render as "30S".
        onNodeWithText("All").assertIsDisplayed()
        assertTrue(
            onAllNodesWithText(" · ", substring = true).fetchSemanticsNodes().isEmpty(),
            "rendered a name chip row with no names",
        )
    }
}
