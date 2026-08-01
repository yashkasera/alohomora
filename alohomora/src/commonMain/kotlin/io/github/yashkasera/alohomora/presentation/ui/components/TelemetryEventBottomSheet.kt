package io.github.yashkasera.alohomora.presentation.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.common.DateUtils
import io.github.yashkasera.alohomora.common.TelemetryEvent
import io.github.yashkasera.alohomora.ui.components.AlohomoraBottomSheetModal
import io.github.yashkasera.alohomora.ui.components.AlohomoraCodeBlock
import io.github.yashkasera.alohomora.ui.icons.Copy
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Slack

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
        skipPartiallyExpanded = true,
    )
    val clipboardManager = LocalClipboardManager.current

    val formattedJson = remember(event.properties) {
        formatJson(event.properties)
    }

    val shareText = remember(event) {
        buildString {
            appendLine("Event: ${event.name}")
            appendLine(
                "Time: ${
                    DateUtils.format(
                        event.time,
                        DateUtils.Format.ISO_DATE_TIME_SECONDS,
                    )
                }",
            )
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
                        text = DateUtils.format(
                            event.time,
                            DateUtils.Format.READABLE_DATE_TIME,
                        ),
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
            AlohomoraCodeBlock(
                content = formattedJson,
                isScrollable = true,
                jsonPrettify = true,
            )
        }
    }
}

/**
 * Formats a JSON element as a pretty-printed JSON string.
 */
private fun formatJson(jsonElement: kotlinx.serialization.json.JsonElement?): String {
    if (jsonElement == null) return "{}"
    return try {
        Alohomora.json.encodeToString(
            kotlinx.serialization.json.JsonElement.serializer(),
            jsonElement,
        )
    } catch (e: Exception) {
        jsonElement.toString()
    }
}
