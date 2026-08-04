package io.github.yashkasera.alohomora.desktop.data.local

import io.github.yashkasera.alohomora.common.Event
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Captured telemetry events, **newest first** — matching [TrafficStore].
 *
 * As there, the device's snapshot arrives `ORDER BY time DESC` while [append] used to add to the
 * end, so streamed events piled up *below* an already-descending snapshot.
 */
class EventStore {
    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()

    /**
     * Inserts [event] at the top.
     *
     * Unlike a trace an event is immutable once recorded, so there is nothing to upsert — but
     * duplicates are skipped anyway so redelivery of the same frame stays idempotent.
     */
    fun append(event: Event) {
        val current = _events.value
        if (current.any { it.id == event.id && it.time == event.time }) return
        // take, not takeLast: newest is at the head now, so the tail holds the oldest.
        _events.value = (listOf(event) + current).take(MAX_ENTRIES)
    }

    /** Replaces everything, normalising to newest-first rather than trusting the wire order. */
    fun replace(events: List<Event>) {
        _events.value = events
            .sortedByDescending { it.time }
            .take(MAX_ENTRIES)
    }

    /** Dims an event opened in this window; see TrafficStore.markViewed for why this stays local. */
    fun markViewed(id: Long) {
        val current = _events.value
        val index = current.indexOfFirst { it.id == id }
        if (index < 0 || current[index].isViewed) return
        _events.value = current.toMutableList().also { list ->
            list[index] = list[index].copy(isViewed = true)
        }
    }

    fun clear() {
        _events.value = emptyList()
    }

    companion object {
        /**
         * Not private: the panel subtitle names this number when the store is full. The tail is dropped
         * with no other signal, so a total that stalls at exactly the cap would otherwise be
         * indistinguishable from a stalled stream.
         */
        const val MAX_ENTRIES = 2000
    }
}
