package io.github.yashkasera.alohomora.presentation.ui.screens.telemetry

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.common.TelemetryEvent
import io.github.yashkasera.alohomora.presentation.ui.components.EmptyState
import io.github.yashkasera.alohomora.presentation.ui.components.TelemetryEventBottomSheet
import io.github.yashkasera.alohomora.ui.components.ConfirmationBottomSheet
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraOutlinedTextField
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextFieldDefaults
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.ui.components.ConfirmationConfig
import io.github.yashkasera.alohomora.ui.icons.ArrowLeft
import io.github.yashkasera.alohomora.ui.icons.Eye
import io.github.yashkasera.alohomora.ui.icons.EyeOff
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Search
import io.github.yashkasera.alohomora.ui.icons.Trash
import io.github.yashkasera.alohomora.ui.icons.chartLine
import io.github.yashkasera.alohomora.ui.theme.CanvasBlack
import io.github.yashkasera.alohomora.ui.theme.CanvasDarkGray
import io.github.yashkasera.alohomora.ui.theme.CanvasLightGray
import io.github.yashkasera.alohomora.ui.theme.CanvasWhite
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        AlohomoraOutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            shape = RectangleShape,
            colors = AlohomoraTextFieldDefaults.outlinedColors(
                containerColor = MaterialTheme.colorScheme.surface,
                placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                cursorColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            placeholder = {
                Text("Search events by name")
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Search,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun TelemetryList(
    events: List<TelemetryEvent>,
    showProperties: Boolean,
    onEventClick: (TelemetryEvent) -> Unit,
) {
    if (events.isEmpty()) {
        EmptyState(
            icon = Icons.chartLine,
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
                text = formatTimestamp(event.time),
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

fun formatTimestamp(timestamp: Long): String {
    return try {
        val instant = Instant.fromEpochMilliseconds(timestamp)
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        "${local.hour.toString().padStart(2, '0')}:${
            local.minute.toString().padStart(2, '0')
        }:${local.second.toString().padStart(2, '0')}"
    } catch (e: Exception) {
        "00:00:00"
    }
}
