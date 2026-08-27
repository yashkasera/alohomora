package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.common.MockRule
import io.github.yashkasera.alohomora.desktop.data.local.MockSession
import io.github.yashkasera.alohomora.desktop.data.local.MockSessionSummary
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.AlohomoraSideSheet
import io.github.yashkasera.alohomora.ui.components.AlohomoraAlertDialog
import io.github.yashkasera.alohomora.ui.components.AlohomoraButtonSize
import io.github.yashkasera.alohomora.ui.components.AlohomoraCheckbox
import io.github.yashkasera.alohomora.ui.components.AlohomoraCodeBlock
import io.github.yashkasera.alohomora.ui.components.AlohomoraDropdownMenu
import io.github.yashkasera.alohomora.ui.components.AlohomoraDropdownMenuItem
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilledButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraFloatingActionButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraOutlinedButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextField
import io.github.yashkasera.alohomora.ui.components.AlohomoraTriStateCheckbox
import io.github.yashkasera.alohomora.ui.components.fabClearanceItem
import io.github.yashkasera.alohomora.ui.components.jsoneditor.JsonEditor
import io.github.yashkasera.alohomora.ui.components.jsoneditor.JsonEditorState
import io.github.yashkasera.alohomora.ui.icons.ChevronDown
import io.github.yashkasera.alohomora.ui.icons.Download
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Plus
import io.github.yashkasera.alohomora.ui.icons.Save
import io.github.yashkasera.alohomora.ui.icons.Trash
import io.github.yashkasera.alohomora.ui.icons.Upload
import io.github.yashkasera.alohomora.ui.icons.X
import io.github.yashkasera.alohomora.ui.theme.dimens

@Composable
fun MockRulesSideSheet(
    visible: Boolean,
    rules: List<MockRule>,
    currentSession: MockSession?,
    sessions: List<MockSessionSummary>,
    onAddRule: (MockRule) -> Unit,
    onUpdateRule: (MockRule) -> Unit,
    onDeleteRule: (String) -> Unit,
    onToggleRule: (String) -> Unit,
    onToggleAll: () -> Unit,
    onLoadSession: (String) -> Unit,
    onSaveSession: (String) -> Unit,
    onSaveAsSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onDetachSession: () -> Unit,
    onExport: (String) -> Unit,
    onImport: (String) -> String?,
    onDismiss: () -> Unit,
) {
    var editingRule by remember { mutableStateOf<MockRule?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showSessionDropdown by remember { mutableStateOf(false) }
    var saveAsMode by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }

    AlohomoraSideSheet(
        visible = visible,
        onDismiss = onDismiss,
        widthFraction = 0.5f,
        floatingActionButton = {
            AlohomoraFloatingActionButton(
                onClick = { editingRule = BLANK_RULE },
                containerColor = MaterialTheme.colorScheme.inverseSurface,
            ) {
                Icon(
                    Icons.Plus,
                    contentDescription = "Add rule",
                )
            }
        },
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
                        "Mock rules",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        currentSession?.name ?: "No session",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                val count = rules.count { it.enabled }
                val state = when {
                    count != 0 && count == rules.size -> ToggleableState.On
                    count >= 1 -> ToggleableState.Indeterminate
                    else -> ToggleableState.Off
                }

                AlohomoraOutlinedButton(
                    text = "Enable Mocking",
                    leadingIcon = {
                        AlohomoraTriStateCheckbox(
                            state = state,
                            onClick = null,
                        )
                    },
                    onClick = onToggleAll,
                )
                AlohomoraIconButton(onClick = onDismiss) {
                    Icon(Icons.X, contentDescription = "Close")
                }
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.secondary) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = MaterialTheme.dimens.margin.sm),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AlohomoraFilledButton(
                        text = "Sessions",
                        trailingIcon = {
                            Icon(
                                modifier = Modifier.size(MaterialTheme.dimens.icon.md),
                                imageVector = Icons.ChevronDown,
                                contentDescription = null,
                            )
                        },
                        onClick = { showSessionDropdown = true },
                        size = AlohomoraButtonSize.SMALL,
                        uppercase = false,
                    )
                    AlohomoraDropdownMenu(
                        expanded = showSessionDropdown,
                        onDismissRequest = { showSessionDropdown = false },
                    ) {
                        if (sessions.isEmpty()) {
                            AlohomoraDropdownMenuItem(
                                text = {
                                    Text(
                                        "No saved sessions",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                onClick = {},
                                enabled = false,
                            )
                        }
                        sessions.forEach { summary ->
                            AlohomoraDropdownMenuItem(
                                text = {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(summary.name)
                                        Text(
                                            "${summary.ruleCount} rules",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                },
                                trailingIcon = {
                                    AlohomoraIconButton(
                                        onClick = {
                                            showSessionDropdown = false
                                            onDeleteSession(summary.id)
                                        },
                                    ) {
                                        Icon(
                                            modifier = Modifier.size(MaterialTheme.dimens.icon.md),
                                            imageVector = Icons.Trash,
                                            contentDescription = "Delete session",
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                },
                                onClick = {
                                    showSessionDropdown = false
                                    onLoadSession(summary.id)
                                },
                            )
                        }
                        if (currentSession != null) {
                            AlohomoraHorizontalDivider()
                            AlohomoraDropdownMenuItem(
                                text = { Text("Detach session") },
                                onClick = {
                                    showSessionDropdown = false
                                    onDetachSession()
                                },
                            )
                            AlohomoraDropdownMenuItem(
                                text = {
                                    Text(
                                        "Delete session",
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                },
                                onClick = {
                                    showSessionDropdown = false
                                    onDeleteSession(currentSession.id)
                                },
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    if (rules.isNotEmpty()) {
                        AlohomoraIconButton(
                            onClick = {
                                saveAsMode = currentSession == null
                                showSaveDialog = true
                            },
                        ) {
                            Icon(
                                Icons.Save,
                                contentDescription = if (currentSession != null) "Save" else "Save as",
                            )
                        }
                        AlohomoraIconButton(
                            onClick = {
                                val path = io.github.yashkasera.alohomora.desktop.util.pickSavePath(
                                    defaultName = (currentSession?.name
                                        ?: "mock-rules") + ".alohomora-mocks.json",
                                    dialogTitle = "Export mock rules",
                                    extension = ".json",
                                )
                                if (path != null) onExport(path)
                            },
                        ) {
                            Icon(Icons.Download, contentDescription = "Export")
                        }
                    }
                    AlohomoraIconButton(
                        onClick = {
                            val path = io.github.yashkasera.alohomora.desktop.util.pickLoadPath(
                                dialogTitle = "Import mock rules",
                                ".json", ".har",
                            )
                            if (path != null) {
                                importError = onImport(path)
                            }
                        },
                    ) {
                        Icon(Icons.Upload, contentDescription = "Import")
                    }
                }
            }

            if (importError != null) {
                Text(
                    importError!!,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = MaterialTheme.dimens.margin.sm),
                )
            }

            AlohomoraHorizontalDivider()

            if (rules.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(MaterialTheme.dimens.margin.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        "No mock rules",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Add a rule to intercept matching requests and return a custom response.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = MaterialTheme.dimens.margin.xs),
                    )
                    AlohomoraOutlinedButton(
                        text = "Add rule",
                        onClick = { editingRule = BLANK_RULE },
                        modifier = Modifier.padding(top = MaterialTheme.dimens.margin.md),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(
                        vertical = MaterialTheme.dimens.margin.sm,
                    ),
                ) {
                    items(rules, key = { it.id }) { rule ->
                        MockRuleRow(
                            rule = rule,
                            onClick = { editingRule = rule },
                            onToggle = { onToggleRule(rule.id) },
                            onDelete = { onDeleteRule(rule.id) },
                        )
                        AlohomoraHorizontalDivider()
                    }
                    fabClearanceItem()
                }
            }
        }
    }

    EditMockRuleSideSheet(
        visible = visible && editingRule != null,
        rule = editingRule,
        onSave = { rule ->
            if (editingRule?.id?.isBlank() != false) {
                onAddRule(rule)
            } else {
                onUpdateRule(rule)
            }
            editingRule = null
        },
        onDismiss = { editingRule = null },
    )

    if (showSaveDialog) {
        SaveSessionDialog(
            initialName = if (saveAsMode) "" else (currentSession?.name ?: ""),
            onSave = { name ->
                if (saveAsMode || currentSession == null) {
                    onSaveAsSession(name)
                } else {
                    onSaveSession(name)
                }
                showSaveDialog = false
            },
            onDismiss = { showSaveDialog = false },
        )
    }
}

private val BLANK_RULE = MockRule(
    id = "",
    urlPattern = "",
    statusCode = 200,
    contentType = "application/json",
)

@Composable
private fun EditMockRuleSideSheet(
    visible: Boolean,
    rule: MockRule?,
    onSave: (MockRule) -> Unit,
    onDismiss: () -> Unit,
) {
    val isNew = rule?.id?.isBlank() != false
    var name by remember(rule) { mutableStateOf(rule?.name ?: "") }
    var urlPattern by remember(rule) { mutableStateOf(rule?.urlPattern ?: "") }
    var isRegex by remember(rule) { mutableStateOf(rule?.isRegex ?: false) }
    var method by remember(rule) { mutableStateOf(rule?.method) }
    var statusCode by remember(rule) { mutableStateOf(rule?.statusCode?.toString() ?: "200") }
    var contentType by remember(rule) { mutableStateOf(rule?.contentType ?: "application/json") }
    val responseBodyState = remember(rule) { JsonEditorState(rule?.responseBody ?: "") }
    var showMethodDropdown by remember { mutableStateOf(false) }
    var showGeneratorDropdown by remember { mutableStateOf(false) }

    AlohomoraSideSheet(
        visible = visible,
        onDismiss = onDismiss,
        widthFraction = 0.4f,
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
                Text(
                    if (isNew) "New mock rule" else "Edit mock rule",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                AlohomoraIconButton(onClick = onDismiss) {
                    Icon(Icons.X, contentDescription = "Close")
                }
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = MaterialTheme.dimens.margin.xl,
                    vertical = MaterialTheme.dimens.margin.md,
                ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
        ) {
            AlohomoraTextField(
                value = name,
                onValueChange = { name = it },
                label = "Name",
                placeholder = "e.g. Empty list, Error response",
                modifier = Modifier.fillMaxWidth(),
            )

            AlohomoraTextField(
                value = urlPattern,
                onValueChange = { urlPattern = it },
                label = "URL pattern",
                placeholder = "/api/users",
            )


            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AlohomoraOutlinedButton(
                        text = method ?: "Any method",
                        onClick = { showMethodDropdown = true },
                        size = AlohomoraButtonSize.SMALL,
                        uppercase = false,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.ChevronDown,
                                contentDescription = null,
                                modifier = Modifier.size(MaterialTheme.dimens.margin.sm),
                            )
                        },
                    )
                    AlohomoraDropdownMenu(
                        expanded = showMethodDropdown,
                        onDismissRequest = { showMethodDropdown = false },
                    ) {
                        HTTP_METHODS.forEach { m ->
                            AlohomoraDropdownMenuItem(
                                text = {
                                    Text(
                                        text = m ?: "Any method",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                },
                                onClick = {
                                    method = m
                                    showMethodDropdown = false
                                },
                            )
                        }
                    }
                }
                AlohomoraTextButton(
                    text = "Regex",
                    leadingIcon = {
                        AlohomoraCheckbox(checked = isRegex, onCheckedChange = null)
                    },
                    size = AlohomoraButtonSize.SMALL,
                    uppercase = false,
                    onClick = {
                        isRegex = !isRegex

                    },
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm)) {
                AlohomoraTextField(
                    value = statusCode,
                    onValueChange = { statusCode = it.filter { c -> c.isDigit() }.take(3) },
                    label = "Status",
                    modifier = Modifier.width(100.dp),
                )
                AlohomoraTextField(
                    value = contentType,
                    onValueChange = { contentType = it },
                    label = "Content-Type",
                    modifier = Modifier.weight(1f),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Response body", style = MaterialTheme.typography.bodySmall)
                    AlohomoraTextButton(
                        text = "Insert generator",
                        onClick = { showGeneratorDropdown = true },
                        size = AlohomoraButtonSize.SMALL,
                        uppercase = false,
                    )
                    AlohomoraDropdownMenu(
                        expanded = showGeneratorDropdown,
                        onDismissRequest = { showGeneratorDropdown = false },
                    ) {
                        GENERATORS.forEach { (label, syntax) ->
                            AlohomoraDropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(label)
                                        Text(
                                            "{{$syntax}}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                },
                                onClick = {
                                    showGeneratorDropdown = false
                                    responseBodyState.insertTemplate(syntax)
                                },
                            )
                        }
                    }
                }
                JsonEditor(
                    modifier = Modifier.fillMaxHeight(),
                    state = responseBodyState,
                    minLines = 10,
                )
            }

            AlohomoraFilledButton(
                text = if (isNew) "Add" else "Save",
                onClick = {
                    val code = statusCode.toIntOrNull() ?: 200
                    onSave(
                        MockRule(
                            id = rule?.id ?: "",
                            name = name.ifBlank { null },
                            enabled = rule?.enabled ?: true,
                            urlPattern = urlPattern,
                            isRegex = isRegex,
                            method = method,
                            statusCode = code,
                            responseBody = responseBodyState.text,
                            contentType = contentType,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = urlPattern.isNotBlank(),
                size = AlohomoraButtonSize.MEDIUM,
                uppercase = false,
            )
        }
    }
}

@Composable
private fun SaveSessionDialog(
    initialName: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    AlohomoraAlertDialog(
        onDismissRequest = onDismiss,
        title = "Save session",
        content = {
            AlohomoraTextField(
                value = name,
                onValueChange = { name = it },
                label = "Session name",
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            AlohomoraFilledButton(
                text = "Save",
                onClick = { onSave(name.trim()) },
                enabled = name.isNotBlank(),
                size = AlohomoraButtonSize.SMALL,
                uppercase = false,
            )
        },
        dismissButton = {
            AlohomoraTextButton(
                text = "Cancel",
                onClick = onDismiss,
                size = AlohomoraButtonSize.SMALL,
                uppercase = false,
            )
        },
    )
}

@Composable
private fun MockRuleRow(
    rule: MockRule,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        color = if (rule.enabled) MaterialTheme.colorScheme.surfaceContainerLowest else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (rule.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = MaterialTheme.dimens.margin.xs,
                    end = MaterialTheme.dimens.margin.sm,
                    top = MaterialTheme.dimens.margin.md,
                    bottom = MaterialTheme.dimens.margin.md,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlohomoraCheckbox(checked = rule.enabled, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f)) {
                val ruleName = rule.name
                ruleName?.ifEmpty { null }?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xs))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = buildString {
                            rule.method?.let { append("$it ") }
                            append(rule.urlPattern)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${rule.statusCode}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                rule.responseBody.ifBlank { null }?.let { body ->
                    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))
                    val maxPreviewChars = 200
                    val truncated = body.length > maxPreviewChars
                    AlohomoraCodeBlock(
                        content = if (truncated) body.take(maxPreviewChars) + " …" else body,
                        isScrollable = false,
                    )
                    if (truncated) {
                        Text(
                            text = "Click to view full response",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = MaterialTheme.dimens.margin.xs),
                        )
                    }
                }
            }
            AlohomoraIconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Trash,
                    contentDescription = "Delete rule",
                    modifier = Modifier.size(MaterialTheme.dimens.icon.md),
                )
            }
        }
    }
}

private val HTTP_METHODS = listOf(null, "GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS")

private val GENERATORS = listOf(
    "UUID" to "uuid",
    "Full name" to "name",
    "First name" to "firstName",
    "Last name" to "lastName",
    "Email" to "email",
    "Integer" to "int(1,100)",
    "Float" to "float(0,1)",
    "Amount" to "amount(10,500)",
    "Date (past)" to "date(past,30)",
    "Date (future)" to "date(future,365)",
    "Timestamp" to "timestamp",
    "Boolean" to "bool",
    "One of..." to "oneOf(a,b,c)",
)
