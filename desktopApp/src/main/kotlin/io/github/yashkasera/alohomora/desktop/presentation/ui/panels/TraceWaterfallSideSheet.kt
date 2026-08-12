package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import io.github.yashkasera.alohomora.common.DateUtils
import io.github.yashkasera.alohomora.common.Span
import io.github.yashkasera.alohomora.common.durationNanos
import io.github.yashkasera.alohomora.common.startEpochMillis
import io.github.yashkasera.alohomora.common.trace.formatDuration
import io.github.yashkasera.alohomora.common.trace.selfTimeNanos
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.AlohomoraSideSheet
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.KeyValueRow
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.SectionLabel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.TraceDetailState
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.TracesViewModel
import io.github.yashkasera.alohomora.ui.components.AlohomoraChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraPrimaryTabRow
import io.github.yashkasera.alohomora.ui.components.AlohomoraTab
import io.github.yashkasera.alohomora.ui.components.jsonviewer.JsonTreeView
import io.github.yashkasera.alohomora.ui.components.waterfall.TraceWaterfall
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Layers
import io.github.yashkasera.alohomora.ui.icons.X
import io.github.yashkasera.alohomora.ui.theme.dimens
import kotlinx.coroutines.launch

/**
 * Wider than the Traffic sheet, and a fraction rather than a fixed width: in a waterfall, horizontal
 * space *is* time resolution, so a larger monitor should buy a more readable axis.
 */
private const val TRACE_SHEET_WIDTH_FRACTION = 0.7f

/** Share of the sheet given to the selected span's detail, when there is one. */
private const val DETAIL_HEIGHT_FRACTION = 0.38f

/**
 * The trace waterfall, in a right-hand sheet.
 *
 * Layout is header / axis+rows / detail, stacked vertically. The selected span's detail goes at the
 * bottom rather than in a third column (which would leave the waterfall too narrow to be a waterfall)
 * and rather than inline in the row (which pushes rows apart and destroys the vertical alignment that
 * makes a waterfall legible in the first place).
 */
@Composable
fun TraceWaterfallSideSheet(
    tracesViewModel: TracesViewModel,
    onDismiss: () -> Unit,
) {
    val detail by tracesViewModel.traceDetail.collectAsState()
    val nameFraction by tracesViewModel.nameFraction.collectAsState()

    AlohomoraSideSheet(
        visible = detail != null,
        onDismiss = onDismiss,
        widthFraction = TRACE_SHEET_WIDTH_FRACTION,
        header = { detail?.let { TraceSheetHeader(it, tracesViewModel::collapseAll, onDismiss) } },
    ) {
        detail?.let { state ->
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    TraceWaterfall(
                        rows = state.rows,
                        window = state.window,
                        selectedSpanId = state.selectedSpanId,
                        nameFraction = nameFraction,
                        onNameFractionChange = tracesViewModel::onNameFractionChange,
                        onToggleCollapse = tracesViewModel::toggleCollapse,
                        onSelectSpan = tracesViewModel::selectSpan,
                    )
                }
                state.selectedSpan?.let { span ->
                    AlohomoraHorizontalDivider()
                    SpanDetailSection(
                        span = span,
                        children = state.selectedSpanChildren,
                        traceStartNanos = state.window.startNanos,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(DETAIL_HEIGHT_FRACTION),
                    )
                }
            }
        }
    }
}

@Composable
private fun TraceSheetHeader(
    state: TraceDetailState,
    onCollapseAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    val summary = state.summary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaterialTheme.dimens.margin.xl,
                vertical = MaterialTheme.dimens.margin.md,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
            ) {
                Text(
                    text = summary?.rootSpanName ?: "(root pending)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (summary?.hasError == true) {
                    AlohomoraChip(
                        label = "error",
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
                if (summary?.isComplete == false) {
                    AlohomoraChip(
                        label = "partial",
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                // The absolute wall-clock start appears exactly once, here. Axis labels are offsets
                // from it, because at tick density absolute timestamps are unreadable.
                text = buildString {
                    append(formatDuration(summary?.durationNanos ?: 0L))
                    append(" · ")
                    append(summary?.spanCount ?: 0)
                    append(" spans · ")
                    append(
                        DateUtils.format(
                            summary?.startMillis ?: 0L,
                            DateUtils.Format.HH_MM_SS_3MS,
                        ),
                    )
                    append(" · ")
                    append(state.traceId)
                },
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        AlohomoraIconButton(onClick = onCollapseAll) {
            Icon(imageVector = Icons.Layers, contentDescription = "Collapse or expand all spans")
        }
        AlohomoraIconButton(onClick = onDismiss) {
            Icon(imageVector = Icons.X, contentDescription = "Close")
        }
    }
}

/**
 * Detail for the selected span: attributes, events, then everything else.
 *
 * Reuses the tab pattern from the Traffic sheet rather than inventing a second one.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SpanDetailSection(
    span: Span,
    children: List<Span>,
    traceStartNanos: Long,
    modifier: Modifier = Modifier,
) {
    val tabs = listOf("Attributes", "Events", "Overview")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    Column(modifier = modifier) {
        AlohomoraPrimaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            modifier = Modifier.fillMaxWidth(),
        ) {
            tabs.forEachIndexed { index, tab ->
                AlohomoraTab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = tab,
                    uppercase = false,
                )
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
        ) { page ->
            when (page) {
                0 -> SpanAttributesTab(span)
                1 -> SpanEventsTab(span, traceStartNanos)
                else -> SpanOverviewTab(span, children, traceStartNanos)
            }
        }
    }
}

@Composable
private fun SpanAttributesTab(span: Span) {
    val attributes = span.attributes
    if (attributes == null) {
        DetailEmptyState("This span carries no attributes.")
        return
    }
    // JsonTreeView keys its parse on the json string, so switching spans re-parses correctly. That was
    // a real bug — see JsonTreeViewKeyingTest.
    JsonTreeView(json = attributes.toString())
}

@Composable
private fun SpanEventsTab(span: Span, traceStartNanos: Long) {
    if (span.events.isEmpty()) {
        // Expected rather than exceptional: several tracers have no per-span event concept at all —
        // Sentry among them — so this is the normal state for a lot of apps.
        DetailEmptyState("This span has no events. Not every tracer records them.")
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(MaterialTheme.dimens.margin.lg),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
    ) {
        span.events.forEach { event ->
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xs)) {
                KeyValueRow(
                    label = event.name,
                    value = "+${formatDuration(event.epochNanos - traceStartNanos)}",
                    monospaceValue = true,
                )
                event.attributes?.forEach { (key, value) ->
                    Text(
                        text = "$key = $value",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }
}

@Composable
private fun SpanOverviewTab(span: Span, children: List<Span>, traceStartNanos: Long) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(MaterialTheme.dimens.margin.lg),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
    ) {
        SectionLabel(span.name)
        KeyValueRow("Span id", span.spanId, monospaceValue = true)
        KeyValueRow("Parent span id", span.parentSpanId ?: "— (root)", monospaceValue = true)
        KeyValueRow("Kind", span.kind)
        KeyValueRow("Status", span.statusCode + (span.statusDescription?.let { " · $it" } ?: ""))
        KeyValueRow("Scope", span.scopeName ?: "-")
        KeyValueRow("Start", "+${formatDuration(span.startEpochNanos - traceStartNanos)}", monospaceValue = true)
        KeyValueRow(
            "Wall clock",
            DateUtils.format(span.startEpochMillis(), DateUtils.Format.HH_MM_SS_3MS),
            monospaceValue = true,
        )
        KeyValueRow("Duration", formatDuration(span.durationNanos()), monospaceValue = true)
        // Self time is the most actionable derived number here and nothing else in the console shows
        // it: a 400ms span whose children account for 390ms is a slow dependency, whereas one with
        // 10ms of children is a slow parent. The bars alone cannot separate those.
        KeyValueRow(
            "Self time",
            formatDuration(selfTimeNanos(span, children)),
            monospaceValue = true,
        )
        if (span.endEpochNanos < span.startEpochNanos) {
            KeyValueRow("Clock skew", "reported end precedes start")
        }
    }
}

@Composable
private fun DetailEmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(MaterialTheme.dimens.margin.lg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
