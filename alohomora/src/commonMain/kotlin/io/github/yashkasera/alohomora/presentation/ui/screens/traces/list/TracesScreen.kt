package io.github.yashkasera.alohomora.presentation.ui.screens.traces.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.yashkasera.alohomora.common.DateUtils
import io.github.yashkasera.alohomora.common.trace.TraceSummary
import io.github.yashkasera.alohomora.common.trace.formatDuration
import io.github.yashkasera.alohomora.ui.components.AlohomoraCard
import io.github.yashkasera.alohomora.ui.components.AlohomoraCardDefaults
import io.github.yashkasera.alohomora.ui.components.AlohomoraChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilterChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraSearchTextField
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.ui.components.EmptyState
import io.github.yashkasera.alohomora.ui.components.ScrollToTopButton
import io.github.yashkasera.alohomora.ui.components.fabClearanceItem
import io.github.yashkasera.alohomora.ui.icons.ArrowLeft
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Trash
import io.github.yashkasera.alohomora.ui.icons.Waypoints
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags
import io.github.yashkasera.alohomora.ui.theme.dimens
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
                    AlohomoraIconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag(AlohomoraTestTags.Chrome.BACK),
                    ) {
                        Icon(Icons.ArrowLeft, contentDescription = "back")
                    }
                },
                actions = {
                    AlohomoraIconButton(
                        onClick = { viewModel.clearAllTraces() },
                        modifier = Modifier.testTag(AlohomoraTestTags.Chrome.CLEAR_ALL),
                    ) {
                        Icon(
                            Icons.Trash,
                            contentDescription = "Clear all traces",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.dimens.margin.md),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlohomoraSearchTextField(
                    query = state.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    placeholder = "Filter traces",
                    onClear = { viewModel.onSearchQueryChange("") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag(AlohomoraTestTags.Chrome.SEARCH),
                )
                AlohomoraFilterChip(
                    label = "Errors",
                    selected = state.errorsOnly,
                    modifier = Modifier.testTag(AlohomoraTestTags.Traces.ERROR_FILTER),
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
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize().testTag(AlohomoraTestTags.Traces.LIST),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
                        contentPadding = PaddingValues(MaterialTheme.dimens.margin.md),
                    ) {
                        items(state.traces, key = { it.traceId }) { trace ->
                            TraceRowItem(
                                trace = trace,
                                modifier = Modifier.testTag(
                                    AlohomoraTestTags.Traces.item(trace.traceId),
                                ),
                                onClick = { onNavigateToTrace(trace.traceId) },
                            )
                        }
                        fabClearanceItem()
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
private fun TraceRowItem(trace: TraceSummary, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val containerColor = when {
        trace.isViewed -> MaterialTheme.colorScheme.surfaceContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }

    AlohomoraCard(
        onClick = onClick,
        modifier = modifier,
        colors = AlohomoraCardDefaults.colors(
            containerColor = containerColor,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.dimens.margin.md),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = trace.rootSpanName ?: "(root pending)",
                    style = MaterialTheme.typography.labelMedium,
                    // Unread is carried by contrast rather than weight — see WaterfallRow.
                    color = if (trace.isViewed) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = formatDuration(trace.durationNanos),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = DateUtils.format(trace.startMillis, DateUtils.Format.HH_MM_SS_2MS),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${trace.spanCount} ${if (trace.spanCount == 1) "span" else "spans"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
