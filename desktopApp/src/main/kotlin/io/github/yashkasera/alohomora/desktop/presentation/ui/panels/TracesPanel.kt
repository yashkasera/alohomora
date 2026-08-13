package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.ClearCapturedDialog
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.EmptyState
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.TraceItem
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.TracesViewModel
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilterChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraSearchTextField
import io.github.yashkasera.alohomora.ui.components.FollowNewest
import io.github.yashkasera.alohomora.ui.components.ScrollToTopButton
import io.github.yashkasera.alohomora.ui.components.fabClearanceItem
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Trash
import io.github.yashkasera.alohomora.ui.icons.Waypoints
import io.github.yashkasera.alohomora.ui.theme.dimens

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TracesPanel(
    tracesViewModel: TracesViewModel,
    onTraceClick: (String) -> Unit,
) {
    val traces by tracesViewModel.traces.collectAsState()
    val query by tracesViewModel.query.collectAsState()
    val errorsOnly by tracesViewModel.errorsOnly.collectAsState()
    val captureSupported by tracesViewModel.captureSupported.collectAsState()
    val spanCount by tracesViewModel.spanCount.collectAsState()
    val lazyListState = rememberLazyListState()
    var showClearConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AlohomoraTopBar(
                title = "Traces",
                subtitle = "Spans grouped by trace, newest first",
                actions = {
                    AlohomoraIconButton(onClick = { showClearConfirmation = true }) {
                        Icon(
                            imageVector = Icons.Trash,
                            contentDescription = "Clear all traces",
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Search is worth having here where the other panels have none: a traceId is opaque and
            // root span names repeat constantly across traces of the same operation.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaterialTheme.dimens.margin.xxl,
                        vertical = MaterialTheme.dimens.margin.sm,
                    ),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlohomoraSearchTextField(
                    query = query,
                    onQueryChange = tracesViewModel::onQueryChange,
                    placeholder = "Filter by span name, scope or trace id",
                    onClear = { tracesViewModel.onQueryChange("") },
                    modifier = Modifier.weight(1f),
                )
                AlohomoraFilterChip(
                    label = "Errors",
                    selected = errorsOnly,
                    onClick = { tracesViewModel.onErrorsOnlyChange(!errorsOnly) },
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (traces.isEmpty()) {
                    TracesEmptyState(
                        captureSupported = captureSupported,
                        isFiltered = query.isNotBlank() || errorsOnly,
                        hasSpans = spanCount > 0,
                    )
                } else {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
                        contentPadding = PaddingValues(
                            MaterialTheme.dimens.margin.md
                        )
                    ) {
                        items(traces, key = { it.traceId }) { trace ->
                            TraceItem(trace = trace, onClick = { onTraceClick(trace.traceId) })
                        }
                        fabClearanceItem()
                    }
                    ScrollToTopButton(lazyListState)
                }
            }
        }
        FollowNewest(lazyListState, traces.size)
    }

    if (showClearConfirmation) {
        ClearCapturedDialog(
            title = "Clear all traces?",
            message = "Captured spans will be deleted from the device. This cannot be undone.",
            onConfirm = {
                tracesViewModel.clearTraces()
                showClearConfirmation = false
            },
            onDismiss = { showClearConfirmation = false },
        )
    }
}

/**
 * Distinguishes the three reasons this panel can be empty.
 *
 * Worth the branching: "the app has no tracer wired up" and "no traces have happened yet" look
 * identical but need opposite actions from the reader, and telling them apart is the entire reason
 * `spanCaptureSupported` crosses the wire.
 */
@Composable
private fun TracesEmptyState(captureSupported: Boolean, isFiltered: Boolean, hasSpans: Boolean) {
    when {
        isFiltered && hasSpans -> EmptyState(
            icon = Icons.Waypoints,
            title = "No traces match",
            subtitle = "Clear the filter to see every captured trace.",
        )

        !captureSupported -> EmptyState(
            icon = Icons.Waypoints,
            title = "No tracer connected",
            subtitle = "This app has not recorded a span yet. Bridge your tracer to " +
                "Alohomora.recordSpan(...) — the README has adapters for OpenTelemetry and Sentry.",
        )

        else -> EmptyState(
            icon = Icons.Waypoints,
            title = "No traces yet",
            subtitle = "Traces appear here as the connected app records spans.",
        )
    }
}
