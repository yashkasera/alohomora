package io.github.yashkasera.alohomora.presentation.ui.screens.traces.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
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
import io.github.yashkasera.alohomora.ui.components.rememberViewedStateColors
import io.github.yashkasera.alohomora.ui.icons.ArrowLeft
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Trash
import io.github.yashkasera.alohomora.ui.icons.Waypoints
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags
import io.github.yashkasera.alohomora.ui.theme.AppTheme
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

@Composable
private fun TraceRowItem(trace: TraceSummary, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val viewedColors = rememberViewedStateColors(trace.isViewed)
    val iconTint = if (trace.hasError)
        MaterialTheme.colorScheme.error
    else
        MaterialTheme.colorScheme.tertiary

    AlohomoraCard(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = AlohomoraCardDefaults.colors(
            containerColor = viewedColors.containerColor.value,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.dimens.margin.md),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(MaterialTheme.dimens.icon.xl)
                    .background(iconTint.copy(alpha = 0.12f), MaterialShapes.Clover4Leaf.toShape()),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Waypoints,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(MaterialTheme.dimens.icon.lg),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xs),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = trace.rootSpanName ?: "(root pending)",
                        style = MaterialTheme.typography.titleSmall,
                        color = viewedColors.titleColor.value,
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
}

@Preview
@Composable
private fun TraceRowItemPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
                modifier = Modifier.padding(MaterialTheme.dimens.margin.md),
            ) {
                TraceRowItem(
                    trace = TraceSummary(
                        traceId = "abcdef1234567890abcdef1234567890",
                        rootSpanName = "GET /api/v1/users",
                        startMillis = 1724234567000L,
                        durationNanos = 245_000_000L,
                        spanCount = 5,
                        hasError = false,
                        isComplete = true,
                        isViewed = false,
                        scopeName = "io.ktor.client",
                    ),
                    onClick = {},
                )
                TraceRowItem(
                    trace = TraceSummary(
                        traceId = "fedcba0987654321fedcba0987654321",
                        rootSpanName = "POST /api/v1/orders",
                        startMillis = 1724234590000L,
                        durationNanos = 1_820_000_000L,
                        spanCount = 12,
                        hasError = true,
                        isComplete = false,
                        isViewed = true,
                        scopeName = null,
                    ),
                    onClick = {},
                )
            }
        }
    }
}

@Preview
@Composable
private fun TraceRowItemPendingPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            TraceRowItem(
                trace = TraceSummary(
                    traceId = "11111111111111111111111111111111",
                    rootSpanName = null,
                    startMillis = 1724234600000L,
                    durationNanos = 0L,
                    spanCount = 1,
                    hasError = false,
                    isComplete = false,
                    isViewed = false,
                    scopeName = null,
                ),
                onClick = {},
                modifier = Modifier.padding(MaterialTheme.dimens.margin.md),
            )
        }
    }
}
