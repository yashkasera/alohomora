package io.github.yashkasera.alohomora.desktop.presentation.model

import io.github.yashkasera.alohomora.common.Event
import io.github.yashkasera.alohomora.desktop.data.local.EventStore

/**
 * Rolling window choices for the Events panel.
 *
 * [All] is the *absence* of a floor rather than a very large one, so it costs no ticker and no
 * arithmetic — see `EventsViewModel.rollingFloor`, which does not start one at all in this state.
 */
enum class EventsTimeWindow(val label: String, private val durationMillis: Long?) {
    All("All", null),
    Last30Seconds("30s", 30_000),
    LastMinute("1m", 60_000),
    Last5Minutes("5m", 300_000),
    Last15Minutes("15m", 900_000),
    ;

    /**
     * Oldest timestamp this window admits at [nowMillis], or null for [All].
     *
     * Pulled out of the ticker deliberately: this is the only arithmetic in the time filter, and with
     * it here the ticker is a loop with nothing left to get wrong and this is reachable from a plain
     * unit test without virtual time.
     */
    fun floorAt(nowMillis: Long): Long? = durationMillis?.let { nowMillis - it }
}

/**
 * Everything the user can set to narrow the Events list.
 *
 * One data class rather than loose `StateFlow`s for three reasons, in order of weight: `combine` tops
 * out at five typed sources and the indexed list plus the ticker already take two; "clear filters" and
 * a device switch change four fields at once, and through separate flows each write would refilter the
 * whole store and briefly paint a half-cleared state; and a one-argument predicate is what makes the
 * filter testable without a ViewModel. `showProperties` is deliberately *not* here — it changes
 * nothing about which events are shown, so folding it in would refilter on every toggle.
 */
data class EventsFilterState(
    val query: String = "",
    /** Names to hide. An exclusion set, so the default of empty shows everything. */
    val mutedNames: Set<String> = emptySet(),
    val unreadOnly: Boolean = false,
    val window: EventsTimeWindow = EventsTimeWindow.All,
    /**
     * Floor pinned by Mark. Never persisted — see
     * [io.github.yashkasera.alohomora.desktop.data.devtools.DesktopEventPrefs].
     */
    val markFloorMillis: Long? = null,
) {
    /**
     * Whether anything the user set *this session* is narrowing the list.
     *
     * Mutes are excluded on purpose: they persist, so they are the one filter that can already be
     * active on a fresh launch, and the empty state has to name them separately or an empty panel
     * reads as a dead stream rather than as a filter set days ago.
     */
    val hasTransientFilter: Boolean
        get() = query.isNotBlank() ||
            unreadOnly ||
            window != EventsTimeWindow.All ||
            markFloorMillis != null

    fun withMuteToggled(name: String): EventsFilterState =
        copy(mutedNames = if (name in mutedNames) mutedNames - name else mutedNames + name)

    /**
     * "Show only [name]" expressed as an exclusion: every other name in [knownNames] gets muted.
     *
     * Pure, and here rather than in the view model, so the one-in-many-out inversion is testable
     * without a repository. Names already muted stay muted — [knownNames] is what the store holds, so a
     * name that has aged out of the window keeps its mute rather than silently coming back.
     */
    fun withSolo(name: String, knownNames: Collection<String>): EventsFilterState =
        copy(mutedNames = (mutedNames + knownNames) - name)

    fun withMutesCleared(): EventsFilterState = copy(mutedNames = emptySet())

    /**
     * Drops everything the user set this session and keeps the mutes.
     *
     * One transformation rather than four `copy` calls: through `combine` each separate write would
     * refilter the whole list and briefly paint a half-cleared state.
     */
    fun withTransientCleared(): EventsFilterState = EventsFilterState(mutedNames = mutedNames)
}

/** An event paired with the lowercased text a query is matched against. See `EventSearchIndex`. */
data class IndexedEvent(val event: Event, val haystack: String)

/**
 * One filter pass over the indexed events.
 *
 * A class rather than a free function taking the flags because `query.trim().lowercase()` inside the
 * predicate would allocate twice for every one of up to [EventStore.MAX_ENTRIES] events on every
 * recompute, and a recompute happens on each keystroke.
 */
class EventsPredicate(
    private val filters: EventsFilterState,
    /** The stricter of the rolling floor and the Mark floor. Null admits everything. */
    private val floorMillis: Long?,
) {
    private val needle = filters.query.trim().lowercase()

    /**
     * Ordered cheapest and most selective first — a set lookup and two comparisons before the
     * substring scan, so a muted or out-of-window event never touches its haystack.
     */
    fun matches(indexed: IndexedEvent): Boolean {
        val event = indexed.event
        if (event.name in filters.mutedNames) return false
        if (filters.unreadOnly && event.isViewed) return false
        if (floorMillis != null && event.time < floorMillis) return false
        return needle.isEmpty() || indexed.haystack.contains(needle)
    }
}

data class EventsUiState(
    val events: List<Event> = emptyList(),
    /** Everything the store holds, so the subtitle can say how much the filters removed. */
    val totalCount: Int = 0,
    /**
     * Occurrences per event name across everything held, **before** the mute set is applied.
     *
     * Pre-filter because the number that decides whether muting a name helps is how much of the
     * stream that name is, not how much of it survived the other filters — and a post-filter count
     * would just restate [shownCount] and churn on every keystroke.
     */
    val nameCounts: Map<String, Int> = emptyMap(),
    /** [nameCounts] keys ordered by volume, which is the order someone hunting a mute target wants. */
    val names: List<String> = emptyList(),
    val atStoreCap: Boolean = false,
    val filters: EventsFilterState = EventsFilterState(),
) {
    val shownCount: Int get() = events.size
}

/**
 * Clamps a payload to [max] lines, reporting what it hid.
 *
 * Truncation has to announce itself: a silently cut payload reads as an event that genuinely carried
 * only six keys. See `EventItem` for why the row clamps rather than scrolls.
 */
fun String.clampLines(max: Int): String {
    val lines = lines()
    if (lines.size <= max) return this
    return lines.take(max).joinToString("\n") + "\n… ${lines.size - max} more lines"
}

/**
 * The panel subtitle, e.g. "412 events · 38 shown · oldest dropped at 2000".
 *
 * A pure function so the string is testable. The cap clause matters because [EventStore] drops its
 * tail without a signal, and a total that stalls at exactly the cap otherwise looks like a stalled
 * stream — the same reason `TracesViewModel` exposes a span count.
 */
fun eventsSubtitle(state: EventsUiState): String = buildString {
    append("${state.totalCount} events")
    if (state.shownCount != state.totalCount) append(" · ${state.shownCount} shown")
    if (state.atStoreCap) append(" · oldest dropped at ${EventStore.MAX_ENTRIES}")
}
