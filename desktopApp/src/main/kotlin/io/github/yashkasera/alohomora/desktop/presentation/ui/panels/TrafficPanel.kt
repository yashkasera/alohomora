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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.AlohomoraSideSheet
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.ClearCapturedDialog
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.EmptyState
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.KeyValueRow
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.SectionLabel
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.TrafficItem
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevToolsViewModel
import io.github.yashkasera.alohomora.replay.replayBlockedReason
import io.github.yashkasera.alohomora.replay.toReplayRequest
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraPrimaryTabRow
import io.github.yashkasera.alohomora.ui.components.AlohomoraTab
import io.github.yashkasera.alohomora.ui.components.FollowNewest
import io.github.yashkasera.alohomora.ui.components.ScrollToTopButton
import io.github.yashkasera.alohomora.ui.components.jsonviewer.JsonTreeView
import io.github.yashkasera.alohomora.ui.icons.Copy
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Repeat
import io.github.yashkasera.alohomora.ui.icons.Server
import io.github.yashkasera.alohomora.ui.icons.Slack
import io.github.yashkasera.alohomora.ui.icons.Trash
import io.github.yashkasera.alohomora.ui.icons.X
import io.github.yashkasera.alohomora.ui.theme.dimens
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TrafficPanel(
    devToolsViewModel: DevToolsViewModel,
    onLogClick: (TrafficEntry) -> Unit,
) {
    val logs by devToolsViewModel.traffic.collectAsState()
    val lazyListState = rememberLazyListState()
    var showClearConfirmation by remember { mutableStateOf(false) }

    ScaffoldContent(
        lazyListState = lazyListState,
        logs = logs,
        onLogClick = { log ->
            // Dim the row as soon as it is opened; TrafficItem already styles on isViewed.
            devToolsViewModel.markTrafficViewed(log.id)
            onLogClick(log)
        },
        onClearRequested = { showClearConfirmation = true },
    )

    if (showClearConfirmation) {
        ClearCapturedDialog(
            title = "Clear all traffic?",
            message = "Captured traffic will be deleted from the device. This cannot be undone.",
            onConfirm = {
                devToolsViewModel.clearTraffic()
                showClearConfirmation = false
            },
            onDismiss = { showClearConfirmation = false },
        )
    }
}

@Composable
private fun ScaffoldContent(
    lazyListState: androidx.compose.foundation.lazy.LazyListState,
    logs: List<TrafficEntry>,
    onLogClick: (TrafficEntry) -> Unit,
    onClearRequested: () -> Unit,
) {
    androidx.compose.material3.Scaffold(
        topBar = {
            AlohomoraTopBar(
                title = "Traffic",
                subtitle = "Live traffic entries from connected app",
                showDivider = lazyListState.canScrollBackward,
                actions = {
                    AlohomoraIconButton(onClick = onClearRequested) {
                        Icon(
                            imageVector = Icons.Trash,
                            contentDescription = "Clear all traffic",
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Box(modifier = Modifier.padding(it).fillMaxSize()) {
            if (logs.isEmpty()) {
                EmptyState(
                    icon = Icons.Server,
                    title = "No traffic yet",
                    subtitle = "Requests appear here as the connected app makes them.",
                )
            } else {
                LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize()) {
                    items(logs, key = { log -> log.id }) { log ->
                        TrafficItem(call = log, onClick = { onLogClick(log) })
                        AlohomoraHorizontalDivider()
                    }
                }
                ScrollToTopButton(lazyListState)
            }
        }
        FollowNewest(lazyListState, logs.size)
    }
}

/**
 * Narrower than the Traces sheet: three tabs of key-value rows and a JSON tree need far less
 * horizontal room than a waterfall, where width buys time resolution.
 */
private const val TRAFFIC_SHEET_WIDTH_FRACTION = 0.5f

@Composable
fun TrafficDetailsSideSheet(
    traffic: TrafficEntry?,
    devToolsViewModel: DevToolsViewModel,
    onDismiss: () -> Unit,
) {
    // Keyed on the entry, not bare `remember`: this composable is reused as the selection changes,
    // so an un-keyed flag stays true across the switch and the dialog reopens for whichever entry
    // was clicked next. For replay that means a request the user never composed, pre-filled and one
    // click from being sent for real.
    var showSlackShareDialog by remember(traffic?.id) { mutableStateOf(false) }
    var showReplayDialog by remember(traffic?.id) { mutableStateOf(false) }
    val slackShareError by devToolsViewModel.slackShareError.collectAsState()
    val buildInfo by devToolsViewModel.buildInfo.collectAsState()
    val replayState by devToolsViewModel.replayState.collectAsState()
    val isSlackConfigured = buildInfo?.slackWebhookUrl.isNullOrBlank().not()

    // Null when the device cannot replay at all, or when this particular entry cannot be
    // reproduced — a truncated or multipart body. Both cases hide the action rather than offering
    // one that would send wrong data.
    val replayRequest = traffic
        ?.takeIf { replayState.supported }
        ?.toReplayRequest()
    val replayBlockedReason = traffic?.replayBlockedReason()

    AlohomoraSideSheet(
        visible = traffic != null,
        onDismiss = onDismiss,
        widthFraction = TRAFFIC_SHEET_WIDTH_FRACTION,
        header = {
            if (traffic != null) {
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
                        Text(
                            text = "API Request",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = traffic.path ?: traffic.url ?: "-",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (replayState.supported) {
                        AlohomoraIconButton(
                            onClick = { showReplayDialog = true },
                            enabled = replayRequest != null &&
                                !replayState.isInFlight(traffic.id),
                        ) {
                            Icon(
                                imageVector = Icons.Repeat,
                                // Names the blocker, so an unavailable action explains
                                // itself instead of just looking broken.
                                contentDescription = replayBlockedReason?.message
                                    ?: "Replay this request",
                            )
                        }
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
            }
        },
    ) {
        if (traffic != null) {
            DesktopTrafficDetailsContent(
                traffic = traffic,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    // Closes the dialog once a replay comes back clean, so success needs no extra click. A failure
    // leaves it open with the error and the user's edits intact — the usual cause is something in
    // the form itself.
    val replayInFlight = traffic != null && replayState.isInFlight(traffic.id)
    val replayError = traffic?.let { replayState.errorFor(it.id) }
    LaunchedEffect(replayInFlight, replayError) {
        if (showReplayDialog && !replayInFlight && replayError == null) showReplayDialog = false
    }

    if (showReplayDialog && replayRequest != null && traffic != null) {
        ReplayRequestDialog(
            initial = replayRequest,
            inFlight = replayState.isInFlight(traffic.id),
            error = replayState.errorFor(traffic.id),
            onDismiss = {
                showReplayDialog = false
                devToolsViewModel.dismissReplayError(traffic.id)
            },
            // Deliberately stays open: the device answers with a failure often enough — a hand-edited
            // URL, a refused connection — that closing on send would throw away both the error and
            // the edits that produced it.
            onSend = devToolsViewModel::replayTraffic,
        )
    }

    traffic?.let {
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
                    devToolsViewModel.shareCurlToSlack(traffic, email) {
                        showSlackShareDialog = false
                    }
                },
                onShareText = { email ->
                    devToolsViewModel.shareTextToSlack(traffic, email) {
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
fun DesktopTrafficDetailsContent(
    traffic: TrafficEntry,
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
                0 -> DesktopOverviewTab(traffic = traffic)
                1 -> DesktopRequestTab(traffic = traffic)
                else -> DesktopResponseTab(traffic = traffic)
            }
        }
    }
}

@Composable
private fun DesktopOverviewTab(traffic: TrafficEntry) {
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = MaterialTheme.dimens.margin.xl, vertical = MaterialTheme.dimens.margin.lg),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
    ) {
        MethodChip(traffic.method.orEmpty())
        Text(
            text = traffic.pathWithQuery().ifBlank { traffic.url.orEmpty() },
            style = MaterialTheme.typography.bodyMedium,
        )
        AlohomoraHorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        OverviewStatRow(
            status = traffic.status?.toString() ?: "-",
            duration = "${traffic.duration ?: 0} ms",
            size = "${traffic.responseSize ?: 0} B",
        )
        KeyValueRow("Host", traffic.host ?: "-")
        KeyValueRow("Scheme", traffic.scheme ?: "-")
        KeyValueRow("Query", traffic.query ?: "-")
        KeyValueRow("Timestamp", (traffic.time ?: 0L).toString())
    }
}

@Composable
private fun DesktopRequestTab(traffic: TrafficEntry) {
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = MaterialTheme.dimens.margin.xl, vertical = MaterialTheme.dimens.margin.lg),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
    ) {
        MethodChip(traffic.method.orEmpty())
        Text(
            text = traffic.url ?: "-",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SectionLabel("Headers")
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Text(
                text = traffic.requestHeaders
                    ?.flatMap { (key, values) -> values.map { "$key: $it" } }
                    ?.joinToString("\n")
                    .orEmpty()
                    .ifBlank { "No headers" },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(MaterialTheme.dimens.margin.md),
            )
        }
        SectionLabel("Body")
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Text(
                text = traffic.requestBody.orEmpty().ifBlank { "{}" },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(MaterialTheme.dimens.margin.md),
            )
        }
    }
}

@Composable
private fun DesktopResponseTab(traffic: TrafficEntry) {
    JsonTreeView(
        json = traffic.responseBody.orEmpty().ifBlank { "{}" },
        parentContent = {
            item(key = "response_summary") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.dimens.margin.xl, vertical = MaterialTheme.dimens.margin.md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Status: ${traffic.status ?: "-"}",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        text = "Duration: ${traffic.duration ?: 0} ms",
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
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md)) {
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
                        Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.sm))
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
                    // Deliberately reports only whether a URL arrived, never the URL itself:
                    // a Slack webhook is a live posting credential and this dialog ends up in
                    // screenshots and screen shares.
                    Text(
                        text = if (currentWebhookUrl.isNullOrBlank()) {
                            "The connected app did not send a webhook URL."
                        } else {
                            "A webhook URL was received but appears unusable."
                        },
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
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.lg),
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

