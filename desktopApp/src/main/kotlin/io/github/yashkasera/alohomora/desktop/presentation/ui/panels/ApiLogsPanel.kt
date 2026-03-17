package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.common.TraceEntry
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.TraceItem
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevToolsViewModel
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraPrimaryTabRow
import io.github.yashkasera.alohomora.ui.components.AlohomoraTab
import io.github.yashkasera.alohomora.ui.components.jsonviewer.JsonTreeView
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.X
import kotlinx.coroutines.launch

@Composable
fun ApiLogsPanel(
    devToolsViewModel: DevToolsViewModel,
    onLogClick: (TraceEntry) -> Unit,
) {
    val logs by devToolsViewModel.apiLogs.collectAsState()
    val lazyListState = rememberLazyListState()

    ScaffoldContent(
        lazyListState = lazyListState,
        logs = logs,
        onLogClick = onLogClick,
    )
}

@Composable
private fun ScaffoldContent(
    lazyListState: androidx.compose.foundation.lazy.LazyListState,
    logs: List<TraceEntry>,
    onLogClick: (TraceEntry) -> Unit,
) {
    androidx.compose.material3.Scaffold(
        topBar = {
            AlohomoraTopBar(
                title = "Traces",
                subtitle = "Live trace entries from connected app",
                showDivider = lazyListState.canScrollBackward,
            )
        },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .padding(it)
                .fillMaxSize(),
        ) {
            items(logs) { log ->
                TraceItem(call = log, onClick = { onLogClick(log) })
                AlohomoraHorizontalDivider()
            }
        }
    }
}

@Composable
fun TraceDetailsSideModal(
    trace: TraceEntry,
    onDismiss: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.30f))
                .clickable(onClick = onDismiss),
        )
        Surface(
            modifier = Modifier
                .width(620.dp)
                .fillMaxHeight(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "API Request",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = trace.path ?: trace.url ?: "-",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    AlohomoraIconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.X, contentDescription = "Close")
                    }
                }
                AlohomoraHorizontalDivider()
                DesktopTraceDetailsContent(
                    trace = trace,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DesktopTraceDetailsContent(
    trace: TraceEntry,
    modifier: Modifier = Modifier,
) {
    val tabs = listOf("Overview", "Request", "Response")
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
                0 -> DesktopOverviewTab(trace = trace)
                1 -> DesktopRequestTab(trace = trace)
                else -> DesktopResponseTab(trace = trace)
            }
        }
    }
}

@Composable
private fun DesktopOverviewTab(trace: TraceEntry) {
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MethodChip(trace.method.orEmpty())
        Text(
            text = trace.pathWithQuery.ifBlank { trace.url.orEmpty() },
            style = MaterialTheme.typography.bodyMedium,
        )
        AlohomoraHorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        OverviewStatRow(
            status = trace.status?.toString() ?: "-",
            duration = "${trace.duration ?: 0} ms",
            size = "${trace.responseSize ?: 0} B",
        )
        KeyValueRow("Host", trace.host ?: "-")
        KeyValueRow("Scheme", trace.scheme ?: "-")
        KeyValueRow("Query", trace.query ?: "-")
        KeyValueRow("Timestamp", (trace.time ?: 0L).toString())
    }
}

@Composable
private fun DesktopRequestTab(trace: TraceEntry) {
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MethodChip(trace.method.orEmpty())
        Text(
            text = trace.url ?: "-",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SectionLabel("Headers")
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Text(
                text = trace.requestHeaders
                    ?.flatMap { (key, values) -> values.map { "$key: $it" } }
                    ?.joinToString("\n")
                    .orEmpty()
                    .ifBlank { "No headers" },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(12.dp),
            )
        }
        SectionLabel("Body")
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Text(
                text = trace.requestBody.orEmpty().ifBlank { "{}" },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}

@Composable
private fun DesktopResponseTab(trace: TraceEntry) {
    JsonTreeView(
        json = trace.responseBody.orEmpty().ifBlank { "{}" },
        parentContent = {
            item(key = "response_summary") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Status: ${trace.status ?: "-"}",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        text = "Duration: ${trace.duration ?: 0} ms",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                AlohomoraHorizontalDivider()
            }
        },
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun MethodChip(method: String) {
    Text(
        text = method.ifBlank { "UNKNOWN" }.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraSmall)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun OverviewStatRow(status: String, duration: String, size: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        KeyValueColumn("Status", status, Modifier.weight(1f))
        KeyValueColumn("Latency", duration, Modifier.weight(1f))
        KeyValueColumn("Size", size, Modifier.weight(1f))
    }
}

@Composable
private fun KeyValueColumn(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        SectionLabel(label)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun KeyValueRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        SectionLabel(label)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}
