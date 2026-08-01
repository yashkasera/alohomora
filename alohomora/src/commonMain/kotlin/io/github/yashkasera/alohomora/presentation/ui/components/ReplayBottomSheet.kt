package io.github.yashkasera.alohomora.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.replay.ReplayHeaderText
import io.github.yashkasera.alohomora.replay.ReplayRequest
import io.github.yashkasera.alohomora.ui.components.AlohomoraBottomSheetModal
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilledButton
import io.github.yashkasera.alohomora.ui.theme.dimens

/**
 * Lets the user edit a captured request before the app re-sends it.
 *
 * Everything is editable because the request goes back through the app's own client, which
 * regenerates whatever it derives per-request — a payload signature above all. Editing a body here
 * and having it arrive correctly signed is the reason the sheet exists.
 *
 * Headers the capture redacted, and headers the client recomputes, are absent by the time this is
 * called: offering `Authorization: [REDACTED]` for editing would invite the user to hand-fix a
 * header the app is about to set correctly anyway.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReplayBottomSheet(
    initial: ReplayRequest,
    isReplaying: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSend: (ReplayRequest) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var method by remember(initial.sourceTraceId) { mutableStateOf(initial.method) }
    var url by remember(initial.sourceTraceId) { mutableStateOf(initial.url) }
    var headers by remember(initial.sourceTraceId) {
        mutableStateOf(ReplayHeaderText.render(initial.headers))
    }
    var body by remember(initial.sourceTraceId) { mutableStateOf(initial.body.orEmpty()) }

    AlohomoraBottomSheetModal(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
        ) {
            Text(text = "Replay request", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Sent through this app's own HTTP client, so signatures and auth headers " +
                    "are regenerated for the payload below.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm)) {
                OutlinedTextField(
                    value = method,
                    onValueChange = { method = it.uppercase() },
                    label = { Text("Method") },
                    singleLine = true,
                    enabled = !isReplaying,
                    modifier = Modifier.width(110.dp),
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    enabled = !isReplaying,
                    modifier = Modifier.weight(1f),
                )
            }

            OutlinedTextField(
                value = headers,
                onValueChange = { headers = it },
                label = { Text("Headers") },
                placeholder = { Text("Accept: application/json") },
                enabled = !isReplaying,
                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp, max = 140.dp),
            )

            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("Body") },
                enabled = !isReplaying,
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 220.dp),
            )

            if (!error.isNullOrBlank()) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            AlohomoraFilledButton(
                text = if (isReplaying) "Sending…" else "Send",
                onClick = {
                    onSend(
                        initial.copy(
                            method = method.trim().uppercase(),
                            url = url.trim(),
                            headers = ReplayHeaderText.parse(headers),
                            // Blank means bodyless, not an empty-string body: a captured GET has no
                            // body and must not acquire one by passing through this sheet.
                            body = body.takeIf { it.isNotBlank() },
                        ),
                    )
                },
                enabled = !isReplaying && url.isNotBlank() && method.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = if (!isReplaying) {
                    null
                } else {
                    { CircularProgressIndicator(modifier = Modifier.width(16.dp), strokeWidth = 2.dp) }
                },
            )
        }
    }
}
