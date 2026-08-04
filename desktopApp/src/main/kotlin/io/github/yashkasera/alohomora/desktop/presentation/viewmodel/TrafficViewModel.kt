package io.github.yashkasera.alohomora.desktop.presentation.viewmodel

import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.desktop.domain.repository.DevToolsRepository
import io.github.yashkasera.alohomora.desktop.presentation.model.IndexedTraffic
import io.github.yashkasera.alohomora.desktop.presentation.model.TrafficFilterState
import io.github.yashkasera.alohomora.desktop.presentation.model.TrafficUiState
import io.github.yashkasera.alohomora.desktop.presentation.model.filterTraffic
import io.github.yashkasera.alohomora.desktop.presentation.model.isError
import io.github.yashkasera.alohomora.desktop.presentation.model.methodLabel
import io.github.yashkasera.alohomora.desktop.presentation.model.searchHaystack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Owns the Traffic panel's filter state and derived list.
 *
 * Separate from [DevToolsViewModel] for the reason [TracesViewModel] documents: that one is handed to every
 * panel, so indexing and filtering the whole traffic store there would stay warm whether or not anyone is
 * looking at Traffic.
 */
class TrafficViewModel(
    private val repository: DevToolsRepository,
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _filters = MutableStateFlow(TrafficFilterState())

    /**
     * Exposed on its own rather than read out of [uiState].
     *
     * The search field renders from this, and it must update in the same frame as the keystroke. Reading it
     * back through the `combine` below would add a dispatcher hop, which is what made the caret jump left —
     * see the comment in `AlohomoraTextField`. That component now survives the lag either way, but there is
     * no reason to reintroduce it.
     */
    val query: StateFlow<String> = _filters.map { it.query }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, "")

    /**
     * Every held entry paired with its search text, rebuilt only when the store changes.
     *
     * Not memoised per entry the way `EventSearchIndex` is: the haystack here is method, URL and status —
     * short, already-materialised strings — where an event's is a serialised JSON payload. Rebuilding these
     * on a streamed request is cheap; serialising 2000 payloads was not.
     */
    private val indexed: StateFlow<List<IndexedTraffic>> = repository.traffic
        .map { entries -> entries.map { IndexedTraffic(it, it.searchHaystack()) } }
        .stateIn(scope, SharingStarted.WhileSubscribed(SUBSCRIPTION_GRACE_MILLIS), emptyList())

    val uiState: StateFlow<TrafficUiState> =
        combine(indexed, _filters) { entries, filters ->
            // Counted before the filters narrow anything: the number that tells you whether filtering to a
            // method is worth it is how much of the stream that method is.
            val counts = entries.groupingBy { it.entry.methodLabel() }.eachCount()
            TrafficUiState(
                entries = entries.filterTraffic(filters),
                totalCount = entries.size,
                errorCount = entries.count { it.entry.isError() },
                methodCounts = counts,
                methods = counts.entries
                    .sortedWith(
                        compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key },
                    )
                    .map { it.key },
                filters = filters,
            )
        }.stateIn(scope, SharingStarted.WhileSubscribed(SUBSCRIPTION_GRACE_MILLIS), TrafficUiState())

    fun onQueryChange(query: String) = _filters.update { it.copy(query = query) }

    fun onMethodToggle(method: String) = _filters.update { it.withMethodToggled(method) }

    fun onErrorsOnlyChange(errorsOnly: Boolean) = _filters.update { it.copy(errorsOnly = errorsOnly) }

    fun clearFilters() = _filters.update { it.cleared() }

    /** Dims the row as soon as it is opened; `TrafficItem` already styles on `isViewed`. */
    fun markViewed(entry: TrafficEntry) = repository.markTrafficViewed(entry.id)

    fun clearTraffic() = repository.clearCaptured(traces = true)

    fun close() {
        scope.cancel()
    }

    private companion object {
        /** Keeps the derivation warm across a quick panel switch; see [TracesViewModel]. */
        const val SUBSCRIPTION_GRACE_MILLIS = 5_000L
    }
}
