package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes.Companion.Puffy
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import io.github.yashkasera.alohomora.common.TraceEntry
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.TraceItem
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevToolsViewModel
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraPrimaryTabRow
import io.github.yashkasera.alohomora.ui.components.AlohomoraTab
import io.github.yashkasera.alohomora.ui.components.jsonviewer.JsonTreeView
import io.github.yashkasera.alohomora.ui.icons.Copy
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Slack
import io.github.yashkasera.alohomora.ui.icons.X
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
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
    trace: TraceEntry?,
    devToolsViewModel: DevToolsViewModel,
    onDismiss: () -> Unit,
) {
    var showSlackShareDialog by remember { mutableStateOf(false) }
    val slackShareError by devToolsViewModel.slackShareError.collectAsState()
    val buildInfo by devToolsViewModel.buildInfo.collectAsState()
    val isSlackConfigured = buildInfo?.slackWebhookUrl.isNullOrBlank().not()

    AnimatedVisibility(
        trace != null,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.30f))
                    .clickable(
                        indication = null,
                        interactionSource = null,
                        onClick = onDismiss,
                    ),
            )
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInHorizontally { -it },
                exit = fadeOut() + slideOutHorizontally { it },
            ) {
                trace?.let {
                    Surface(
                        modifier = Modifier
                            .width(620.dp)
                            .fillMaxHeight(),
                        color = MaterialTheme.colorScheme.background,
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
                                AlohomoraIconButton(onClick = { showSlackShareDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Slack,
                                        contentDescription = "Share to Slack",
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
        }
    }

    trace?.let {
        if (showSlackShareDialog) {
            SlackShareDialog(
                isConfigured = isSlackConfigured,
                currentWebhookUrl = buildInfo?.slackWebhookUrl,
                shareError = slackShareError,
                onDismiss = {
                    showSlackShareDialog = false
                    devToolsViewModel.clearSlackShareError()
                },
                onShareCurl = { email ->
                    devToolsViewModel.shareCurlToSlack(trace, email) {
                        showSlackShareDialog = false
                    }
                },
                onShareText = { email ->
                    devToolsViewModel.shareTextToSlack(trace, email) {
                        showSlackShareDialog = false
                    }
                },
                onClearError = devToolsViewModel::clearSlackShareError,
            )
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
private fun SlackShareDialog(
    isConfigured: Boolean,
    currentWebhookUrl: String?,
    shareError: String?,
    onDismiss: () -> Unit,
    onShareCurl: (String) -> Unit,
    onShareText: (String) -> Unit,
    onClearError: () -> Unit,
) {
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share to Slack") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isConfigured) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            if (shareError != null) onClearError()
                        },
                        label = { Text("Recipient Email") },
                        placeholder = { Text("abc.xyz@example.org") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "This will share in the DM with the specified user",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = { onShareCurl(email) },
                        enabled = email.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Share cURL to Slack")
                    }
                    Button(
                        onClick = { onShareText(email) },
                        enabled = email.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(imageVector = Icons.Copy, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share Text to Slack")
                    }
                    if (!shareError.isNullOrBlank()) {
                        Text(
                            text = shareError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                } else {
                    Text(
                        text = "Slack is not configured.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Configure slackWebhookUrl in your mobile Alohomora build config.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Desktop received webhook: ${currentWebhookUrl ?: "<null>"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
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
