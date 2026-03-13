package io.github.yashkasera.alohomora.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.common.TelemetryEvent
import io.github.yashkasera.alohomora.ui.components.AlohomoraBottomSheetModal
import io.github.yashkasera.alohomora.ui.icons.Copy
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Share
import io.github.yashkasera.alohomora.ui.icons.Slack
import io.github.yashkasera.alohomora.ui.theme.CanvasDarkGray
import io.github.yashkasera.alohomora.ui.theme.CanvasLightGray
import io.github.yashkasera.alohomora.ui.theme.CanvasWhite
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json

/**
 * Bottom sheet that displays a telemetry event with formatted JSON properties
 * and options to share via Slack or copy to clipboard.
 *
 * @param event The telemetry event to display
 * @param isSlackConfigured Whether Slack webhook is configured
 * @param onDismiss Callback when the sheet is dismissed
 * @param onShareToSlack Callback to share the event to Slack with the recipient email
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TelemetryEventBottomSheet(
    event: TelemetryEvent,
    isSlackConfigured: Boolean,
    onDismiss: () -> Unit,
    onShareToSlack: (email: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val clipboardManager = LocalClipboardManager.current

    val formattedJson = remember(event.properties) {
        formatJson(event.properties)
    }

    val shareText = remember(event) {
        buildString {
            appendLine("Event: ${event.name}")
            appendLine("Time: ${formatTimestampFull(event.time)}")
            appendLine()
            appendLine("Properties:")
            appendLine(formattedJson)
        }
    }

    AlohomoraBottomSheetModal(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header with event name and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.name,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = formatTimestampFull(event.time),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Copy to clipboard button
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(shareText))
                    },
                ) {
                    Icon(
                        imageVector = Icons.Copy,
                        contentDescription = "Copy to clipboard",
                    )
                }

                // Share to Slack button (only if configured)
                if (isSlackConfigured) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { onShareToSlack("") }, // Will open the slack sheet
                    ) {
                        Icon(
                            imageVector = Icons.Slack,
                            contentDescription = "Share to Slack",
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Properties section
            Text(
                text = "Properties",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            // JSON code block
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CanvasLightGray.copy(alpha = 0.5f))
                    .padding(1.dp)
                    .background(CanvasWhite),
            ) {
                // Left accent border for exceptions
                if (event.name == "App.Exception") {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(3.dp)
                            .height(24.dp)
                            .background(MaterialTheme.colorScheme.error),
                    )
                }

                Text(
                    text = formattedJson,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = CanvasDarkGray,
                        lineHeight = 18.sp,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .padding(start = if (event.name == "App.Exception") 16.dp else 12.dp)
                        .verticalScroll(rememberScrollState()),
                )
            }

            // Slack sharing option (if configured)
            if (isSlackConfigured) {
                SlackShareBottomSheetContent(
                    onShare = onShareToSlack,
                    content = shareText,
                )
            }
        }
    }
}

@Composable
private fun SlackShareBottomSheetContent(
    onShare: (email: String) -> Unit,
    content: String,
) {
    var showSlackSheet by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Share,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Share this event",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = "Send to Slack via DM",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            )
        }

        IconButton(
            onClick = { showSlackSheet = true },
        ) {
            Icon(
                imageVector = Icons.Slack,
                contentDescription = "Share to Slack",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }

    if (showSlackSheet) {
        SlackShareBottomSheet(
            title = "Share Event to Slack",
            isConfigured = true,
            onDismiss = { showSlackSheet = false },
            shareOptions = listOf(
                SlackShareOption(
                    icon = Icons.Share,
                    title = "Share Event Details",
                    subtitle = "Send formatted event with properties",
                    onShare = onShare,
                ),
            ),
        )
    }
}

/**
 * Formats a JSON element as a pretty-printed JSON string.
 */
private fun formatJson(jsonElement: kotlinx.serialization.json.JsonElement?): String {
    if (jsonElement == null) return "{}"
    return try {
        Alohomora.json.encodeToString(kotlinx.serialization.json.JsonElement.serializer(), jsonElement)
    } catch (e: Exception) {
        jsonElement.toString()
    }
}

/**
 * Formats a timestamp as a full date-time string.
 */
private fun formatTimestampFull(timestamp: Long): String {
    return try {
        val instant = Instant.fromEpochSeconds(timestamp)
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        "${local.date} ${local.hour.toString().padStart(2, '0')}:${
            local.minute.toString().padStart(2, '0')
        }:${local.second.toString().padStart(2, '0')}"
    } catch (e: Exception) {
        "Unknown"
    }
}
