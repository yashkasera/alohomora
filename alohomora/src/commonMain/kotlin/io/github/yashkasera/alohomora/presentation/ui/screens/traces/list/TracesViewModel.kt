package io.github.yashkasera.alohomora.presentation.ui.screens.traces.list

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.yashkasera.alohomora.common.trace.TraceSummary
import io.github.yashkasera.alohomora.common.trace.toTraceSummaries
import io.github.yashkasera.alohomora.devtools.DevToolsDefaults
import io.github.yashkasera.alohomora.domain.repository.SpanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
internal data class TracesState(
    val traces: List<TraceSummary> = emptyList(),
    val searchQuery: String = "",
    val errorsOnly: Boolean = false,
)

/**
 * Traces for the in-app console list.
 *
 * Unlike [ErrorViewModel] and the other list view models, this does **not** page. A trace straddles
 * many rows, so paging by span would slice one across a page boundary and render half a waterfall.
 * Instead it observes a bounded window of recent spans and groups them with the same
 * `toTraceSummaries()` the desktop uses — one definition of "what a trace is", rather than a SQL
 * aggregate here and Kotlin grouping there. Two implementations of that definition is precisely how the
 * two consoles came to disagree on an error row's title before `exceptionTypeName()` was shared.
 *
 * The cost is that this shows traces from the most recent [DevToolsDefaults.SPAN_SNAPSHOT_LIMIT] spans
 * rather than all history — acceptable for a session-scoped debug database that is wiped on schema
 * change anyway, and exactly what the desktop already shows.
 */
internal class TracesViewModel(
    private val spanRepository: SpanRepository,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val errorsOnly = MutableStateFlow(false)

    val state: StateFlow<TracesState> = combine(
        spanRepository.observeLatestSpans(DevToolsDefaults.SPAN_SNAPSHOT_LIMIT),
        searchQuery,
        errorsOnly,
    ) { spans, query, errorsOnly ->
        TracesState(
            traces = spans.toTraceSummaries()
                .filter { it.matches(query) && (!errorsOnly || it.hasError) },
            searchQuery = query,
            errorsOnly = errorsOnly,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_GRACE_MILLIS),
        initialValue = TracesState(),
    )

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onErrorsOnlyChange(value: Boolean) {
        errorsOnly.value = value
    }

    fun clearAllTraces() {
        viewModelScope.launch {
            spanRepository.clearAll()
        }
    }

    private companion object {
        const val SUBSCRIPTION_GRACE_MILLIS = 5_000L
    }
}
