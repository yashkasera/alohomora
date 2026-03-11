package io.github.yashkasera.alohomora.presentation.ui.screens.incident.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.yashkasera.alohomora.ui.theme.CanvasAlertRed
import io.github.yashkasera.alohomora.ui.theme.CanvasBlack
import io.github.yashkasera.alohomora.ui.theme.CanvasDarkGray
import io.github.yashkasera.alohomora.ui.theme.CanvasLightGray
import io.github.yashkasera.alohomora.ui.theme.CanvasWhite
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilledButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraOutlinedButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.presentation.ui.components.icons.Copy
import io.github.yashkasera.alohomora.presentation.ui.components.icons.Icons
import io.github.yashkasera.alohomora.presentation.ui.components.icons.ArrowLeft
import io.github.yashkasera.alohomora.presentation.ui.components.icons.Share
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun IncidentDetailsScreen(
    incidentId: Long,
    onBackClick: () -> Unit,
) {
    val viewModel = koinViewModel<IncidentDetailsViewModel> { parametersOf(incidentId) }
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            AlohomoraTopBar(
                title = "BACK",
                subtitle = "REPORT #${incidentId}",
                navigationIcon = {
                    AlohomoraIconButton(onClick = onBackClick) {
                        Icon(Icons.ArrowLeft, contentDescription = "back")
                    }
                },
            )
        },
        containerColor = CanvasWhite,
        contentColor = CanvasBlack,
    ) { padding ->
        if (state.isLoading || state.incident == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = CanvasBlack)
            }
        } else {
            val incident = state.incident!!

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
            ) {
                // Header Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CanvasWhite)
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                ) {
                    // Fatal Exception Badge
                    Box(
                        modifier = Modifier
                            .background(CanvasAlertRed.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = "FATAL EXCEPTION",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                            ),
                            color = CanvasAlertRed,
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Exception Name
                    Text(
                        text = incident.reason?.substringAfterLast(".")?.substringBefore(":")
                            ?: "Unknown Exception",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Normal,
                            fontSize = 40.sp,
                            fontStyle = FontStyle.Italic,
                        ),
                        color = CanvasBlack,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Exception Message
                    Text(
                        text = incident.reason ?: "No additional information available",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            fontStyle = FontStyle.Italic,
                            lineHeight = 20.sp,
                        ),
                        color = CanvasDarkGray.copy(alpha = 0.8f),
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Metadata Grid
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            MetadataItem(
                                label = "DEVICE",
                                value = "Unknown",
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            MetadataItem(
                                label = "ANDROID VERSION",
                                value = "API 34\n(UpsideDownCake)",
                                modifier = Modifier.weight(1f),
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            MetadataItem(
                                label = "APP VERSION",
                                value = "Unknown",
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            MetadataItem(
                                label = "TIMESTAMP",
                                value = formatDetailTimestamp(incident.time * 1000),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                // Stack Trace Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CanvasWhite)
                        .padding(horizontal = 20.dp),
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "FULL STACKTRACE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                            ),
                            color = CanvasDarkGray.copy(alpha = 0.6f),
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "${incident.stackTrace?.lines()?.size ?: 0} lines",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                            ),
                            color = CanvasDarkGray.copy(alpha = 0.5f),
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stack Trace Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CanvasLightGray.copy(alpha = 0.3f))
                            .border(1.dp, CanvasLightGray)
                            .padding(16.dp),
                    ) {
                        Text(
                            text = incident.stackTrace ?: "No stack trace available",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            ),
                            color = CanvasBlack.copy(alpha = 0.8f),
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        AlohomoraOutlinedButton(
                            text = "Copy Trace",
                            onClick = { /* TODO: Copy trace to clipboard */ },
                            modifier = Modifier.weight(1f),
                            shape = RectangleShape,
                            contentColor = CanvasBlack,
                            borderColor = CanvasBlack,
                        ) {
                            Icon(
                                Icons.Copy,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                            Text(
                                "COPY TRACE",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                ),
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        AlohomoraFilledButton(
                            text = "Share Report",
                            onClick = { /* TODO: Share report */ },
                            modifier = Modifier.weight(1f),
                            shape = RectangleShape,
                            containerColor = CanvasBlack,
                            contentColor = CanvasWhite,
                        ) {
                            Icon(
                                Icons.Share,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                            Text(
                                "SHARE REPORT",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                ),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
private fun MetadataItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            ),
            color = CanvasDarkGray.copy(alpha = 0.5f),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 18.sp,
                fontStyle = FontStyle.Italic,
                lineHeight = 22.sp,
            ),
            color = CanvasBlack,
        )
    }
}

private fun formatDetailTimestamp(timestamp: Long): String {
    return try {
        val instant = Instant.fromEpochMilliseconds(timestamp)
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val month = local.month.name.substring(0, 3).lowercase()
            .replaceFirstChar { it.uppercase() }
        "${month} ${local.dayOfMonth}, ${local.hour.toString().padStart(2, '0')}:${
            local.minute.toString().padStart(2, '0')
        }:${local.second.toString().padStart(2, '0')}"
    } catch (e: Exception) {
        "Unknown"
    }
}
