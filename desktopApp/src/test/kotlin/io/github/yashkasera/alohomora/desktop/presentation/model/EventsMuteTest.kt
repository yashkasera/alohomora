package io.github.yashkasera.alohomora.desktop.presentation.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The mute algebra.
 *
 * Hiding and filtering are one mechanism here — a single exclusion set — so these pin the inversion
 * that makes "show only this" fall out of it rather than needing a second code path.
 */
class EventsMuteTest {

    private val known = listOf("App.Start", "Screen.View", "Api.Call")

    @Test
    fun `solo mutes every known name except the soloed one`() {
        val result = EventsFilterState().withSolo("Screen.View", known)

        assertEquals(setOf("App.Start", "Api.Call"), result.mutedNames)
    }

    @Test
    fun `solo on the only known name mutes nothing`() {
        val result = EventsFilterState().withSolo("App.Start", listOf("App.Start"))

        assertTrue(result.mutedNames.isEmpty(), "soloing the only event hid everything")
    }

    @Test
    fun `solo keeps a mute on a name no longer held`() {
        // knownNames is what the store currently holds, so a name that has aged out must keep its mute
        // rather than quietly coming back the next time it fires.
        val existing = EventsFilterState(mutedNames = setOf("Gone.Event"))

        val result = existing.withSolo("Screen.View", known)

        assertTrue("Gone.Event" in result.mutedNames)
    }

    @Test
    fun `solo does not mute the soloed name even if it was already muted`() {
        // Otherwise "show only this" on a muted chip would produce an empty list, which is the one
        // outcome the action can never have meant.
        val existing = EventsFilterState(mutedNames = setOf("Screen.View"))

        val result = existing.withSolo("Screen.View", known)

        assertTrue("Screen.View" !in result.mutedNames)
    }

    @Test
    fun `toggling an unmuted name mutes it`() {
        assertEquals(setOf("App.Start"), EventsFilterState().withMuteToggled("App.Start").mutedNames)
    }

    @Test
    fun `toggling a muted name unmutes it`() {
        val existing = EventsFilterState(mutedNames = setOf("App.Start", "Api.Call"))

        val result = existing.withMuteToggled("App.Start")

        assertEquals(setOf("Api.Call"), result.mutedNames)
    }

    @Test
    fun `clearing mutes empties the set`() {
        val existing = EventsFilterState(mutedNames = setOf("App.Start"))

        assertTrue(existing.withMutesCleared().mutedNames.isEmpty())
    }

    @Test
    fun `clearing the transient filters keeps the mute set`() {
        // The whole point of the split: mutes are deliberate and persisted, the rest is momentary.
        val existing = EventsFilterState(
            query = "checkout",
            mutedNames = setOf("Screen.View"),
            unreadOnly = true,
            window = EventsTimeWindow.LastMinute,
            markFloorMillis = 5_000,
        )

        val result = existing.withTransientCleared()

        assertEquals(setOf("Screen.View"), result.mutedNames)
        assertEquals("", result.query)
        assertEquals(false, result.unreadOnly)
        assertEquals(EventsTimeWindow.All, result.window)
        assertEquals(null, result.markFloorMillis)
    }

    @Test
    fun `clearing mutes does not disturb the transient filters`() {
        val existing = EventsFilterState(query = "checkout", mutedNames = setOf("Screen.View"))

        val result = existing.withMutesCleared()

        assertEquals("checkout", result.query)
    }
}
