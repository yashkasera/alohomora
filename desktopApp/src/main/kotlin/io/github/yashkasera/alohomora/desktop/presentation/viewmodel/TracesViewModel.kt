package io.github.yashkasera.alohomora.desktop.presentation.viewmodel

import io.github.yashkasera.alohomora.common.Span
import io.github.yashkasera.alohomora.common.trace.TraceRow
import io.github.yashkasera.alohomora.common.trace.TraceSummary
import io.github.yashkasera.alohomora.common.trace.TraceWindow
import io.github.yashkasera.alohomora.common.trace.summarize
import io.github.yashkasera.alohomora.common.trace.toTraceRows
import io.github.yashkasera.alohomora.common.trace.toTraceSummaries
import io.github.yashkasera.alohomora.common.trace.traceWindow
import io.github.yashkasera.alohomora.desktop.domain.repository.DevToolsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** What the waterfall sheet needs for one trace, assembled in one place. */
data class TraceDetailState(
    val traceId: String,
    val summary: TraceSummary?,
    val rows: List<TraceRow>,
    val window: TraceWindow,
    val spans: List<Span>,
    val selectedSpanId: String?,
) {
    val selectedSpan: Span? get() = spans.firstOrNull { it.spanId == selectedSpanId }

    /** Direct children of the selected span, for its self-time figure. */
    val selectedSpanChildren: List<Span>
        get() = selectedSpanId?.let { id -> spans.filter { it.parentSpanId == id } } ?: emptyList()
}

/**
 * Owns the Traces panel's derived state.
 *
 * Separate from [DevToolsViewModel] deliberately. Grouping a flat span list into traces is the one
 * derivation here with real cost, and `DevToolsViewModel` is handed to every panel in the window — so
 * putting it there would keep the grouping warm whether or not anyone is looking at Traces.
 * [DatabaseViewModel] and [CacheViewModel] set the precedent for a per-domain view model over one store.
 *
 * The grouping also does not belong in `SpanStore` (which would re-group on every streamed span, O(n²)
 * across a session) nor in a composable (`remember` blocks a frame, and the search field would re-group
 * inside composition on every keystroke).
 */
class TracesViewModel(
    private val repository: DevToolsRepository,
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _errorsOnly = MutableStateFlow(false)
    val errorsOnly: StateFlow<Boolean> = _errorsOnly.asStateFlow()

    private val _selectedTraceId = MutableStateFlow<String?>(null)
    val selectedTraceId: StateFlow<String?> = _selectedTraceId.asStateFlow()

    private val _selectedSpanId = MutableStateFlow<String?>(null)

    /** Collapsed span ids per trace, so reopening a trace keeps the shape the user left it in. */
    private val _collapsed = MutableStateFlow<Map<String, Set<String>>>(emptyMap())

    private val _nameFraction = MutableStateFlow(DEFAULT_NAME_FRACTION)
    val nameFraction: StateFlow<Float> = _nameFraction.asStateFlow()

    val captureSupported: StateFlow<Boolean> = repository.spanCaptureSupported

    /** Total spans held, so the panel can say why a trace might be partial. */
    val spanCount: StateFlow<Int> = repository.spans
        .map { it.size }
        .stateIn(scope, SharingStarted.WhileSubscribed(SUBSCRIPTION_GRACE_MILLIS), 0)

    /**
     * Trace rows, newest first.
     *
     * `WhileSubscribed` rather than `Eagerly`: this is the expensive flow in the window, and it should
     * only run while the Traces panel is actually on screen. The grace period keeps it warm across a
     * quick tab switch instead of re-grouping from scratch.
     *
     * No debounce on [query]. `StateFlow` already conflates bursts, and a debounce would delay first
     * paint on a fresh snapshot — precisely when the user is watching.
     */
    val traces: StateFlow<List<TraceSummary>> =
        combine(repository.spans, _query, _errorsOnly) { spans, query, errorsOnly ->
            spans.toTraceSummaries()
                .filter { it.matches(query) && (!errorsOnly || it.hasError) }
        }.stateIn(scope, SharingStarted.WhileSubscribed(SUBSCRIPTION_GRACE_MILLIS), emptyList())

    /** State for the open trace, or null when the sheet is closed. */
    val traceDetail: StateFlow<TraceDetailState?> =
        combine(
            repository.spans,
            _selectedTraceId,
            _selectedSpanId,
            _collapsed,
        ) { spans, traceId, spanId, collapsed ->
            if (traceId == null) return@combine null
            val traceSpans = spans.filter { it.traceId == traceId }
            TraceDetailState(
                traceId = traceId,
                summary = traceSpans.summarize(),
                rows = traceSpans.toTraceRows(collapsed[traceId].orEmpty()),
                window = traceWindow(traceSpans),
                spans = traceSpans,
                // Drop a selection whose span is no longer present, so the detail pane cannot show a
                // span the waterfall has stopped rendering.
                selectedSpanId = spanId?.takeIf { id -> traceSpans.any { it.spanId == id } },
            )
        }.stateIn(scope, SharingStarted.WhileSubscribed(SUBSCRIPTION_GRACE_MILLIS), null)

    fun onQueryChange(query: String) {
        _query.value = query
    }

    fun onErrorsOnlyChange(errorsOnly: Boolean) {
        _errorsOnly.value = errorsOnly
    }

    fun openTrace(traceId: String) {
        _selectedTraceId.value = traceId
        _selectedSpanId.value = null
        repository.markTraceViewed(traceId)
        // Backfills spans the snapshot truncated. A no-op against a device that cannot answer.
        repository.requestTraceSpans(traceId)
    }

    fun closeTrace() {
        _selectedTraceId.value = null
        _selectedSpanId.value = null
    }

    fun selectSpan(spanId: String) {
        // Re-tapping the selected span clears it, which is the only way to collapse the detail pane
        // without closing the whole sheet.
        _selectedSpanId.value = if (_selectedSpanId.value == spanId) null else spanId
    }

    fun toggleCollapse(spanId: String) {
        val traceId = _selectedTraceId.value ?: return
        _collapsed.value = _collapsed.value.toMutableMap().apply {
            val current = this[traceId].orEmpty()
            this[traceId] = if (spanId in current) current - spanId else current + spanId
        }
    }

    fun collapseAll() {
        val detail = traceDetail.value ?: return
        val traceId = detail.traceId
        val parents = detail.rows.filter { it.hasChildren }.map { it.span.spanId }.toSet()
        _collapsed.value = _collapsed.value.toMutableMap().apply {
            // Collapse everything, or expand everything if it is already fully collapsed — one button
            // that always does the thing the current state makes useful.
            this[traceId] =
                if (this[traceId].orEmpty().containsAll(parents)) emptySet() else parents
        }
    }

    fun onNameFractionChange(fraction: Float) {
        _nameFraction.value = fraction
    }

    fun clearTraces() {
        closeTrace()
        repository.clearCaptured(spans = true)
    }

    fun close() {
        scope.cancel()
    }

    private companion object {
        const val DEFAULT_NAME_FRACTION = 0.4f

        /** Keeps the grouping warm across a quick panel switch without holding it for a closed window. */
        const val SUBSCRIPTION_GRACE_MILLIS = 5_000L
    }
}
