package io.github.yashkasera.alohomora.desktop.presentation.model

import io.github.yashkasera.alohomora.common.Event
import kotlinx.serialization.json.JsonNull

/**
 * Builds the search text for each held event, at most once per event.
 *
 * There are two wrong answers this exists to avoid, and only one of them is obvious. Stringifying
 * `properties` inside the filter repeats it for the whole store on every keystroke. Stringifying it in
 * a plain `map` over the list repeats it for every *neighbour* on every streamed event, which on a
 * chatty app is the more expensive of the two. So the text is memoised and only genuinely new events
 * pay.
 *
 * Keyed on `id` **and** `time`, which is `EventStore.append`'s dedupe key and the panel's list key.
 * `id` alone is not enough twice over: `markViewed` copies the event so instance identity is out, and
 * a device switch restarts row ids from 1 with different payloads behind them.
 *
 * Not thread-safe, by design. Driven by the single `stateIn` collector in `EventsViewModel`; that is
 * why `indexed` is a `StateFlow` there rather than a cold `map` collected in two places.
 */
internal class EventSearchIndex {
    private var cache: Map<Key, String> = emptyMap()

    /** Haystacks actually built. Exposed only so a test can pin the memoisation. */
    internal var computations: Int = 0
        private set

    fun reindex(events: List<Event>): List<IndexedEvent> {
        val next = HashMap<Key, String>(events.size)
        val result = events.map { event ->
            val key = Key(event.id, event.time)
            val haystack = cache[key] ?: event.searchHaystack().also { computations++ }
            next[key] = haystack
            IndexedEvent(event, haystack)
        }
        // Rebuilt rather than pruned: an event the store evicted must not keep its text alive, or the
        // cache grows for the life of the window.
        cache = next
        return result
    }

    private data class Key(val id: Long, val time: Long)
}

/**
 * Name plus compact properties JSON, lowercased.
 *
 * Compact rather than pretty, unlike the rendered payload: newlines and indentation would only pad a
 * string a query scans. [JsonNull] is skipped so a search for "null" does not match every event
 * recorded without properties.
 */
internal fun Event.searchHaystack(): String = buildString(name.length + HAYSTACK_PADDING) {
    append(name)
    properties?.takeUnless { it is JsonNull }?.let {
        append(' ')
        append(it.toString())
    }
}.lowercase()

/** Rough headroom for a flat string map, so the common case does not resize the builder. */
private const val HAYSTACK_PADDING = 64
