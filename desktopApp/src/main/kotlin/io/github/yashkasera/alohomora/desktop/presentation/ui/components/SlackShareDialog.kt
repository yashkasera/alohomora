package io.github.yashkasera.alohomora.desktop.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilledButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.yashkasera.alohomora.ui.icons.Copy
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.theme.dimens

/**
 * Collects a recipient and posts the open record to Slack.
 *
 * Lifted out of `TrafficPanel` once the Events sheet needed it too. [onShareCurl] is nullable because
 * only a traffic entry has a cURL form — an event has one representation, so its dialog shows one
 * button rather than a disabled second one.
 */
@Composable
fun SlackShareDialog(
    isConfigured: Boolean,
    currentWebhookUrl: String?,
    shareError: String?,
    onDismiss: () -> Unit,
    onShareText: (String) -> Unit,
    onClearError: () -> Unit,
    onShareCurl: ((String) -> Unit)? = null,
    shareTextLabel: String = "Share Text to Slack",
) {
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share to Slack") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md)) {
                if (isConfigured) {
                    AlohomoraTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            if (shareError != null) onClearError()
                        },
                        label = "Recipient Email",
                        placeholder = "abc.xyz@example.org",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "This will share in the DM with the specified user",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (onShareCurl != null) {
                        AlohomoraFilledButton(
                            text = "Share cURL to Slack",
                            onClick = { onShareCurl(email) },
                            enabled = email.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            uppercase = false,
                        )
                    }
                    AlohomoraFilledButton(
                        text = shareTextLabel,
                        onClick = { onShareText(email) },
                        enabled = email.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        uppercase = false,
                        leadingIcon = {
                            Icon(imageVector = Icons.Copy, contentDescription = null)
                        },
                    )
                    if (!shareError.isNullOrBlank()) {
                        Text(
                            text = shareError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                } else {
                    Text(
                        text = "Slack is not configured.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Configure slackWebhookUrl in your mobile Alohomora build config.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    // Deliberately reports only whether a URL arrived, never the URL itself:
                    // a Slack webhook is a live posting credential and this dialog ends up in
                    // screenshots and screen shares.
                    Text(
                        text = if (currentWebhookUrl.isNullOrBlank()) {
                            "The connected app did not send a webhook URL."
                        } else {
                            "A webhook URL was received but appears unusable."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            AlohomoraTextButton(
                text = "Close",
                onClick = onDismiss,
            )
        },
    )
}
