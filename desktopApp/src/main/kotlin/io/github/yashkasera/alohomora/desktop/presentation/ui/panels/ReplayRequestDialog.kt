package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import io.github.yashkasera.alohomora.ui.icons.Check
import io.github.yashkasera.alohomora.ui.icons.EyeOff
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Plus
import io.github.yashkasera.alohomora.ui.icons.Trash
import io.github.yashkasera.alohomora.ui.icons.X
import io.github.yashkasera.alohomora.ui.theme.alohomoraColors
import io.github.yashkasera.alohomora.ui.theme.dimens

private enum class BodyMode(val label: String) {
    JSON("JSON"),
    TEXT("Text"),
}

private enum class HeaderMode(val label: String) {
    ROWS("Rows"),
    RAW("Raw"),
}

/**
 * One editable header before the request goes back out. [key] is a stable identity so a row keeps its
 * text field state when siblings are added or removed; [included] `false` drops the header from the
 * replay entirely, letting the app's own client regenerate it (auth, signature, content-length).
 */
internal data class ReplayHeaderRow(
    val key: Long,
    val name: String,
    val value: String,
    val included: Boolean,
)

/**
 * Collapses the editable rows back into the wire shape, preserving order and grouping repeated names
 * the same way [ReplayHeaderText.parse] does. Excluded rows and blank names are dropped rather than
 * sent as empty headers.
 */
internal fun buildReplayHeaderMap(rows: List<ReplayHeaderRow>): Map<String, List<String>> {
    val map = LinkedHashMap<String, MutableList<String>>()
    rows.forEach { row ->
        if (!row.included) return@forEach
        val name = row.name.trim()
        if (name.isEmpty()) return@forEach
        map.getOrPut(name) { mutableListOf() }.add(row.value.trim())
    }
    return map
}

private fun Map<String, List<String>>.toReplayHeaderRows(nextKey: () -> Long): List<ReplayHeaderRow> =
    entries.flatMap { (name, values) -> values.map { name to it } }
        .map { (name, value) -> ReplayHeaderRow(nextKey(), name, value, included = true) }

/**
 * Lets the user edit a captured request before the device re-sends it.
 *
 * Everything is editable because the app re-signs the payload on the way out: changing a body here
 * and getting a valid signature back is the reason the feature exists. What is *not* shown is any
 * header the sanitiser removed — redacted secrets and values the client recomputes — since offering
 * `Authorization: [REDACTED]` for editing would invite the user to fix a header the app is about to
 * set correctly anyway.
 *
 * Headers are edited as structured rows seeded from the captured request: a per-row toggle decides
 * what to carry over, the value field is the overwrite, and "Add header" adds custom ones. A raw
 * `Name: value` tab is kept as an escape hatch for pasting a bulk header block.
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

    var nextKey by remember(initial.sourceTraceId) { mutableStateOf(0L) }
    val allocKey = { nextKey++ }
    val headerRows = remember(initial.sourceTraceId) {
        initial.headers.toReplayHeaderRows(allocKey).toMutableStateList()
    }
    var headerMode by remember(initial.sourceTraceId) { mutableStateOf(HeaderMode.ROWS) }
    var rawHeaders by remember(initial.sourceTraceId) {
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

    fun switchHeaderMode(target: HeaderMode) {
        if (headerMode == target) return
        if (target == HeaderMode.RAW) {
            rawHeaders = ReplayHeaderText.render(buildReplayHeaderMap(headerRows))
        } else {
            val parsed = ReplayHeaderText.parse(rawHeaders).toReplayHeaderRows(allocKey)
            headerRows.clear()
            headerRows.addAll(parsed)
        }
        headerMode = target
    }

    fun currentHeaders(): Map<String, List<String>> = when (headerMode) {
        HeaderMode.ROWS -> buildReplayHeaderMap(headerRows)
        HeaderMode.RAW -> ReplayHeaderText.parse(rawHeaders)
    }

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
        Column(modifier = Modifier.fillMaxSize()) {
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

                // Headers — bounded by its own weight and scrolls internally, so it can never
                // starve the body the way the old unbounded multiline field did.
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Headers",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (headerMode == HeaderMode.ROWS) {
                            AlohomoraIconButton(
                                onClick = { headerRows.add(ReplayHeaderRow(allocKey(), "", "", true)) },
                                enabled = !inFlight,
                            ) {
                                Icon(
                                    Icons.Plus,
                                    contentDescription = "Add header",
                                    modifier = Modifier.size(MaterialTheme.dimens.icon.md),
                                )
                            }
                        }
                    }

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        HeaderMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = headerMode == mode,
                                onClick = { switchHeaderMode(mode) },
                                enabled = !inFlight,
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = HeaderMode.entries.size,
                                    baseShape = MaterialTheme.shapes.small,
                                ),
                            ) {
                                Text(mode.label, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    when (headerMode) {
                        HeaderMode.ROWS -> {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                            ) {
                                if (headerRows.isEmpty()) {
                                    Text(
                                        "No headers. Use + to add one, or let the app supply them.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                headerRows.forEachIndexed { index, row ->
                                    HeaderRowEditor(
                                        row = row,
                                        enabled = !inFlight,
                                        onToggle = {
                                            headerRows[index] = row.copy(included = !row.included)
                                        },
                                        onNameChange = { headerRows[index] = row.copy(name = it) },
                                        onValueChange = { headerRows[index] = row.copy(value = it) },
                                        onRemove = { headerRows.removeAt(index) },
                                    )
                                }
                            }
                        }

                        HeaderMode.RAW -> {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                AlohomoraTextField(
                                    value = rawHeaders,
                                    onValueChange = { rawHeaders = it },
                                    placeholder = "Accept: application/json",
                                    singleLine = false,
                                    enabled = !inFlight,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }

                // Body — gets the larger share and always stays visible.
                Column(
                    modifier = Modifier.weight(1.6f),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                ) {
                    Text(
                        "Body",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
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
                                    baseShape = MaterialTheme.shapes.small,
                                ),
                            ) {
                                Text(mode.label, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    when (bodyMode) {
                        BodyMode.JSON -> {
                            JsonEditor(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                state = jsonBodyState,
                                readOnly = inFlight,
                                minLines = 6,
                            )
                        }

                        BodyMode.TEXT -> {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                AlohomoraTextField(
                                    value = textBody,
                                    onValueChange = { textBody = it },
                                    singleLine = false,
                                    enabled = !inFlight,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
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
                            headers = currentHeaders(),
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
                            modifier = Modifier.size(MaterialTheme.dimens.icon.md),
                            strokeWidth = MaterialTheme.dimens.stroke.medium,
                            color = MaterialTheme.colorScheme.background,
                        )
                    }
                } else null,
            )
        }
    }
}

@Composable
private fun HeaderRowEditor(
    row: ReplayHeaderRow,
    enabled: Boolean,
    onToggle: () -> Unit,
    onNameChange: (String) -> Unit,
    onValueChange: (String) -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlohomoraIconButton(onClick = onToggle, enabled = enabled) {
            if (row.included) {
                Icon(
                    Icons.Check,
                    contentDescription = "Included — click to drop",
                    tint = MaterialTheme.alohomoraColors.success,
                    modifier = Modifier.size(MaterialTheme.dimens.icon.md),
                )
            } else {
                Icon(
                    Icons.EyeOff,
                    contentDescription = "Dropped — click to include",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(MaterialTheme.dimens.icon.md),
                )
            }
        }
        val fieldAlpha = if (row.included) 1f else 0.5f
        AlohomoraTextField(
            value = row.name,
            onValueChange = onNameChange,
            placeholder = "Header",
            singleLine = true,
            enabled = enabled,
            modifier = Modifier.weight(0.42f).alpha(fieldAlpha),
        )
        AlohomoraTextField(
            value = row.value,
            onValueChange = onValueChange,
            placeholder = "value",
            singleLine = true,
            enabled = enabled,
            modifier = Modifier.weight(0.58f).alpha(fieldAlpha),
        )
        AlohomoraIconButton(onClick = onRemove, enabled = enabled) {
            Icon(
                Icons.Trash,
                contentDescription = "Remove header",
                modifier = Modifier.size(MaterialTheme.dimens.icon.md),
            )
        }
    }
}
