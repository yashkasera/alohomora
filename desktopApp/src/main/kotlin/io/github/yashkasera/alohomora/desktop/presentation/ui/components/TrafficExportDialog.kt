package io.github.yashkasera.alohomora.desktop.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.yashkasera.alohomora.desktop.data.local.TrafficExportFormat
import io.github.yashkasera.alohomora.ui.components.AlohomoraAlertDialog
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilledButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraRadioButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextButton
import io.github.yashkasera.alohomora.ui.theme.dimens

@Composable
fun TrafficExportDialog(
    entryCount: Int,
    onExport: (TrafficExportFormat) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf(TrafficExportFormat.JSON) }

    AlohomoraAlertDialog(
        onDismissRequest = onDismiss,
        title = "Export Traffic",
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
            ) {
                Text(
                    text = "Exporting $entryCount ${if (entryCount == 1) "request" else "requests"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                TrafficExportFormat.entries.forEach { format ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                    ) {
                        AlohomoraRadioButton(
                            selected = selected == format,
                            onClick = { selected = format },
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = format.label,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = formatDescription(format),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            AlohomoraFilledButton(
                text = "Save",
                onClick = { onExport(selected) },
            )
        },
        dismissButton = {
            AlohomoraTextButton(text = "Cancel", onClick = onDismiss)
        },
    )
}

private fun formatDescription(format: TrafficExportFormat): String = when (format) {
    TrafficExportFormat.JSON -> "Full request/response data with headers and bodies"
    TrafficExportFormat.HAR -> "HTTP Archive format for Postman, Charles, Chrome DevTools"
    TrafficExportFormat.CURL -> "Bash script with one curl command per request"
}
