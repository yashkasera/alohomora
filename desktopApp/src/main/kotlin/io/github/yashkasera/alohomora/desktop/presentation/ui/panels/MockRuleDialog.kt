package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.common.MockRule
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Trash
import io.github.yashkasera.alohomora.ui.theme.dimens

@Composable
fun MockRuleDialog(
    rules: List<MockRule>,
    onAddRule: (MockRule) -> Unit,
    onUpdateRule: (MockRule) -> Unit,
    onDeleteRule: (String) -> Unit,
    onToggleRule: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var showAddForm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mock rules") },
        text = {
            Column(
                modifier = Modifier.width(520.dp),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
            ) {
                if (rules.isEmpty() && !showAddForm) {
                    Text(
                        "No mock rules configured. Add a rule to intercept matching requests and return a custom response.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (rules.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xs),
                    ) {
                        items(rules, key = { it.id }) { rule ->
                            MockRuleRow(
                                rule = rule,
                                onToggle = { onToggleRule(rule.id) },
                                onDelete = { onDeleteRule(rule.id) },
                            )
                        }
                    }
                }

                if (showAddForm) {
                    HorizontalDivider()
                    AddMockRuleForm(
                        onAdd = { rule ->
                            onAddRule(rule)
                            showAddForm = false
                        },
                        onCancel = { showAddForm = false },
                    )
                }
            }
        },
        confirmButton = {
            if (!showAddForm) {
                TextButton(onClick = { showAddForm = true }) {
                    Text("Add rule")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun MockRuleRow(
    rule: MockRule,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = MaterialTheme.dimens.margin.xs,
                    end = MaterialTheme.dimens.margin.sm,
                    top = MaterialTheme.dimens.margin.xs,
                    bottom = MaterialTheme.dimens.margin.xs,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = rule.enabled, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = buildString {
                        rule.method?.let { append("$it ") }
                        append(rule.urlPattern)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (!rule.enabled) TextDecoration.LineThrough else null,
                )
                Text(
                    text = "${rule.statusCode} · ${rule.contentType}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AlohomoraIconButton(onClick = onDelete) {
                Icon(Icons.Trash, contentDescription = "Delete rule")
            }
        }
    }
}

private val HTTP_METHODS = listOf(null, "GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS")

@Composable
private fun AddMockRuleForm(
    onAdd: (MockRule) -> Unit,
    onCancel: () -> Unit,
) {
    var urlPattern by remember { mutableStateOf("") }
    var isRegex by remember { mutableStateOf(false) }
    var method by remember { mutableStateOf<String?>(null) }
    var statusCode by remember { mutableStateOf("200") }
    var contentType by remember { mutableStateOf("application/json") }
    var responseBody by remember { mutableStateOf("") }
    var showMethodDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
    ) {
        Text("New mock rule", style = MaterialTheme.typography.titleSmall)

        OutlinedTextField(
            value = urlPattern,
            onValueChange = { urlPattern = it },
            label = { Text("URL pattern") },
            placeholder = { Text("/api/users") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isRegex, onCheckedChange = { isRegex = it })
                Text("Regex", style = MaterialTheme.typography.bodySmall)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { showMethodDropdown = true }) {
                    Text(method ?: "Any method")
                }
                DropdownMenu(
                    expanded = showMethodDropdown,
                    onDismissRequest = { showMethodDropdown = false },
                ) {
                    HTTP_METHODS.forEach { m ->
                        DropdownMenuItem(
                            text = { Text(m ?: "Any method") },
                            onClick = {
                                method = m
                                showMethodDropdown = false
                            },
                        )
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm)) {
            OutlinedTextField(
                value = statusCode,
                onValueChange = { statusCode = it.filter { c -> c.isDigit() }.take(3) },
                label = { Text("Status") },
                singleLine = true,
                modifier = Modifier.width(100.dp),
            )
            OutlinedTextField(
                value = contentType,
                onValueChange = { contentType = it },
                label = { Text("Content-Type") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }

        OutlinedTextField(
            value = responseBody,
            onValueChange = { responseBody = it },
            label = { Text("Response body") },
            minLines = 3,
            maxLines = 8,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onCancel) { Text("Cancel") }
            Spacer(Modifier.width(MaterialTheme.dimens.margin.sm))
            TextButton(
                onClick = {
                    val code = statusCode.toIntOrNull() ?: 200
                    onAdd(
                        MockRule(
                            id = "",
                            enabled = true,
                            urlPattern = urlPattern,
                            isRegex = isRegex,
                            method = method,
                            statusCode = code,
                            responseBody = responseBody,
                            contentType = contentType,
                        ),
                    )
                },
                enabled = urlPattern.isNotBlank(),
            ) {
                Text("Add")
            }
        }
    }
}
