package io.github.yashkasera.alohomora.presentation.ui.screens.trace.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.yashkasera.alohomora.common.DateUtils
import io.github.yashkasera.alohomora.common.TraceEntry
import io.github.yashkasera.alohomora.presentation.ui.components.EmptyState
import io.github.yashkasera.alohomora.ui.components.AlohomoraChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilterChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraSearchTextField
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.ui.components.ConfirmationBottomSheet
import io.github.yashkasera.alohomora.ui.components.ConfirmationConfig
import io.github.yashkasera.alohomora.ui.icons.ArrowLeft
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Server
import io.github.yashkasera.alohomora.ui.icons.Trash
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun TraceScreen(
    onTraceClick: (String) -> Unit,
    onBackClick: () -> Unit,
) {
    val viewModel = koinViewModel<TraceViewModel>()
    val state by viewModel.state.collectAsState()

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            TraceTopBar(onBackClick, state, viewModel)
        },
    ) { padding ->
        if (state.calls.isEmpty()) {
            EmptyState(
                icon = Icons.Server,
                title = "No Network Requests",
                subtitle = "Network requests will appear here as your app makes API calls",
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                items(state.calls) { call ->
                    TraceItem(call = call, onClick = { onTraceClick(call.id) })
                    AlohomoraHorizontalDivider()
                }
                // Spacer to avoid bottom bar overlap if scaffold padding isn't enough (usually it is)
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }

        // Clear all confirmation bottom sheet
        if (state.showClearConfirmation) {
            ConfirmationBottomSheet(
                config = ConfirmationConfig(
                    title = "Clear All Traces",
                    message = "Are you sure you want to delete all network request traces? This action cannot be undone.",
                    confirmButtonText = "Clear All",
                    dismissButtonText = "Cancel",
                    isDestructive = true,
                ),
                onConfirm = viewModel::clearAllTraces,
                onDismiss = viewModel::hideClearConfirmation,
            )
        }
    }
}

@Composable
private fun TraceTopBar(
    onBackClick: () -> Unit,
    state: TraceState,
    viewModel: TraceViewModel,
) {
    val searchQuery by viewModel.query.collectAsStateWithLifecycle()
    val selectedMethod by viewModel.method.collectAsStateWithLifecycle()
    Column {
        AlohomoraTopBar(
            title = "Traffic Logs",
            navigationIcon = {
                AlohomoraIconButton(onClick = onBackClick) {
                    Icon(Icons.ArrowLeft, contentDescription = "back")
                }
            },
            actions = {
                // Clear all button (only show if there are traces)
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
                    .padding(horizontal = 24.dp),
                placeholder = "Search endpoints",
            )
            Spacer(modifier = Modifier.size(8.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 24.dp),
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
private fun TraceItem(call: TraceEntry, onClick: () -> Unit) {
    val containerColor = if (call.isSuccessful().not()) MaterialTheme.colorScheme.errorContainer
    else MaterialTheme.colorScheme.background

    Column(
        modifier = Modifier.fillMaxWidth()
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "${call.duration}ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )

                val statusColor = when {
                    call.isSuccessful() -> MaterialTheme.colorScheme.onSurface // Design shows Black for 200 GET, Emerald for 201 etc. using Black for simplicity or custom logic
                    call.isViewed -> MaterialTheme.colorScheme.onSurface
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
            modifier = Modifier.padding(end = 32.dp),
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
    // Design: POST/PUT/PATCH -> Black bg + White text. GET -> Border + Black text.
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
            width = 1.dp,
            color = contentColor,
        ).takeIf { !isWrite },
    )
}

