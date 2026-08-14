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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
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
import io.github.yashkasera.alohomora.ui.icons.ArrowLeft
import io.github.yashkasera.alohomora.ui.icons.ArrowLeftRight
import io.github.yashkasera.alohomora.ui.icons.Check
import io.github.yashkasera.alohomora.ui.icons.CircleAlert
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Trash
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags
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

        // Clear all confirmation bottom sheet
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
                placeholder = "Search endpoints",
            )
            Spacer(modifier = Modifier.size(MaterialTheme.dimens.margin.sm))
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.dimens.margin.xl),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
            ) {
                items(listOf("GET", "POST", "PUT", "PATCH", "DELETE")) { method ->
                    val isSelected = method.equals(selectedMethod, ignoreCase = true)
                    AlohomoraFilterChip(
                        label = method,
                        selected = isSelected,
                        modifier = Modifier.testTag(
                            AlohomoraTestTags.Traffic.methodFilter(method),
                        ),
                        onClick = {
                            if (method != selectedMethod) {
                                viewModel.setMethod(method)
                            } else {
                                viewModel.setMethod("")
                            }
                        },
                    )
                }
            }
            AlohomoraHorizontalDivider()
        }
    }
}

@Composable
private fun TrafficItem(call: TrafficEntry, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val containerColor = when {
        call.isViewed -> MaterialTheme.colorScheme.surfaceContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }

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
        colors = AlohomoraCardDefaults.colors(
            containerColor = containerColor,
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
                        ) {
                            Text(
                                text = "${call.duration}ms",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                    }
                }

                Text(
                    text = call.pathWithQuery(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = "${call.host}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

