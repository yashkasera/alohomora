package io.github.yashkasera.alohomora.presentation.ui.screens.traffic.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.yashkasera.alohomora.common.DateUtils
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.presentation.ui.components.EmptyState
import io.github.yashkasera.alohomora.ui.components.AlohomoraChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilterChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraSearchTextField
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.ui.components.ConfirmationBottomSheet
import io.github.yashkasera.alohomora.ui.components.ConfirmationConfig
import io.github.yashkasera.alohomora.ui.components.FollowNewest
import io.github.yashkasera.alohomora.ui.components.ScrollToTopButton
import io.github.yashkasera.alohomora.ui.icons.ArrowLeft
import io.github.yashkasera.alohomora.ui.icons.ArrowLeftRight
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Trash
import io.github.yashkasera.alohomora.ui.theme.dimens
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun TrafficScreen(
    onTrafficClick: (String) -> Unit,
    onBackClick: () -> Unit,
) {
    val viewModel = koinViewModel<TrafficViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
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
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(state.calls, key = { it.id }) { call ->
                        TrafficItem(call = call, onClick = { onTrafficClick(call.id) })
                        AlohomoraHorizontalDivider()
                    }
                    // Spacer to avoid bottom bar overlap if scaffold padding isn't enough (usually it is)
                    item { Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xxxl)) }
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
                AlohomoraIconButton(onClick = onBackClick) {
                    Icon(Icons.ArrowLeft, contentDescription = "back")
                }
            },
            actions = {
                if (state.calls.isNotEmpty()) {
                    AlohomoraIconButton(
                        onClick = viewModel::showClearConfirmation,
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
                    .padding(horizontal = MaterialTheme.dimens.margin.xl),
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
private fun TrafficItem(call: TrafficEntry, onClick: () -> Unit) {
    val containerColor = when {
        call.isSuccessful().not() -> MaterialTheme.colorScheme.errorContainer
        call.isViewed -> MaterialTheme.colorScheme.surfaceContainerLowest
        else -> MaterialTheme.colorScheme.background
    }

    Column(
        modifier = Modifier.fillMaxWidth()
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(
                horizontal = MaterialTheme.dimens.margin.xl,
                vertical = MaterialTheme.dimens.margin.lg,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
            ) {
                Text(
                    text = "${call.duration}ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )

                val statusColor = when {
                    call.isSuccessful() -> MaterialTheme.colorScheme.onSurface
                    // Not softened by isViewed: having read a failed request does not make it
                    // succeed, and the previous rule dropped the red as soon as it was opened.
                    else -> MaterialTheme.colorScheme.error
                }
                // Override for 201 -> Emerald using theme color
                val finalStatusColor =
                    if (call.status == 201)
                        MaterialTheme.colorScheme.tertiary
                    else statusColor

                Text(
                    text = "${call.status}",
                    style = MaterialTheme.typography.labelSmall,
                    color = finalStatusColor,
                )
            }
        }

        Text(
            text = call.pathWithQuery(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = MaterialTheme.dimens.margin.xxxl),
        )

        Text(
            text = "host: ${call.host}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MethodBadge(method: String) {
    val isWrite = method in listOf("POST", "PUT", "PATCH", "DELETE")

    val backgroundColor =
        if (isWrite) MaterialTheme.colorScheme.inverseSurface
        else Color.Transparent

    val contentColor = if (isWrite) MaterialTheme.colorScheme.inverseOnSurface
    else MaterialTheme.colorScheme.onSurface

    AlohomoraChip(
        label = method,
        uppercase = true,
        containerColor = backgroundColor,
        contentColor = contentColor,
        borderStroke = BorderStroke(
            width = MaterialTheme.dimens.stroke.small,
            color = contentColor,
        ).takeIf { !isWrite },
    )
}

