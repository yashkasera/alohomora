package io.github.yashkasera.alohomora.ui.components.waterfall

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.common.trace.TimeUnitScale
import io.github.yashkasera.alohomora.common.trace.TraceRow
import io.github.yashkasera.alohomora.common.trace.TraceWindow
import io.github.yashkasera.alohomora.common.trace.niceTickStep
import io.github.yashkasera.alohomora.common.trace.pickUnit
import io.github.yashkasera.alohomora.common.trace.ticks
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.theme.dimens
import io.github.yashkasera.alohomora.ui.theme.muted
import io.github.yashkasera.alohomora.ui.theme.panelBorder

/**
 * Most spans rendered for one trace.
 *
 * A cap rather than a guarantee, and it is announced in the UI rather than applied silently: a
 * debugging tool that quietly drops half a trace is worse than one that says it did. Set well above
 * any hand-instrumented trace; only pathological auto-instrumentation reaches it.
 */
const val SPAN_RENDER_LIMIT: Int = 2000

/** Bounds on the name/bar split, so neither column can be dragged out of existence. */
private const val MIN_NAME_FRACTION = 0.2f
private const val MAX_NAME_FRACTION = 0.6f

private val DividerHitWidth = 8.dp

/**
 * A time-sliced waterfall of one trace's spans.
 *
 * Rendered as a [LazyColumn] of ordinary composables, with each bar drawn in `drawBehind`, rather than
 * as one big `Canvas`. A single canvas would have to reimplement scrolling, clipping, hit-testing and
 * text layout by hand; virtualised rows give all four for free and keep the live composable count
 * bounded by the viewport, so 20 spans and 1000 spans cost the same per frame. What grows with span
 * count is the tree flatten, and that happens once per data change in a ViewModel, not per frame.
 *
 * [rows] must already be flattened and collapse-filtered — see `List<Span>.toTraceRows()` in
 * `alohomora-common`, which also handles orphan promotion and cycle-breaking. That logic is shared
 * with the mobile console rather than reimplemented here.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TraceWaterfall(
    rows: List<TraceRow>,
    window: TraceWindow,
    selectedSpanId: String?,
    nameFraction: Float,
    onNameFractionChange: (Float) -> Unit,
    onToggleCollapse: (String) -> Unit,
    onSelectSpan: (String) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    val visibleRows = remember(rows) { rows.take(SPAN_RENDER_LIMIT) }
    val unit = remember(window) { pickUnit(window.rangeNanos) }
    val ticks = remember(window) { window.ticks(niceTickStep(window.rangeNanos)) }

    LazyColumn(state = listState, modifier = modifier.fillMaxSize()) {
        stickyHeader {
            Column(Modifier.background(MaterialTheme.colorScheme.surface)) {
                // The drag handle lives here, in the header, and nowhere else. It started life as an
                // overlay spanning the whole list, which silently ate every click that landed on the
                // split — an 8dp vertical strip in which no span could be selected. A column separator
                // is dragged by its header anyway; the rows only draw an inert guide line.
                WaterfallHeaderRow(
                    window = window,
                    unit = unit,
                    ticks = ticks,
                    nameFraction = nameFraction,
                    onNameFractionChange = onNameFractionChange,
                )
                AlohomoraHorizontalDivider()
            }
        }

        if (rows.size > SPAN_RENDER_LIMIT) {
            item {
                TruncationBanner(shown = SPAN_RENDER_LIMIT, total = rows.size)
            }
        }

        items(visibleRows, key = { it.span.spanId }) { row ->
            WaterfallRowItem(
                row = row,
                window = window,
                ticks = ticks,
                isSelected = row.span.spanId == selectedSpanId,
                nameFraction = nameFraction,
                onToggleCollapse = { onToggleCollapse(row.span.spanId) },
                onSelect = { onSelectSpan(row.span.spanId) },
            )
        }
    }
}

/**
 * The axis, plus the draggable handle for the name/bar split.
 *
 * The split is a fraction rather than a `Dp` so it survives a window resize and so a wider window buys
 * time resolution instead of empty margin. Resizable at all because span names range from `GET` to
 * fully-qualified handler names, and no fixed split serves both.
 */
@Composable
private fun WaterfallHeaderRow(
    window: TraceWindow,
    unit: TimeUnitScale,
    ticks: List<Long>,
    nameFraction: Float,
    onNameFractionChange: (Float) -> Unit,
) {
    var totalWidth by remember { mutableIntStateOf(0) }
    Box(Modifier.fillMaxWidth().onSizeChanged { totalWidth = it.width }) {
        WaterfallAxis(
            window = window,
            unit = unit,
            ticks = ticks,
            nameFraction = nameFraction,
        )
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth(nameFraction))
            Box(
                Modifier
                    .width(DividerHitWidth)
                    .fillMaxHeight()
                    .pointerInput(totalWidth) {
                        if (totalWidth == 0) return@pointerInput
                        detectHorizontalDragGestures { _, dragAmount ->
                            onNameFractionChange(
                                (nameFraction + dragAmount / totalWidth)
                                    .coerceIn(MIN_NAME_FRACTION, MAX_NAME_FRACTION),
                            )
                        }
                    }
                    .background(MaterialTheme.colorScheme.panelBorder),
            )
        }
    }
}

@Composable
private fun TruncationBanner(shown: Int, total: Int) {
    Text(
        text = "Showing the first $shown of $total spans in this trace.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.muted,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(MaterialTheme.dimens.margin.sm),
    )
}
