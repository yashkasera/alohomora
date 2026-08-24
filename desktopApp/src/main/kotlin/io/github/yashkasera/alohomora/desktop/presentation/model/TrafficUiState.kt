package io.github.yashkasera.alohomora.desktop.presentation.model

import io.github.yashkasera.alohomora.common.TrafficEntry

/**
 * The Traffic panel's filters.
 *
 * Deliberately no mute set, unlike Events. A noisy *event name* repeats verbatim and is worth silencing
 * for good, whereas a request is identified by a URL that varies per call — so the useful axes here are a
 * free-text query and the method, and neither is worth persisting.
 */
data class TrafficFilterState(
    val query: String = "",
    /**
     * Methods to show. **Empty means no filter**, not "hide everything".
     *
     * An include set rather than the exclusion set Events uses: with a handful of verbs the natural act is
     * "show me the POSTs", not "hide the GETs". The empty default then needs no knowledge of which methods
     * exist before the first request arrives.
     */
    val methods: Set<String> = emptySet(),
    /** Narrows to failed responses. Mirrors the Traces panel's Errors chip. */
    val errorsOnly: Boolean = false,
) {
    val hasFilter: Boolean get() = query.isNotBlank() || methods.isNotEmpty() || errorsOnly

    fun withMethodToggled(method: String): TrafficFilterState =
        copy(methods = if (method in methods) methods - method else methods + method)

    fun cleared(): TrafficFilterState = TrafficFilterState()
}

/**
 * Whether the response failed.
 *
 * A null status is deliberately **not** an error. It means the exchange produced no response code — still in
 * flight, or the capture never saw one — and calling that a failure would put requests in the Errors view that
 * may have succeeded. Same rule as [TrafficEntry.isSuccessful], read from the other end.
 */
fun TrafficEntry.isError(): Boolean = (status ?: 0) >= HTTP_ERROR_FLOOR

private const val HTTP_ERROR_FLOOR = 400

/** A traffic entry paired with the lowercased text a query is matched against. */
data class IndexedTraffic(val entry: TrafficEntry, val haystack: String)

/**
 * Method as displayed and filtered on: upper-cased, with a missing one given a visible name.
 *
 * One function so the chip label, the filter set and the search text cannot disagree — a `null` method
 * silently matching nothing would look like a dropped request.
 */
fun TrafficEntry.methodLabel(): String =
    method?.takeIf { it.isNotBlank() }?.uppercase() ?: "UNKNOWN"

/**
 * The text a query runs against: method, URL and status.
 *
 * Bodies and headers are excluded on purpose. They are the largest fields captured and would make every
 * keystroke scan megabytes, while the thing being hunted in a traffic list is almost always an endpoint or
 * a status. The detail sheet is where a body is read.
 */
fun TrafficEntry.searchHaystack(): String = buildString {
    append(methodLabel())
    append(' ')
    // isNotBlank rather than a null check: a capture can carry an empty url, and `?:` would then index an
    // empty string and make the row unsearchable. Same reason methodLabel guards a blank method.
    append(url?.takeIf { it.isNotBlank() } ?: pathWithQuery())
    status?.let {
        append(' ')
        append(it)
    }
}.lowercase()

class TrafficPredicate(
    private val filters: TrafficFilterState,
) {
    private val needle = filters.query.trim().lowercase()

    /** Cheapest and most selective first, so a filtered-out entry never touches its haystack. */
    fun matches(indexed: IndexedTraffic): Boolean {
        if (filters.errorsOnly && !indexed.entry.isError()) return false
        if (filters.methods.isNotEmpty() && indexed.entry.methodLabel() !in filters.methods) return false
        return needle.isEmpty() || indexed.haystack.contains(needle)
    }
}

data class TrafficUiState(
    val entries: List<TrafficEntry> = emptyList(),
    val totalCount: Int = 0,
    /** Occurrences per method across everything held, before the filters narrow it. */
    val methodCounts: Map<String, Int> = emptyMap(),
    /** [methodCounts] keys by volume, so the verb you actually use most is nearest the search box. */
    val methods: List<String> = emptyList(),
    /** Failed responses across everything held, so the chip can say whether it is worth pressing. */
    val errorCount: Int = 0,
    val filters: TrafficFilterState = TrafficFilterState(),
    val selectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
) {
    val shownCount: Int get() = entries.size

    private val visibleSelectedIds: Set<String> by lazy {
        if (selectedIds.isEmpty()) emptySet()
        else {
            val visibleIds = entries.mapTo(HashSet(entries.size)) { it.id }
            selectedIds.intersect(visibleIds)
        }
    }

    val selectedCount: Int get() = visibleSelectedIds.size

    val allFilteredSelected: Boolean
        get() = entries.isNotEmpty() && visibleSelectedIds.size == entries.size

    val exportableCount: Int
        get() = if (selectionMode && visibleSelectedIds.isNotEmpty()) visibleSelectedIds.size else entries.size
}

/** Builds the rows for [filters], newest first as the store hands them over. */
fun List<IndexedTraffic>.filterTraffic(filters: TrafficFilterState): List<TrafficEntry> {
    val predicate = TrafficPredicate(filters)
    return filter(predicate::matches).map(IndexedTraffic::entry)
}

/** "412 requests · 38 shown". Pure so the string is testable. */
fun trafficSubtitle(state: TrafficUiState): String = buildString {
    append("${state.totalCount} ${if (state.totalCount == 1) "request" else "requests"}")
    if (state.shownCount != state.totalCount) append(" · ${state.shownCount} shown")
}
