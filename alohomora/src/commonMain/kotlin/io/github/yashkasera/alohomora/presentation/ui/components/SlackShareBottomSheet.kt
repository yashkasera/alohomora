package io.github.yashkasera.alohomora.presentation.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.ui.components.AlohomoraBottomSheetModal
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextField

/**
 * Configuration for a share option in the Slack share sheet.
 *
 * @property icon The icon to display for this option
 * @property title The title text for this share option
 * @property subtitle The subtitle/description text
 * @property onShare Callback when this option is selected with the recipient email
 */
data class SlackShareOption(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val onShare: (email: String) -> Unit,
)

/**
 * A generic bottom sheet for sharing content to Slack.
 *
 * @param title The title to display at the top of the sheet
 * @param isConfigured Whether Slack webhook is configured
 * @param onDismiss Callback when the sheet is dismissed
 * @param shareOptions List of share options to display
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SlackShareBottomSheet(
    title: String = "Share to Slack",
    isConfigured: Boolean,
    onDismiss: () -> Unit,
    shareOptions: List<SlackShareOption>,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    var email by remember { mutableStateOf("") }

    AlohomoraBottomSheetModal(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            if (isConfigured) {
                // Email input
                Text(
                    text = "Recipient Email",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                AlohomoraTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "abc.xyz@example.org",
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "This will share in the DM with the specified user",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp),
                )

                // Share options
                shareOptions.forEachIndexed { index, option ->
                    ShareOption(
                        icon = option.icon,
                        title = option.title,
                        subtitle = option.subtitle,
                        onClick = {
                            if (email.isNotBlank()) {
                                option.onShare(email)
                                onDismiss()
                            }
                        },
                        enabled = email.isNotBlank(),
                    )
                    if (index < shareOptions.size - 1) {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            } else {
                // Not configured state
                Text(
                    text = "Slack is not configured",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 32.dp),
                )

                Text(
                    text = "Add slackWebhookUrl to your alohomora configuration in build.gradle.kts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
private fun ShareOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            },
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
            )
        }
    }
}
