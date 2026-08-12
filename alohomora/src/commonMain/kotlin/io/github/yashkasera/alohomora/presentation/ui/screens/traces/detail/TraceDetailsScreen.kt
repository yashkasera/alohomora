package io.github.yashkasera.alohomora.presentation.ui.screens.traces.detail

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.yashkasera.alohomora.common.DateUtils
import io.github.yashkasera.alohomora.common.Span
import io.github.yashkasera.alohomora.common.durationNanos
import io.github.yashkasera.alohomora.common.startEpochMillis
import io.github.yashkasera.alohomora.common.trace.formatDuration
import io.github.yashkasera.alohomora.common.trace.selfTimeNanos
import io.github.yashkasera.alohomora.ui.components.AlohomoraBottomSheetModal
import io.github.yashkasera.alohomora.ui.components.AlohomoraChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilterChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.ui.components.waterfall.TraceWaterfall
import io.github.yashkasera.alohomora.ui.icons.ArrowLeft
import io.github.yashkasera.alohomora.ui.icons.ChartLine
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.theme.dimens
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Discrete zoom steps for the waterfall mode. */
private val ZoomSteps = listOf(1f, 2f, 4f)

/**
 * One trace, on a phone.
 *
 * **List-first, not a shrunken waterfall**, and that is a deliberate product call rather than a
 * limitation. At 360-430dp a name column wide enough to read leaves roughly 220dp of track, a 5-deep
 * span loses its name to indentation, and there is no hover to recover either. So the default view is
 * the span tree with a mini bar lane — which keeps position and extent, the two things a waterfall
 * actually contributes — and the real waterfall is a toggle for when someone wants it.
 *
 * The phone console answers "did this trace happen, and was it slow". Comparative latency analysis is
 * the desktop's job.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TraceDetailsScreen(
    traceId: String,
    onBackClick: () -> Unit,
) {
    val viewModel = koinViewModel<TraceDetailsViewModel> { parametersOf(traceId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lazyListState = rememberLazyListState()

    Scaffold(
        topBar = {
            AlohomoraTopBar(
                // The root span name, which is data rather than a re-title of the section.
                title = state.summary?.rootSpanName ?: "Trace",
                subtitle = state.summary?.let { "${it.spanCount} SPANS" },
                navigationIcon = {
                    AlohomoraIconButton(onClick = onBackClick) {
                        Icon(Icons.ArrowLeft, contentDescription = "back")
                    }
                },
                actions = {
                    AlohomoraIconButton(onClick = viewModel::toggleWaterfall) {
                        Icon(
                            Icons.ChartLine,
                            contentDescription = if (state.showWaterfall) {
                                "Show span list"
                            } else {
                                "Show waterfall"
                            },
                            tint = if (state.showWaterfall) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            TraceHeader(state)
            AlohomoraHorizontalDivider()

            if (state.showWaterfall) {
                WaterfallMode(state = state, viewModel = viewModel)
            } else {
                LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize()) {
                    items(state.rows, key = { it.span.spanId }) { row ->
                        TraceSpanRow(
                            row = row,
                            window = state.window,
                            isSelected = row.span.spanId == state.selectedSpanId,
                            onToggleCollapse = { viewModel.toggleCollapse(row.span.spanId) },
                            onSelect = { viewModel.selectSpan(row.span.spanId) },
                        )
                        AlohomoraHorizontalDivider()
                    }
                }
            }
        }
    }

    state.selectedSpan?.let { span ->
        AlohomoraBottomSheetModal(onDismissRequest = { viewModel.selectSpan(null) }) {
            SpanDetailSheet(
                span = span,
                children = state.selectedSpanChildren,
                traceStartNanos = state.window.startNanos,
            )
        }
    }
}

@Composable
private fun TraceHeader(state: TraceDetailsState) {
    val summary = state.summary ?: return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaterialTheme.dimens.margin.lg,
                vertical = MaterialTheme.dimens.margin.sm,
            ),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formatDuration(summary.durationNanos),
            style = MaterialTheme.typography.labelMedium,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            text = DateUtils.format(summary.startMillis, DateUtils.Format.HH_MM_SS_2MS),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (summary.hasError) {
            AlohomoraChip(
                label = "error",
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
        if (!summary.isComplete) {
            AlohomoraChip(
                label = "partial",
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The real waterfall, horizontally scrollable at a fixed zoom.
 *
 * Discrete zoom steps rather than pinch: pinch inside a vertically scrolling list needs custom
 * nested-scroll plus transform gestures, and getting that wrong makes the whole list feel broken —
 * a worse outcome than having no continuous zoom.
 */
@Composable
private fun WaterfallMode(state: TraceDetailsState, viewModel: TraceDetailsViewModel) {
    val viewportWidth = LocalWindowInfo.current.containerSize.width
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.dimens.margin.lg,
                vertical = MaterialTheme.dimens.margin.xs,
            ),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
        ) {
            ZoomSteps.forEach { step ->
                AlohomoraFilterChip(
                    label = "${step.toInt()}x",
                    selected = state.zoom == step,
                    onClick = { viewModel.setZoom(step) },
                    uppercase = false,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState()),
        ) {
            TraceWaterfall(
                rows = state.rows,
                window = state.window,
                selectedSpanId = state.selectedSpanId,
                // A wider name column than the desktop's 0.4: at this width a truncated span name is
                // useless, and the horizontal scroll means the track is not competing for the space.
                nameFraction = 0.45f,
                onNameFractionChange = {},
                onToggleCollapse = viewModel::toggleCollapse,
                onSelectSpan = viewModel::selectSpan,
                modifier = Modifier.width((viewportWidth * state.zoom).dp),
            )
        }
    }
}

/** Span detail, in a bottom sheet — the touch equivalent of the desktop's hover plus detail pane. */
@Composable
private fun SpanDetailSheet(span: Span, children: List<Span>, traceStartNanos: Long) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(MaterialTheme.dimens.margin.lg),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
    ) {
        Text(text = span.name, style = MaterialTheme.typography.titleSmall)
        SheetRow("Kind", span.kind)
        SheetRow("Status", span.statusCode + (span.statusDescription?.let { " · $it" } ?: ""))
        SheetRow("Start", "+${formatDuration(span.startEpochNanos - traceStartNanos)}")
        SheetRow(
            "Wall clock",
            DateUtils.format(span.startEpochMillis(), DateUtils.Format.HH_MM_SS_3MS),
        )
        SheetRow("Duration", formatDuration(span.durationNanos()))
        // The same derived figure the desktop shows: separates a slow dependency from a slow parent.
        SheetRow("Self time", formatDuration(selfTimeNanos(span, children)))
        SheetRow("Span id", span.spanId)
        SheetRow("Parent", span.parentSpanId ?: "— (root)")
        span.scopeName?.let { SheetRow("Scope", it) }

        span.attributes?.let { attributes ->
            AlohomoraHorizontalDivider()
            Text(text = "Attributes", style = MaterialTheme.typography.labelMedium)
            Text(
                text = attributes.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
            )
        }

        if (span.events.isNotEmpty()) {
            AlohomoraHorizontalDivider()
            Text(text = "Events", style = MaterialTheme.typography.labelMedium)
            span.events.forEach { event ->
                SheetRow(
                    event.name,
                    "+${formatDuration(event.epochNanos - traceStartNanos)}",
                )
                event.attributes?.forEach { (key, value) ->
                    Text(
                        text = "  $key = $value",
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
private fun SheetRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(start = MaterialTheme.dimens.margin.md),
        )
    }
}
