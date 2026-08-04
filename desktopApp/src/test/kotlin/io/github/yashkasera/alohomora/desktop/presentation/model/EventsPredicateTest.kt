package io.github.yashkasera.alohomora.desktop.presentation.model

import io.github.yashkasera.alohomora.desktop.presentation.model.EventsFilterTestData.event
import io.github.yashkasera.alohomora.desktop.presentation.model.EventsFilterTestData.matches
import io.github.yashkasera.alohomora.desktop.presentation.model.EventsFilterTestData.nullProperties
import io.github.yashkasera.alohomora.desktop.presentation.model.EventsFilterTestData.properties
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The Events panel's filter, which is the whole reason the arithmetic lives outside the view model.
 */
class EventsPredicateTest {

    @Test
    fun `a blank query matches every event`() {
        assertTrue(matches(event(), EventsFilterState(query = "")))
    }

    @Test
    fun `the name matches case-insensitively`() {
        val target = event(name = "App.Exception")

        assertTrue(matches(target, EventsFilterState(query = "app.exc")))
        assertTrue(matches(target, EventsFilterState(query = "APP.EXC")))
    }

    @Test
    fun `a query matches a value inside the properties json`() {
        // The non-obvious half of the search, and the reason a haystack exists at all: an event's name
        // is often the least distinguishing thing about it.
        val target = event(properties = properties("screen" to "CheckoutScreen"))

        assertTrue(matches(target, EventsFilterState(query = "checkoutscreen")))
    }

    @Test
    fun `a query matches a property key`() {
        val target = event(properties = properties("cartValue" to "1200"))

        assertTrue(matches(target, EventsFilterState(query = "cartvalue")))
    }

    @Test
    fun `an event recorded without properties does not match the text null`() {
        // JsonNull stringifies to "null", so without the guard in searchHaystack a search for "null"
        // would return every property-less event.
        assertFalse(matches(event(properties = null), EventsFilterState(query = "null")))
        assertFalse(matches(event(properties = nullProperties()), EventsFilterState(query = "null")))
    }

    @Test
    fun `a query is trimmed before matching`() {
        // Trailing whitespace is what a paste leaves behind, and an untrimmed needle would match
        // nothing while looking like a typed query that should.
        assertTrue(matches(event(name = "App.Start"), EventsFilterState(query = "  app.start  ")))
    }

    @Test
    fun `unread only hides an event already viewed`() {
        val filters = EventsFilterState(unreadOnly = true)

        assertTrue(matches(event(viewed = false), filters))
        assertFalse(matches(event(viewed = true), filters))
    }

    @Test
    fun `a muted name is hidden even when it matches the query`() {
        // Mute wins over search on purpose: the mute is the standing instruction and the query is the
        // momentary one, so a search must not resurrect noise the user silenced.
        val target = event(name = "Screen.View")
        val filters = EventsFilterState(query = "screen", mutedNames = setOf("Screen.View"))

        assertFalse(matches(target, filters))
    }

    @Test
    fun `muting one name leaves its neighbours alone`() {
        val filters = EventsFilterState(mutedNames = setOf("Screen.View"))

        assertFalse(matches(event(name = "Screen.View"), filters))
        assertTrue(matches(event(name = "Screen.Viewed"), filters))
    }
}
