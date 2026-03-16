package io.github.yashkasera.alohomora.presentation.ui.screens.telemetry

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.common.DateUtils
import io.github.yashkasera.alohomora.common.TelemetryEvent
import io.github.yashkasera.alohomora.presentation.ui.components.EmptyState
import io.github.yashkasera.alohomora.presentation.ui.components.TelemetryEventBottomSheet
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraSearchTextField
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.ui.components.ConfirmationBottomSheet
import io.github.yashkasera.alohomora.ui.components.ConfirmationConfig
import io.github.yashkasera.alohomora.ui.icons.ArrowLeft
import io.github.yashkasera.alohomora.ui.icons.Eye
import io.github.yashkasera.alohomora.ui.icons.EyeOff
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Trash
import io.github.yashkasera.alohomora.ui.icons.ChartLine
import io.github.yashkasera.alohomora.ui.theme.CanvasBlack
import io.github.yashkasera.alohomora.ui.theme.CanvasDarkGray
import io.github.yashkasera.alohomora.ui.theme.CanvasLightGray
import io.github.yashkasera.alohomora.ui.theme.CanvasWhite
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TelemetryScreen(onBackClick: () -> Unit) {
    val viewModel = koinViewModel<TelemetryViewModel>()
    val state by viewModel.state.collectAsState()
    val isSlackConfigured = remember { Alohomora.config?.slackWebhookUrl.isNullOrBlank().not() }

    Scaffold(
        topBar = {
            AlohomoraTopBar(
                title = "Telemetry",
                navigationIcon = {
                    AlohomoraIconButton(onClick = onBackClick) {
                        Icon(Icons.ArrowLeft, contentDescription = "back")
                    }
                },
                actions = {
                    // Property visibility toggle
                    AlohomoraIconButton(onClick = viewModel::toggleShowProperties) {
                        Icon(
                            imageVector = if (state.showProperties) Icons.Eye else Icons.EyeOff,
                            contentDescription = if (state.showProperties) "Hide properties" else "Show properties",
                        )
                    }
                    // Clear all button (only show if there are events)
                    if (state.events.isNotEmpty()) {
                        AlohomoraIconButton(onClick = viewModel::showClearConfirmation) {
                            Icon(
                                imageVector = Icons.Trash,
                                contentDescription = "Clear all events",
                            )
                        }
                    }
                },
            )
        },
        containerColor = CanvasWhite,
        contentColor = CanvasBlack,
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Search field
            TelemetrySearchBar(
                query = state.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
            )

            // Telemetry list
            TelemetryList(
                events = state.events,
                showProperties = state.showProperties,
                onEventClick = viewModel::onEventClick,
            )
        }

        // Event detail bottom sheet
        state.selectedEvent?.let { event ->
            TelemetryEventBottomSheet(
                event = event,
                isSlackConfigured = isSlackConfigured,
                onDismiss = viewModel::dismissEventDetail,
                onShareToSlack = { email ->
                    viewModel.hideSlackSheet()
                    // Share via Slack would be implemented here
                },
            )
        }

        // Clear all confirmation bottom sheet
        if (state.showClearConfirmation) {
            ConfirmationBottomSheet(
                config = ConfirmationConfig(
                    title = "Clear All Events",
                    message = "Are you sure you want to delete all telemetry events? This action cannot be undone.",
                    confirmButtonText = "Clear All",
                    dismissButtonText = "Cancel",
                    isDestructive = true,
                ),
                onConfirm = viewModel::clearAllEvents,
                onDismiss = viewModel::hideClearConfirmation,
            )
        }
    }
}

@Composable
private fun TelemetrySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    AlohomoraSearchTextField(
        query = query,
        onQueryChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        placeholder = "Search events by name",
    )
}

@Composable
fun TelemetryList(
    events: List<TelemetryEvent>,
    showProperties: Boolean,
    onEventClick: (TelemetryEvent) -> Unit,
) {
    if (events.isEmpty()) {
        EmptyState(
            icon = Icons.ChartLine,
            title = "No Telemetry Yet",
            subtitle = "Telemetry events will appear here in real-time",
        )
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(events) { event ->
                TelemetryItem(
                    event = event,
                    showProperties = showProperties,
                    onClick = { onEventClick(event) },
                )
            }
        }
    }
}

@Composable
fun TelemetryItem(
    event: TelemetryEvent,
    showProperties: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        // Header Row: Title + Timestamp
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                    ),
                    color = CanvasBlack,
                )
            }

            Text(
                text = DateUtils.format(event.time, DateUtils.Format.HH_MM_SS),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }

        // Code Block - only shown if showProperties is true
        if (showProperties) {
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp)
                    .background(CanvasLightGray.copy(alpha = 0.5f))
                    .border(1.dp, CanvasLightGray.copy(alpha = 0.8f)),
            ) {
                // Exceptions get a special left accent border
                if (event.name == "App.Exception") {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(CanvasBlack),
                    )
                }

                Text(
                    text = event.properties?.toString() ?: "{}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = CanvasDarkGray,
                    ),
                    modifier = Modifier
                        .padding(8.dp)
                        .padding(start = if (event.name == "App.Exception") 8.dp else 0.dp),
                )
            }
        }
    }
    AlohomoraHorizontalDivider(color = CanvasLightGray, thickness = 1.dp)
}

