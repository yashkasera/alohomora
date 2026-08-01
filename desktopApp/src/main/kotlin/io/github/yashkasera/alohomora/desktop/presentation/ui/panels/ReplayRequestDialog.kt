package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.replay.ReplayHeaderText
import io.github.yashkasera.alohomora.replay.ReplayRequest
import io.github.yashkasera.alohomora.ui.theme.dimens

/**
 * Lets the user edit a captured request before the device re-sends it.
 *
 * Everything is editable because the app re-signs the payload on the way out: changing a body here
 * and getting a valid signature back is the reason the feature exists. What is *not* shown is any
 * header the sanitiser removed — redacted secrets and values the client recomputes — since offering
 * `Authorization: [REDACTED]` for editing would invite the user to fix a header the app is about to
 * set correctly anyway.
 */
@Composable
fun ReplayRequestDialog(
    initial: ReplayRequest,
    inFlight: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSend: (ReplayRequest) -> Unit,
) {
    // Keyed on the source trace so reopening the dialog for a different trace starts from that
    // trace rather than the previous one's edits.
    var method by remember(initial.sourceTraceId) { mutableStateOf(initial.method) }
    var url by remember(initial.sourceTraceId) { mutableStateOf(initial.url) }
    var headers by remember(initial.sourceTraceId) {
        mutableStateOf(ReplayHeaderText.render(initial.headers))
    }
    var body by remember(initial.sourceTraceId) { mutableStateOf(initial.body.orEmpty()) }

    AlertDialog(
        onDismissRequest = { if (!inFlight) onDismiss() },
        title = { Text("Replay request") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
            ) {
                Text(
                    text = "Sent by the app on the device, through its own HTTP client, so " +
                        "signatures and auth headers are regenerated for the payload below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md)) {
                    OutlinedTextField(
                        value = method,
                        onValueChange = { method = it.uppercase() },
                        label = { Text("Method") },
                        singleLine = true,
                        enabled = !inFlight,
                        modifier = Modifier.width(120.dp),
                    )
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("URL") },
                        singleLine = true,
                        enabled = !inFlight,
                        modifier = Modifier.weight(1f),
                    )
                }

                OutlinedTextField(
                    value = headers,
                    onValueChange = { headers = it },
                    label = { Text("Headers") },
                    placeholder = { Text("Accept: application/json") },
                    enabled = !inFlight,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp, max = 160.dp),
                )

                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Body") },
                    enabled = !inFlight,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 260.dp),
                )

                if (!error.isNullOrBlank()) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSend(
                        initial.copy(
                            method = method.trim().uppercase(),
                            url = url.trim(),
                            headers = ReplayHeaderText.parse(headers),
                            // Blank means bodyless, not an empty string body: the captured request
                            // for a GET has no body and must not gain one by passing through here.
                            body = body.takeIf { it.isNotBlank() },
                        ),
                    )
                },
                enabled = !inFlight && url.isNotBlank() && method.isNotBlank(),
            ) {
                if (inFlight) {
                    CircularProgressIndicator(modifier = Modifier.width(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Send")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !inFlight) {
                Text("Cancel")
            }
        },
    )
}
