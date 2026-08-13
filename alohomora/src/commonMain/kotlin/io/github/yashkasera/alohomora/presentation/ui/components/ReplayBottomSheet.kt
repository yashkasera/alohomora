package io.github.yashkasera.alohomora.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextField
import io.github.yashkasera.alohomora.ui.components.jsoneditor.JsonEditor
import io.github.yashkasera.alohomora.ui.components.jsoneditor.JsonEditorState
import io.github.yashkasera.alohomora.ui.theme.dimens

/** Whether the body is edited as structured JSON (validated, formattable) or as raw text. */
private enum class BodyMode(val label: String) {
    JSON("JSON"),
    TEXT("Text"),
}

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
 *
 * The body offers the same JSON/Text switch as the desktop console (`ReplayRequestDialog`): JSON
 * mode brings validation and formatting, Text mode is the multiline fallback for anything that is
 * not JSON. The initial mode is inferred from the captured `Content-Type`.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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

    // Two body buffers kept in sync on switch, mirroring the desktop dialog: the JSON editor owns
    // its own TextFieldValue, so Text mode has to hand its string across rather than share state.
    val initialBodyMode = remember(initial.sourceTraceId) {
        if (initial.contentType.orEmpty().contains("json", ignoreCase = true)) BodyMode.JSON
        else BodyMode.TEXT
    }
    var bodyMode by remember(initial.sourceTraceId) { mutableStateOf(initialBodyMode) }
    val jsonBodyState = remember(initial.sourceTraceId) { JsonEditorState(initial.body.orEmpty()) }
    var textBody by remember(initial.sourceTraceId) { mutableStateOf(initial.body.orEmpty()) }

    AlohomoraBottomSheetModal(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
        ) {
            Text(
                text = "Replay request",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Sent through this app's own HTTP client, so signatures and auth headers " +
                    "are regenerated for the payload below.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            AlohomoraTextField(
                value = method,
                onValueChange = { method = it.uppercase() },
                label = "Method",
                singleLine = true,
                enabled = !isReplaying,
                modifier = Modifier.width(110.dp),
            )

            AlohomoraTextField(
                value = url,
                onValueChange = { url = it },
                label = "URL",
                enabled = !isReplaying,
            )

            AlohomoraTextField(
                value = headers,
                onValueChange = { headers = it },
                label = "Headers",
                placeholder = "Accept: application/json",
                singleLine = false,
                enabled = !isReplaying,
                modifier = Modifier.fillMaxWidth(),
            )

            // Body, with a JSON/Text switch matching the desktop console.
            Text(
                text = "Body",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                BodyMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = bodyMode == mode,
                        enabled = !isReplaying,
                        onClick = {
                            if (bodyMode != mode) {
                                if (mode == BodyMode.JSON) jsonBodyState.setText(textBody)
                                else textBody = jsonBodyState.text
                                bodyMode = mode
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = BodyMode.entries.size,
                            baseShape = MaterialTheme.shapes.small,
                        ),
                    ) {
                        Text(mode.label, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            when (bodyMode) {
                // Fixed height: JsonEditor sizes its text area with weight(1f), which needs a
                // bounded parent. The sheet's Column scrolls (unbounded height), so without an
                // explicit height the editor collapses. A fixed box lets it scroll internally.
                BodyMode.JSON -> JsonEditor(
                    state = jsonBodyState,
                    readOnly = isReplaying,
                    minLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                )

                BodyMode.TEXT -> AlohomoraTextField(
                    value = textBody,
                    onValueChange = { textBody = it },
                    placeholder = "Request body",
                    singleLine = false,
                    enabled = !isReplaying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                )
            }

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
                    val body = when (bodyMode) {
                        BodyMode.JSON -> jsonBodyState.text
                        BodyMode.TEXT -> textBody
                    }
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
                    {
                        CircularProgressIndicator(
                            modifier = Modifier.width(16.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                },
            )
        }
    }
}
