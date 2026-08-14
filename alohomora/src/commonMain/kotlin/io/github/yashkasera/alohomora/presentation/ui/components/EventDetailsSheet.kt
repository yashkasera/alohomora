package io.github.yashkasera.alohomora.presentation.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.common.DateUtils
import io.github.yashkasera.alohomora.common.Event
import io.github.yashkasera.alohomora.common.prettyProperties
import io.github.yashkasera.alohomora.ui.components.AlohomoraBottomSheetModal
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
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
internal fun EventsDetailsSheet(
    event: Event,
    isSlackConfigured: Boolean,
    onDismiss: () -> Unit,
    onShareToSlack: (email: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
    )
    val clipboardCopy = rememberClipboardCopy()

    val formattedJson = remember(event) { event.prettyProperties() }

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
        modifier = modifier,
        sheetState = sheetState,
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(clipboardCopy.snackbarHostState) },
        ) { _ ->
            Column(modifier = Modifier.fillMaxWidth()) {
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

                    AlohomoraIconButton(
                        onClick = {
                            clipboardCopy.copy(shareText)
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Copy,
                            contentDescription = "Copy to clipboard",
                        )
                    }

                    if (isSlackConfigured) {
                        AlohomoraIconButton(
                            onClick = { onShareToSlack("") },
                        ) {
                            Icon(
                                imageVector = Icons.Slack,
                                contentDescription = "Share to Slack",
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                AlohomoraHorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Properties",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp),
                )

                AlohomoraCodeBlock(
                    content = formattedJson,
                    isScrollable = true,
                )
            }
        }
    }
}
