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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.yashkasera.alohomora.common.TelemetryEvent
import io.github.yashkasera.alohomora.ui.components.AlohomoraExtendedFloatingActionButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.presentation.ui.components.EmptyState
import io.github.yashkasera.alohomora.presentation.ui.components.icons.Icons
import io.github.yashkasera.alohomora.presentation.ui.components.icons.ArrowLeft
import io.github.yashkasera.alohomora.presentation.ui.components.icons.chartLine
import io.github.yashkasera.alohomora.ui.theme.CanvasBlack
import io.github.yashkasera.alohomora.ui.theme.CanvasDarkGray
import io.github.yashkasera.alohomora.ui.theme.CanvasLightGray
import io.github.yashkasera.alohomora.ui.theme.CanvasWhite
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun TelemetryScreen(onBackClick: () -> Unit) {
    val viewModel = koinViewModel<TelemetryViewModel>()
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = { AlohomoraTopBar(
            title = "Telemetry",
            subtitle = "",
            navigationIcon = {
                AlohomoraIconButton(onClick = onBackClick) {
                    Icon(Icons.ArrowLeft, contentDescription = "back")
                }
            },
        ) },
//        floatingActionButton = { CreateJourneyFab() },
        containerColor = CanvasWhite,
        contentColor = CanvasBlack,
    ) { padding ->
        TelemetryList(
            events = state.events,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
fun TelemetryList(events: List<TelemetryEvent>, modifier: Modifier = Modifier) {
    if (events.isEmpty()) {
        EmptyState(
            icon = Icons.chartLine,
            title = "No Telemetry Yet",
            subtitle = "Telemetry events will appear here in real-time",
            modifier = modifier
        )
    } else {
        LazyColumn(modifier = modifier.fillMaxSize()) {
            items(events) { event ->
                TelemetryItem(event = event)
            }
        }
    }
}

@Composable
fun TelemetryItem(event: TelemetryEvent) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 0.5.dp, color = CanvasLightGray) // Bottom border for items
            .clickable { }
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        // Header Row: Icon + Title + Timestamp
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically, // Baseline alignment trickier in Row
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
//                Icon(
//                    imageVector = getEventIcon(event.name),
//                    contentDescription = null,
//                    modifier = Modifier.size(20.dp),
//                    tint = if (event.name == "App.Exception") CanvasAlertRed else CanvasBlack
//                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
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

        Spacer(modifier = Modifier.height(8.dp))

        // Code Block
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
    AlohomoraHorizontalDivider(color = CanvasLightGray, thickness = 1.dp)
}

@Composable
fun CreateJourneyFab() {
    AlohomoraExtendedFloatingActionButton(
        onClick = { /* TODO */ },
        containerColor = CanvasBlack,
        contentColor = CanvasWhite,
        modifier = Modifier.padding(bottom = 60.dp), // Lift above bottom bar specific styling if needed
        shape = RectangleShape, // HTML button looks rectangular with shadow
    ) {
//        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Create Journey",
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                letterSpacing = 0.5.sp,
            ),
        )
    }
}

//fun getEventIcon(eventName: String): ImageVector {
//    return when (eventName) {
//        "App.Launch" -> Icons.Default.RocketLaunch
//        "Screen.View" -> Icons.Default.Visibility
//        "Button.Click" -> Icons.Default.TouchApp
//        "API.Sync" -> Icons.Default.CloudSync
//        "App.Exception" -> Icons.Default.Error
//        else -> Icons.Default.Info
//    }
//}

fun formatTimestamp(timestamp: Long): String {
    // Simple mock formatter for now, assuming timestamp is epoch millis
    // Ideally use explicit date formatter
    try {
        val instant = Instant.fromEpochMilliseconds(timestamp)
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${local.hour.toString().padStart(2, '0')}:${
            local.minute.toString().padStart(2, '0')
        }:${local.second.toString().padStart(2, '0')}"
    } catch (e: Exception) {
        return "00:00:00"
    }
}
