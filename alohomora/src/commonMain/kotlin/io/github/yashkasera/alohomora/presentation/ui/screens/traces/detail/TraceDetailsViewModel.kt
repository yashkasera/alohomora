package io.github.yashkasera.alohomora.presentation.ui.screens.traces.detail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.yashkasera.alohomora.common.Span
import io.github.yashkasera.alohomora.common.trace.TraceRow
import io.github.yashkasera.alohomora.common.trace.TraceSummary
import io.github.yashkasera.alohomora.common.trace.TraceWindow
import io.github.yashkasera.alohomora.common.trace.summarize
import io.github.yashkasera.alohomora.common.trace.toTraceRows
import io.github.yashkasera.alohomora.common.trace.traceWindow
import io.github.yashkasera.alohomora.domain.repository.SpanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
internal data class TraceDetailsState(
    val summary: TraceSummary? = null,
    val rows: List<TraceRow> = emptyList(),
    val window: TraceWindow = TraceWindow(0L, 1L),
    val spans: List<Span> = emptyList(),
    val selectedSpanId: String? = null,
    val showWaterfall: Boolean = false,
    val zoom: Float = 1f,
) {
    val selectedSpan: Span? get() = spans.firstOrNull { it.spanId == selectedSpanId }

    val selectedSpanChildren: List<Span>
        get() = selectedSpanId?.let { id -> spans.filter { it.parentSpanId == id } } ?: emptyList()
}

internal class TraceDetailsViewModel(
    private val traceId: String,
    private val spanRepository: SpanRepository,
) : ViewModel() {

    private val selectedSpanId = MutableStateFlow<String?>(null)
    private val collapsed = MutableStateFlow<Set<String>>(emptySet())
    private val showWaterfall = MutableStateFlow(false)
    private val zoom = MutableStateFlow(1f)

    val state: StateFlow<TraceDetailsState> = combine(
        spanRepository.observeTrace(traceId),
        selectedSpanId,
        collapsed,
        showWaterfall,
        zoom,
    ) { spans, spanId, collapsed, showWaterfall, zoom ->
        TraceDetailsState(
            summary = spans.summarize(),
            rows = spans.toTraceRows(collapsed),
            window = traceWindow(spans),
            spans = spans,
            // Drop a selection whose span is gone, so the detail sheet cannot describe a span the list
            // has stopped showing.
            selectedSpanId = spanId?.takeIf { id -> spans.any { it.spanId == id } },
            showWaterfall = showWaterfall,
            zoom = zoom,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_GRACE_MILLIS),
        initialValue = TraceDetailsState(),
    )

    init {
        // Marks the whole trace, not one span: viewing is a trace-level act, matching the desktop.
        viewModelScope.launch { spanRepository.markTraceAsViewed(traceId) }
    }

    fun selectSpan(spanId: String?) {
        selectedSpanId.value = spanId
    }

    fun toggleCollapse(spanId: String) {
        collapsed.value = collapsed.value.let { if (spanId in it) it - spanId else it + spanId }
    }

    fun toggleWaterfall() {
        showWaterfall.value = !showWaterfall.value
    }

    fun setZoom(value: Float) {
        zoom.value = value
    }

    private companion object {
        const val SUBSCRIPTION_GRACE_MILLIS = 5_000L
    }
}
