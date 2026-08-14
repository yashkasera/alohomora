package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.AlohomoraSideSheet
import io.github.yashkasera.alohomora.replay.ReplayHeaderText
import io.github.yashkasera.alohomora.replay.ReplayRequest
import io.github.yashkasera.alohomora.ui.components.AlohomoraCircularProgressIndicator
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilledButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextField
import io.github.yashkasera.alohomora.ui.components.jsoneditor.JsonEditor
import io.github.yashkasera.alohomora.ui.components.jsoneditor.JsonEditorState
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.X
import io.github.yashkasera.alohomora.ui.theme.dimens

private enum class BodyMode(val label: String) {
    JSON("JSON"),
    TEXT("Text"),
}

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
fun ReplayRequestSideSheet(
    visible: Boolean,
    initial: ReplayRequest,
    inFlight: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSend: (ReplayRequest) -> Unit,
) {
    var method by remember(initial.sourceTraceId) { mutableStateOf(initial.method) }
    var url by remember(initial.sourceTraceId) { mutableStateOf(initial.url) }
    var headers by remember(initial.sourceTraceId) {
        mutableStateOf(ReplayHeaderText.render(initial.headers))
    }
    val initialBodyMode = remember(initial.sourceTraceId) {
        val ct = initial.contentType.orEmpty()
        if (ct.contains("json", ignoreCase = true)) BodyMode.JSON else BodyMode.TEXT
    }
    var bodyMode by remember(initial.sourceTraceId) { mutableStateOf(initialBodyMode) }
    val jsonBodyState = remember(initial.sourceTraceId) {
        JsonEditorState(initial.body.orEmpty())
    }
    var textBody by remember(initial.sourceTraceId) { mutableStateOf(initial.body.orEmpty()) }

    AlohomoraSideSheet(
        visible = visible,
        onDismiss = { if (!inFlight) onDismiss() },
        widthFraction = 0.45f,
        header = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaterialTheme.dimens.margin.xl,
                        vertical = MaterialTheme.dimens.margin.md,
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Replay request",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "Make an API call through the app's own HTTP client",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                AlohomoraIconButton(onClick = onDismiss, enabled = !inFlight) {
                    Icon(Icons.X, contentDescription = "Close")
                }
            }
        },
    ) {
        Column {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(MaterialTheme.dimens.margin.xl),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    AlohomoraTextField(
                        value = method,
                        onValueChange = { method = it.uppercase() },
                        label = "Method",
                        singleLine = true,
                        enabled = !inFlight,
                        modifier = Modifier.width(120.dp),
                    )
                    AlohomoraTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = "URL",
                        singleLine = true,
                        enabled = !inFlight,
                        modifier = Modifier.weight(1f),
                    )
                }

                AlohomoraTextField(
                    value = headers,
                    onValueChange = { headers = it },
                    label = "Headers",
                    placeholder = "Accept: application/json",
                    singleLine = false,
                    enabled = !inFlight,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    "Body",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    BodyMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = bodyMode == mode,
                            onClick = {
                                if (bodyMode != mode) {
                                    if (mode == BodyMode.JSON) {
                                        jsonBodyState.setText(textBody)
                                    } else {
                                        textBody = jsonBodyState.text
                                    }
                                    bodyMode = mode
                                }
                            },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = BodyMode.entries.size,
                                baseShape = MaterialTheme.shapes.small
                            ),
                        ) {
                            Text(mode.label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                when (bodyMode) {
                    BodyMode.JSON -> {
                        JsonEditor(
                            modifier = Modifier.fillMaxHeight(),
                            state = jsonBodyState,
                            readOnly = inFlight,
                            minLines = 6,
                        )
                    }
                    BodyMode.TEXT -> {
                        AlohomoraTextField(
                            value = textBody,
                            onValueChange = { textBody = it },
                            singleLine = false,
                            enabled = !inFlight,
                            modifier = Modifier
                                .weight(1f),
                        )
                    }
                }

                if (!error.isNullOrBlank()) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            AlohomoraFilledButton(
                text = if (inFlight) "" else "Send",
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
                            body = body.takeIf { it.isNotBlank() },
                        ),
                    )
                },
                enabled = !inFlight && url.isNotBlank() && method.isNotBlank(),
                modifier = Modifier
                    .padding(MaterialTheme.dimens.margin.xl)
                    .fillMaxWidth(),
                uppercase = false,
                content = if (inFlight) {
                    {
                        AlohomoraCircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.background,
                        )
                    }
                } else null,
            )
        }
    }
}
