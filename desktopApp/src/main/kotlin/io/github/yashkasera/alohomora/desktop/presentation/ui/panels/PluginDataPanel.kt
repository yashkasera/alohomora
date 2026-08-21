package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.common.PluginDataFieldDescriptor
import io.github.yashkasera.alohomora.common.PluginDataSnapshot
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.PluginDataViewModel
import io.github.yashkasera.alohomora.ui.components.AlohomoraCard
import io.github.yashkasera.alohomora.ui.components.AlohomoraCardDefaults
import io.github.yashkasera.alohomora.ui.components.AlohomoraChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.ui.components.EmptyState
import io.github.yashkasera.alohomora.ui.components.TopBarLayout
import io.github.yashkasera.alohomora.ui.icons.ChevronDown
import io.github.yashkasera.alohomora.ui.icons.ChevronRight
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Layers
import io.github.yashkasera.alohomora.ui.theme.dimens

@Composable
fun PluginDataPanel(pluginDataViewModel: PluginDataViewModel) {
    val uiState by pluginDataViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            AlohomoraTopBar(
                title = "Plugin Data",
                layout = TopBarLayout.START_ALIGNED,
                subtitle = "${uiState.snapshots.size} plugin${if (uiState.snapshots.size != 1) "s" else ""}",
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            AlohomoraHorizontalDivider()

            if (uiState.isEmpty) {
                EmptyState(
                    icon = Icons.Layers,
                    title = "No plugin data",
                    subtitle = "Data fields appear here once a connected plugin declares dataFields.",
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
                    contentPadding = PaddingValues(MaterialTheme.dimens.margin.md),
                ) {
                    items(uiState.snapshots, key = { it.pluginId }) { snapshot ->
                        PluginCard(
                            snapshot = snapshot,
                            onUpdateField = pluginDataViewModel::updateField,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PluginCard(
    snapshot: PluginDataSnapshot,
    onUpdateField: (pluginId: String, key: String, value: String) -> Unit,
) {
    var expanded by remember { mutableStateOf(true) }

    AlohomoraCard(
        modifier = Modifier.fillMaxWidth(),
        colors = AlohomoraCardDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        onClick = { expanded = !expanded },
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.dimens.margin.xxl,
                vertical = MaterialTheme.dimens.margin.md,
            ),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (expanded) Icons.ChevronDown else Icons.ChevronRight,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.size(MaterialTheme.dimens.icon.sm),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(MaterialTheme.dimens.margin.sm))
                Text(
                    text = snapshot.pluginId,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                AlohomoraChip(
                    label = "${snapshot.fields.size} field${if (snapshot.fields.size != 1) "s" else ""}",
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                )
            }

            AnimatedVisibility(expanded) {
                Column(
                    modifier = Modifier.padding(top = MaterialTheme.dimens.margin.md),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                ) {
                    snapshot.fields.forEach { field ->
                        PluginDataFieldRow(
                            pluginId = snapshot.pluginId,
                            field = field,
                            onUpdate = onUpdateField,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PluginDataFieldRow(
    pluginId: String,
    field: PluginDataFieldDescriptor,
    onUpdate: (pluginId: String, key: String, value: String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = field.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))

        if (field.readOnly) {
            Text(
                text = field.value,
                style = MaterialTheme.typography.bodyMedium,
            )
        } else if (field.type == "select" && field.options != null) {
            SelectField(
                value = field.value,
                options = field.options!!,
                onSelect = { onUpdate(pluginId, field.key, it) },
            )
        } else {
            EditableTextField(
                value = field.value,
                onSubmit = { onUpdate(pluginId, field.key, it) },
            )
        }
    }
}

@Composable
private fun EditableTextField(
    value: String,
    onSubmit: (String) -> Unit,
) {
    var draft by remember(value) { mutableStateOf(value) }
    val modified = draft != value

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
    ) {
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall,
        )
        if (modified) {
            OutlinedButton(onClick = { onSubmit(draft) }) {
                Text("Apply", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun SelectField(
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(onClick = { menuExpanded = true }) {
            Text(value.ifEmpty { "Select..." }, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.ChevronDown,
                contentDescription = "Open",
                modifier = Modifier.size(16.dp),
            )
        }
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, style = MaterialTheme.typography.bodySmall) },
                    onClick = {
                        menuExpanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}
