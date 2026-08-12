package io.github.yashkasera.alohomora.presentation.ui.screens.traces.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.yashkasera.alohomora.common.DateUtils
import io.github.yashkasera.alohomora.common.trace.TraceSummary
import io.github.yashkasera.alohomora.common.trace.formatDuration
import io.github.yashkasera.alohomora.presentation.ui.components.EmptyState
import io.github.yashkasera.alohomora.ui.components.AlohomoraChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilterChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraSearchTextField
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.ui.components.ScrollToTopButton
import io.github.yashkasera.alohomora.ui.icons.ArrowLeft
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Trash
import io.github.yashkasera.alohomora.ui.icons.Waypoints
import io.github.yashkasera.alohomora.ui.theme.dimens
import io.github.yashkasera.alohomora.ui.theme.muted
import io.github.yashkasera.alohomora.ui.theme.mutedContainer
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun TracesScreen(
    onBackClick: () -> Unit,
    onNavigateToTrace: (traceId: String) -> Unit,
) {
    val viewModel = koinViewModel<TracesViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lazyListState = rememberLazyListState()

    Scaffold(
        topBar = {
            AlohomoraTopBar(
                title = "Traces",
                subtitle = if (state.traces.isEmpty()) null else "${state.traces.size} TRACES",
                navigationIcon = {
                    AlohomoraIconButton(onClick = onBackClick) {
                        Icon(Icons.ArrowLeft, contentDescription = "back")
                    }
                },
                actions = {
                    AlohomoraIconButton(onClick = { viewModel.clearAllTraces() }) {
                        Icon(
                            Icons.Trash,
                            contentDescription = "Clear all traces",
                            tint = MaterialTheme.colorScheme.onSurface,
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
                AlohomoraSearchTextField(
                    query = state.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    placeholder = "Filter traces",
                    onClear = { viewModel.onSearchQueryChange("") },
                    modifier = Modifier.weight(1f),
                )
                AlohomoraFilterChip(
                    label = "Errors",
                    selected = state.errorsOnly,
                    onClick = { viewModel.onErrorsOnlyChange(!state.errorsOnly) },
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (state.traces.isEmpty()) {
                    EmptyState(
                        icon = Icons.Waypoints,
                        title = "No traces yet",
                        subtitle = "Traces appear here once your tracer is bridged to " +
                            "Alohomora.recordSpan(...).",
                    )
                } else {
                    LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize()) {
                        items(state.traces, key = { it.traceId }) { trace ->
                            TraceRowItem(trace = trace, onClick = { onNavigateToTrace(trace.traceId) })
                            AlohomoraHorizontalDivider()
                        }
                    }
                    ScrollToTopButton(lazyListState)
                }
            }
        }
    }
}

/**
 * A trace as a phone-width row.
 *
 * Drops the `traceId` column the desktop shows — at this width it displaces the span name, which is the
 * only thing that identifies the trace to a human.
 */
@Composable
private fun TraceRowItem(trace: TraceSummary, onClick: () -> Unit) {
    val containerColor = when {
        trace.hasError -> MaterialTheme.colorScheme.errorContainer
        trace.isViewed -> MaterialTheme.colorScheme.surfaceContainerLowest
        else -> MaterialTheme.colorScheme.surface
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(MaterialTheme.dimens.margin.lg),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = trace.rootSpanName ?: "(root pending)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (trace.isViewed) FontWeight.Normal else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatDuration(trace.durationNanos),
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = DateUtils.format(trace.startMillis, DateUtils.Format.HH_MM_SS_2MS),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.muted,
            )
            Text(
                text = "${trace.spanCount} ${if (trace.spanCount == 1) "span" else "spans"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.muted,
            )
            if (trace.hasError) {
                AlohomoraChip(
                    label = "error",
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            if (!trace.isComplete) {
                AlohomoraChip(
                    label = "partial",
                    containerColor = MaterialTheme.colorScheme.mutedContainer,
                    contentColor = MaterialTheme.colorScheme.muted,
                )
            }
        }
    }
}
