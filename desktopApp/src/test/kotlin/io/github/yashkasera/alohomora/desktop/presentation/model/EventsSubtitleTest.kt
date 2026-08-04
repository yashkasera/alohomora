package io.github.yashkasera.alohomora.desktop.presentation.model

import io.github.yashkasera.alohomora.desktop.data.local.EventStore
import io.github.yashkasera.alohomora.desktop.presentation.model.EventsFilterTestData.event
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The panel subtitle, which is the only place the store's silent truncation becomes visible. */
class EventsSubtitleTest {

    private fun state(total: Int, shown: Int, atCap: Boolean = false) = EventsUiState(
        events = (1..shown).map { event(id = it.toLong()) },
        totalCount = total,
        atStoreCap = atCap,
    )

    @Test
    fun `the shown count is omitted when nothing is filtered`() {
        assertEquals("12 events", eventsSubtitle(state(total = 12, shown = 12)))
    }

    @Test
    fun `the shown count appears when filters hide something`() {
        assertEquals("412 events · 38 shown", eventsSubtitle(state(total = 412, shown = 38)))
    }

    @Test
    fun `the store cap is named when the store is full`() {
        // Without this clause a total frozen at exactly the cap is indistinguishable from a stream that
        // stopped arriving.
        val subtitle = eventsSubtitle(
            state(total = EventStore.MAX_ENTRIES, shown = EventStore.MAX_ENTRIES, atCap = true),
        )

        assertTrue(subtitle.contains("oldest dropped at ${EventStore.MAX_ENTRIES}"), subtitle)
    }

    @Test
    fun `an empty panel still reports a count`() {
        assertEquals("0 events", eventsSubtitle(EventsUiState()))
    }
}
