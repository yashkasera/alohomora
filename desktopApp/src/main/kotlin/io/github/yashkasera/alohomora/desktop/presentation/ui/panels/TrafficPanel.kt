package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.desktop.presentation.model.TrafficUiState
import io.github.yashkasera.alohomora.desktop.presentation.model.trafficSubtitle
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.AlohomoraSideSheet
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.ClearCapturedDialog
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.EmptyState
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.KeyValueRow
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.SectionLabel
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.SlackShareDialog
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.TrafficItem
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevToolsViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.NetworkRulesViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.TrafficViewModel
import io.github.yashkasera.alohomora.replay.replayBlockedReason
import io.github.yashkasera.alohomora.replay.toReplayRequest
import io.github.yashkasera.alohomora.ui.components.AlohomoraChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraCodeBlock
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilterChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraOutlinedButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraPrimaryTabRow
import io.github.yashkasera.alohomora.ui.components.AlohomoraSearchTextField
import io.github.yashkasera.alohomora.ui.components.AlohomoraTab
import io.github.yashkasera.alohomora.ui.components.FollowNewest
import io.github.yashkasera.alohomora.ui.components.ScrollToTopButton
import io.github.yashkasera.alohomora.ui.components.jsonviewer.JsonTreeView
import io.github.yashkasera.alohomora.ui.icons.Copy
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Repeat
import io.github.yashkasera.alohomora.ui.icons.Search
import io.github.yashkasera.alohomora.ui.icons.Server
import io.github.yashkasera.alohomora.ui.icons.Slack
import io.github.yashkasera.alohomora.ui.icons.Trash
import io.github.yashkasera.alohomora.ui.icons.X
import io.github.yashkasera.alohomora.ui.theme.dimens
import kotlinx.coroutines.launch

@Composable
fun TrafficPanel(
    trafficViewModel: TrafficViewModel,
    networkRulesViewModel: NetworkRulesViewModel,
    onLogClick: (TrafficEntry) -> Unit,
    onOpenMockRules: () -> Unit = {},
) {
    val uiState by trafficViewModel.uiState.collectAsState()
    val query by trafficViewModel.query.collectAsState()
    val lazyListState = rememberLazyListState()
    var showClearConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AlohomoraTopBar(
                title = "Traffic",
                subtitle = trafficSubtitle(uiState),
                actions = {
                    NetworkRulesActions(
                        viewModel = networkRulesViewModel,
                        onOpenMockRules = onOpenMockRules,
                    )
                    AlohomoraIconButton(onClick = { showClearConfirmation = true }) {
                        Icon(
                            imageVector = Icons.Trash,
                            contentDescription = "Clear all traffic",
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TrafficFilters(
                state = uiState,
                query = query,
                onQueryChange = trafficViewModel::onQueryChange,
                onMethodToggle = trafficViewModel::onMethodToggle,
                onErrorsOnlyChange = trafficViewModel::onErrorsOnlyChange,
                onClearFilters = trafficViewModel::clearFilters,
            )
            AlohomoraHorizontalDivider()

            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.entries.isEmpty()) {
                    TrafficEmptyState(uiState, onClearFilters = trafficViewModel::clearFilters)
                } else {
                    LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize()) {
                        items(uiState.entries, key = { log -> log.id }) { log ->
                            TrafficItem(
                                call = log,
                                onClick = {
                                    trafficViewModel.markViewed(log)
                                    onLogClick(log)
                                },
                            )
                            AlohomoraHorizontalDivider()
                        }
                    }
                    ScrollToTopButton(lazyListState)
                }
            }
        }
        FollowNewest(lazyListState, uiState.entries.size)
    }

    if (showClearConfirmation) {
        ClearCapturedDialog(
            title = "Clear all traffic?",
            message = "Captured traffic will be deleted from the device. This cannot be undone.",
            onConfirm = {
                trafficViewModel.clearTraffic()
                showClearConfirmation = false
            },
            onDismiss = { showClearConfirmation = false },
        )
    }
}

/**
 * Search plus a method filter, laid out like the Traces and Events rows so the three panels read alike.
 */
@Composable
private fun TrafficFilters(
    state: TrafficUiState,
    query: String,
    onQueryChange: (String) -> Unit,
    onMethodToggle: (String) -> Unit,
    onErrorsOnlyChange: (Boolean) -> Unit,
    onClearFilters: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
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
                onQueryChange = onQueryChange,
                // Names the three things matched. Bodies are not searched — see TrafficEntry.searchHaystack.
                placeholder = "Filter by URL, method or status",
                onClear = { onQueryChange("") },
                modifier = Modifier.weight(1f),
            )
            // Counted in the label so the chip reports whether pressing it will show anything, rather than
            // making the user press it to find out.
            AlohomoraFilterChip(
                label = if (state.errorCount > 0) "Errors · ${state.errorCount}" else "Errors",
                selected = state.filters.errorsOnly,
                uppercase = false,
                onClick = { onErrorsOnlyChange(!state.filters.errorsOnly) },
            )
            if (state.filters.hasFilter) {
                AlohomoraFilterChip(
                    label = "Clear filters",
                    selected = false,
                    onClick = onClearFilters,
                )
            }
        }

        if (state.methods.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = MaterialTheme.dimens.margin.xxl,
                        end = MaterialTheme.dimens.margin.xxl,
                        bottom = MaterialTheme.dimens.margin.sm,
                    ),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items(state.methods, key = { it }) { method ->
                    // An include set, so nothing selected shows everything: with no chip lit the list is
                    // unfiltered rather than empty.
                    AlohomoraFilterChip(
                        label = "$method · ${state.methodCounts[method] ?: 0}",
                        selected = method in state.filters.methods,
                        uppercase = false,
                        onClick = { onMethodToggle(method) },
                    )
                }
            }
        }
    }
}

/**
 * Distinguishes "nothing captured" from "the filters hid it all", the way the Traces panel does — the two
 * need opposite actions from the reader.
 */
@Composable
private fun TrafficEmptyState(state: TrafficUiState, onClearFilters: () -> Unit) {
    if (state.totalCount == 0) {
        EmptyState(
            icon = Icons.Server,
            title = "No traffic yet",
            subtitle = "Requests appear here as the connected app makes them.",
        )
    } else {
        EmptyState(
            icon = Icons.Search,
            title = "No requests match",
            subtitle = "${state.totalCount} captured. Clear the filters to see them.",
            action = {
                AlohomoraOutlinedButton(text = "Clear filters", onClick = onClearFilters)
            },
        )
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
    networkRulesViewModel: NetworkRulesViewModel? = null,
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
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = traffic.path ?: traffic.url ?: "-",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
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
                                contentDescription = replayBlockedReason?.message
                                    ?: "Replay this request",
                            )
                        }
                    }
                    if (networkRulesViewModel != null) {
                        val networkRulesSupported by networkRulesViewModel.networkRulesSupported.collectAsState()
                        if (networkRulesSupported) {
                            AlohomoraIconButton(
                                onClick = { networkRulesViewModel.addRuleFromTraffic(traffic) },
                            ) {
                                Icon(
                                    imageVector = Icons.Copy,
                                    contentDescription = "Create mock from this request",
                                )
                            }
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

    if (replayRequest != null && traffic != null) {
        ReplayRequestSideSheet(
            visible = showReplayDialog,
            initial = replayRequest,
            inFlight = replayState.isInFlight(traffic.id),
            error = replayState.errorFor(traffic.id),
            onDismiss = {
                showReplayDialog = false
                devToolsViewModel.dismissReplayError(traffic.id)
            },
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
            .padding(
                horizontal = MaterialTheme.dimens.margin.xl,
                vertical = MaterialTheme.dimens.margin.lg,
            ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MethodChip(traffic.method.orEmpty())
            if (traffic.mockedBy != null) {
                AlohomoraChip(
                    label = "Mocked",
                    uppercase = false,
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                )
            }
        }
        SelectionContainer {
            Text(
                text = traffic.pathWithQuery().ifBlank { traffic.url.orEmpty() },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = MaterialTheme.dimens.margin.xl,
                vertical = MaterialTheme.dimens.margin.lg,
            ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
    ) {
        MethodChip(traffic.method.orEmpty())
        SelectionContainer {
            Text(
                text = traffic.url ?: "-",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        SectionLabel("Headers")

        AlohomoraHorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        AlohomoraCodeBlock(
            content = traffic.requestHeaders
                ?.flatMap { (key, values) -> values.map { "$key: $it" } }
                ?.joinToString("\n")
                .orEmpty()
                .ifBlank { "No headers" },
        )
        SectionLabel("Body")
        AlohomoraCodeBlock(
            content = traffic.requestBody.orEmpty().ifBlank { "{}" },
        )
    }
}

@Composable
private fun DesktopResponseTab(traffic: TrafficEntry) {
    Column() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MaterialTheme.dimens.margin.xl,
                    vertical = MaterialTheme.dimens.margin.md,
                ),
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
        Box(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.dimens.margin.xl,
                vertical = MaterialTheme.dimens.margin.lg,
            )
        ){
            JsonTreeView(
                json = traffic.responseBody.orEmpty().ifBlank { "{}" },
            )
        }
    }
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

