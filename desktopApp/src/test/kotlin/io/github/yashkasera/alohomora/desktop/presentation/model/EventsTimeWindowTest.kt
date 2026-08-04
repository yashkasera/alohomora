package io.github.yashkasera.alohomora.desktop.presentation.model

import io.github.yashkasera.alohomora.desktop.presentation.model.EventsFilterTestData.event
import io.github.yashkasera.alohomora.desktop.presentation.model.EventsFilterTestData.matches
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The time filter.
 *
 * `floorAt` exists as a separate function precisely so this is reachable without virtual time — the
 * ticker around it is then a loop with nothing left to get wrong.
 */
class EventsTimeWindowTest {

    private val now = 1_000_000L

    @Test
    fun `the all window applies no floor`() {
        // Null rather than a very large window, so no ticker is started at all in this state.
        assertNull(EventsTimeWindow.All.floorAt(now))
    }

    @Test
    fun `a thirty second window puts the floor thirty seconds back`() {
        assertEquals(now - 30_000, EventsTimeWindow.Last30Seconds.floorAt(now))
    }

    @Test
    fun `every window except all yields a floor`() {
        EventsTimeWindow.entries
            .filter { it != EventsTimeWindow.All }
            .forEach { window ->
                assertTrue(
                    window.floorAt(now)!! < now,
                    "${window.name} did not put its floor in the past",
                )
            }
    }

    @Test
    fun `an event exactly on the floor is shown`() {
        // Pins `<` against `<=`. A "last 30s" window that drops the event at exactly 30s is off by one
        // in the direction that loses data.
        assertTrue(matches(event(time = 500), floorMillis = 500))
    }

    @Test
    fun `an event one millisecond older than the floor is hidden`() {
        assertFalse(matches(event(time = 499), floorMillis = 500))
    }

    @Test
    fun `a null floor admits an ancient event`() {
        assertTrue(matches(event(time = 0), floorMillis = null))
    }

    @Test
    fun `mark and a rolling window compose to the stricter floor`() {
        // Both mean "hide older than X", so they intersect rather than override. This is the max the
        // view model computes; asserting it here keeps the rule out of the flow.
        val rollingFloor = 500L
        val markFloor = 900L
        val effective = listOfNotNull(rollingFloor, markFloor).maxOrNull()

        assertEquals(900L, effective)
        assertFalse(matches(event(time = 700), floorMillis = effective))
        assertTrue(matches(event(time = 950), floorMillis = effective))
    }

    @Test
    fun `a mark floor alone still filters when the window is all`() {
        val filters = EventsFilterState(window = EventsTimeWindow.All, markFloorMillis = 900)
        val effective = listOfNotNull(filters.window.floorAt(now), filters.markFloorMillis).maxOrNull()

        assertEquals(900L, effective)
        assertFalse(matches(event(time = 800), filters, effective))
    }

    @Test
    fun `a mark floor counts as a transient filter but a mute does not`() {
        // Drives the empty state: mutes persist across restarts and need their own message, so
        // hasTransientFilter deliberately ignores them.
        assertTrue(EventsFilterState(markFloorMillis = 1).hasTransientFilter)
        assertTrue(EventsFilterState(query = "x").hasTransientFilter)
        assertTrue(EventsFilterState(unreadOnly = true).hasTransientFilter)
        assertTrue(EventsFilterState(window = EventsTimeWindow.LastMinute).hasTransientFilter)
        assertFalse(EventsFilterState(mutedNames = setOf("a")).hasTransientFilter)
    }
}
