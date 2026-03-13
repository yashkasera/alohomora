package io.github.yashkasera.alohomora.presentation.ui.screens.incident.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.yashkasera.alohomora.common.Incident
import io.github.yashkasera.alohomora.ui.theme.CanvasBlack
import io.github.yashkasera.alohomora.ui.theme.CanvasDarkGray
import io.github.yashkasera.alohomora.ui.theme.CanvasLightGray
import io.github.yashkasera.alohomora.ui.theme.CanvasWhite
import io.github.yashkasera.alohomora.ui.components.AlohomoraFloatingActionButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraOutlinedButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraSearchTextField
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.presentation.ui.components.EmptyState
import io.github.yashkasera.alohomora.ui.icons.AlertTriangle
import io.github.yashkasera.alohomora.ui.icons.Download
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.ArrowLeft
import io.github.yashkasera.alohomora.ui.icons.clock
import io.github.yashkasera.alohomora.ui.icons.hardDrive
import io.github.yashkasera.alohomora.ui.icons.Trash
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun IncidentScreen(
    onBackClick: () -> Unit,
    onNavigateToIncident: (incidentId: Long) -> Unit,
) {
    val viewModel = koinViewModel<IncidentViewModel>()
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            AlohomoraTopBar(
                title = "Incidents",
                navigationIcon = {
                    AlohomoraIconButton(onClick = onBackClick) {
                        Icon(Icons.ArrowLeft, contentDescription = "back")
                    }
                },
                actions = {
                    AlohomoraIconButton(onClick = { viewModel.clearAllIncidents() }) {
                        Icon(
                            Icons.Trash,
                            contentDescription = "Clear all incidents",
                            tint = CanvasBlack
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            AlohomoraFloatingActionButton(
                onClick = { /* TODO: Export incidents */ },
                containerColor = CanvasBlack,
                contentColor = CanvasWhite,
                shape = CircleShape,
            ) {
                Icon(Icons.Download, contentDescription = "Download incidents")
            }
        },
        containerColor = CanvasWhite,
        contentColor = CanvasBlack,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CanvasWhite)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Incident Logs",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Normal,
                        fontSize = 48.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    ),
                    color = CanvasBlack
                )
                Text(
                    text = "DIAGNOSTIC REPORT LIST",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 2.sp,
                        fontSize = 11.sp
                    ),
                    color = CanvasDarkGray.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Search Bar
                AlohomoraSearchTextField(
                    query = state.searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "Search exceptions or packages...",
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${state.incidents.size} TOTAL OCCURRENCES",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontSize = 11.sp
                        ),
                        color = CanvasDarkGray.copy(alpha = 0.6f)
                    )
                    AlohomoraOutlinedButton(
                        text = "Live Session",
                        onClick = { /* TODO: Filter live session */ },
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Text(
                            "LIVE SESSION",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }
            }

            // Incident List
            if (state.incidents.isEmpty()) {
                EmptyState(
                    icon = Icons.AlertTriangle,
                    title = "No Incidents Recorded",
                    subtitle = "Incident reports will appear here when exceptions occur",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.incidents) { incident ->
                        IncidentListItem(
                            incident = incident,
                            onClick = { onNavigateToIncident(incident.id) }
                        )
                    }

                    // Streaming indicator at bottom
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(CanvasBlack)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "STREAMING LIVE LOGS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        letterSpacing = 2.sp,
                                        fontSize = 10.sp
                                    ),
                                    color = CanvasDarkGray.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IncidentListItem(
    incident: Incident,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 0.5.dp, color = CanvasLightGray)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = incident.reason?.substringAfterLast(".")?.substringBefore(":") ?: "Unknown Exception",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = CanvasBlack
                    )
                    Box(
                        modifier = Modifier
                            .background(CanvasBlack, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "FATAL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = CanvasWhite
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = incident.place ?: "Unknown location",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp
                    ),
                    color = CanvasDarkGray.copy(alpha = 0.7f),
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.clock,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = CanvasDarkGray.copy(alpha = 0.6f)
                        )
                        Text(
                            text = formatTimestamp(incident.time * 1000), // Convert to millis
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp
                            ),
                            color = CanvasDarkGray.copy(alpha = 0.6f)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.hardDrive,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = CanvasDarkGray.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Device Info",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp
                            ),
                            color = CanvasDarkGray.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    return try {
        val instant = Instant.fromEpochMilliseconds(timestamp)
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        "${local.hour.toString().padStart(2, '0')}:${
            local.minute.toString().padStart(2, '0')
        }:${local.second.toString().padStart(2, '0')}.${
            (local.nanosecond / 1000000).toString().padStart(3, '0')
        }"
    } catch (e: Exception) {
        "00:00:00.000"
    }
}
