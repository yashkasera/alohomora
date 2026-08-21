package io.github.yashkasera.alohomora.presentation.ui.screens.traffic.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.yashkasera.alohomora.common.DateUtils
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.ui.components.AlohomoraCard
import io.github.yashkasera.alohomora.ui.components.AlohomoraCardDefaults
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilterChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraSearchTextField
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.ui.components.ConfirmationBottomSheet
import io.github.yashkasera.alohomora.ui.components.ConfirmationConfig
import io.github.yashkasera.alohomora.ui.components.EmptyState
import io.github.yashkasera.alohomora.ui.components.FollowNewest
import io.github.yashkasera.alohomora.ui.components.MethodBadge
import io.github.yashkasera.alohomora.ui.components.ScrollToTopButton
import io.github.yashkasera.alohomora.ui.components.fabClearanceItem
import io.github.yashkasera.alohomora.ui.components.rememberViewedStateColors
import io.github.yashkasera.alohomora.ui.icons.ArrowLeft
import io.github.yashkasera.alohomora.ui.icons.ArrowLeftRight
import io.github.yashkasera.alohomora.ui.icons.Check
import io.github.yashkasera.alohomora.ui.icons.CircleAlert
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Trash
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import io.github.yashkasera.alohomora.ui.theme.alohomoraColors
import io.github.yashkasera.alohomora.ui.theme.dimens
import io.github.yashkasera.alohomora.ui.utils.drawDiagonalLabel
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun TrafficScreen(
    onTrafficClick: (String) -> Unit,
    onBackClick: () -> Unit,
) {
    val viewModel = koinViewModel<TrafficViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TrafficTopBar(onBackClick, state, viewModel)
        },
    ) { padding ->
        if (state.calls.isEmpty()) {
            EmptyState(
                icon = Icons.ArrowLeftRight,
                title = "No Network Requests",
                subtitle = "Network requests will appear here as your app makes API calls",
                modifier = Modifier.padding(padding),
            )
        } else {
            val listState = rememberLazyListState()
            FollowNewest(listState, state.calls.size)
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().testTag(AlohomoraTestTags.Traffic.LIST),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
                    contentPadding = PaddingValues(MaterialTheme.dimens.margin.md),
                ) {
                    items(state.calls, key = { it.id }) { call ->
                        TrafficItem(
                            call = call,
                            modifier = Modifier.testTag(AlohomoraTestTags.Traffic.item(call.id)),
                            onClick = { onTrafficClick(call.id) },
                        )
                    }
                    fabClearanceItem()
                }
                ScrollToTopButton(listState)
            }
        }

        if (state.showClearConfirmation) {
            ConfirmationBottomSheet(
                config = ConfirmationConfig(
                    title = "Clear All Traffic",
                    message = "Are you sure you want to delete all network request traffic? This action cannot be undone.",
                    confirmButtonText = "Clear All",
                    dismissButtonText = "Cancel",
                    isDestructive = true,
                ),
                onConfirm = viewModel::clearAllTraffic,
                onDismiss = viewModel::hideClearConfirmation,
            )
        }
    }
}

@Composable
private fun TrafficTopBar(
    onBackClick: () -> Unit,
    state: TrafficState,
    viewModel: TrafficViewModel,
) {
    val searchQuery by viewModel.query.collectAsStateWithLifecycle()
    val selectedMethod by viewModel.method.collectAsStateWithLifecycle()
    Column {
        AlohomoraTopBar(
            title = "Traffic",
            subtitle = if (state.calls.isEmpty()) null else "${state.calls.size} REQUESTS",
            navigationIcon = {
                AlohomoraIconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag(AlohomoraTestTags.Chrome.BACK),
                ) {
                    Icon(Icons.ArrowLeft, contentDescription = "back")
                }
            },
            actions = {
                if (state.calls.isNotEmpty()) {
                    AlohomoraIconButton(
                        onClick = viewModel::showClearConfirmation,
                        modifier = Modifier.testTag(AlohomoraTestTags.Chrome.CLEAR_ALL),
                    ) {
                        Icon(
                            imageVector = Icons.Trash,
                            contentDescription = "Clear All",
                        )
                    }
                }
            },
        )
        Column {
            AlohomoraSearchTextField(
                query = searchQuery,
                onQueryChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = MaterialTheme.dimens.margin.xl)
                    .testTag(AlohomoraTestTags.Chrome.SEARCH),
            )
            Spacer(modifier = Modifier.size(MaterialTheme.dimens.margin.sm))
            MethodFilterBar(
                selectedMethod = selectedMethod,
                onMethodClick = { method ->
                    if (method != selectedMethod) viewModel.setMethod(method)
                    else viewModel.setMethod("")
                },
            )
            AlohomoraHorizontalDivider()
        }
    }
}

@Composable
private fun TrafficItem(call: TrafficEntry, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val viewedColors = rememberViewedStateColors(call.isViewed)

    AlohomoraCard(
        onClick = onClick,
        modifier = if (call.isMocked()) {
            modifier.clipToBounds()
                .drawDiagonalLabel(
                    text = "MOCKED",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
        } else modifier,
        shape = MaterialTheme.shapes.large,
        colors = AlohomoraCardDefaults.colors(
            containerColor = viewedColors.containerColor.value,
        ),
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.dimens.margin.md)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
        ) {
            AlohomoraCard(
                modifier = Modifier.fillMaxHeight(),
                shape = MaterialTheme.shapes.medium,
                colors = AlohomoraCardDefaults.colors(
                    containerColor = if (call.isSuccessful())
                        MaterialTheme.alohomoraColors.successContainer
                    else
                        MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(MaterialTheme.dimens.margin.sm),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(
                        MaterialTheme.dimens.margin.xs,
                        Alignment.CenterVertically,
                    ),
                ) {
                    Icon(
                        imageVector = if (call.isSuccessful())
                            Icons.Check
                        else Icons.CircleAlert,
                        contentDescription = null,
                        modifier = Modifier.size(MaterialTheme.dimens.icon.md),
                    )
                    Text(
                        text = call.status?.toString() ?: "???",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                    ) {
                        MethodBadge(call.method.orEmpty())
                        Text(
                            text = DateUtils.format(call.time ?: 0, DateUtils.Format.HH_MM_SS_2MS),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (call.isMocked().not()) {
                        Text(
                            text = "${call.duration}ms",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }

                Text(
                    text = call.pathWithQuery(),
                    style = MaterialTheme.typography.titleSmall,
                    color = viewedColors.titleColor.value,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = call.host.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview
@Composable
private fun TrafficItemSuccessPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            TrafficItem(
                call = TrafficEntry(
                    id = "preview-1",
                    status = 200,
                    method = "GET",
                    host = "api.example.com",
                    path = "/v1/users/me/profile",
                    query = "include=avatar",
                    time = 1724234567000L,
                    duration = 142,
                    requestSize = 256,
                    responseSize = 4096,
                    responseContentType = "application/json",
                    isViewed = false,
                ),
                onClick = {},
                modifier = Modifier.padding(MaterialTheme.dimens.margin.md),
            )
        }
    }
}

@Preview
@Composable
private fun TrafficItemErrorPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            TrafficItem(
                call = TrafficEntry(
                    id = "preview-2",
                    status = 500,
                    method = "POST",
                    host = "api.example.com",
                    path = "/v1/orders",
                    time = 1724234568000L,
                    duration = 3200,
                    requestSize = 1024,
                    responseSize = 512,
                    responseContentType = "application/json",
                    isViewed = true,
                ),
                onClick = {},
                modifier = Modifier.padding(MaterialTheme.dimens.margin.md),
            )
        }
    }
}

@Preview
@Composable
private fun TrafficItemMockedPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            TrafficItem(
                call = TrafficEntry(
                    id = "preview-3",
                    status = 200,
                    method = "GET",
                    host = "api.example.com",
                    path = "/v1/config",
                    time = 1724234569000L,
                    duration = 5,
                    requestSize = 128,
                    responseSize = 2048,
                    responseContentType = "application/json",
                    isViewed = false,
                    mockedBy = "mock-rule-1",
                ),
                onClick = {},
                modifier = Modifier.padding(MaterialTheme.dimens.margin.md),
            )
        }
    }
}

@Composable
private fun MethodFilterBar(
    selectedMethod: String?,
    onMethodClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
        contentPadding = PaddingValues(horizontal = MaterialTheme.dimens.margin.xl),
    ) {
        items(listOf("GET", "POST", "PUT", "PATCH", "DELETE")) { method ->
            AlohomoraFilterChip(
                label = method,
                selected = method.equals(selectedMethod, ignoreCase = true),
                modifier = Modifier.testTag(
                    AlohomoraTestTags.Traffic.methodFilter(method),
                ),
                onClick = { onMethodClick(method) },
            )
        }
    }
}

@Preview
@Composable
private fun MethodFilterBarSelectedPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            MethodFilterBar(
                selectedMethod = "POST",
                onMethodClick = {},
                modifier = Modifier.padding(vertical = MaterialTheme.dimens.margin.sm),
            )
        }
    }
}

@Preview
@Composable
private fun MethodFilterBarNoneSelectedPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            MethodFilterBar(
                selectedMethod = null,
                onMethodClick = {},
                modifier = Modifier.padding(vertical = MaterialTheme.dimens.margin.sm),
            )
        }
    }
}

