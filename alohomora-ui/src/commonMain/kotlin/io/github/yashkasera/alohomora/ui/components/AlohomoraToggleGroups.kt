package io.github.yashkasera.alohomora.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import androidx.compose.ui.tooling.preview.Preview

data class AlohomoraToggleItem(
    val id: String,
    val label: String,
    val leadingIcon: (@Composable (() -> Unit))? = null,
)

object AlohomoraToggleDefaults {
    val shape @Composable get() = MaterialTheme.shapes.extraSmall
    val borderWidth: Dp = 1.dp
    val itemPadding @Composable get() = androidx.compose.foundation.layout.PaddingValues(
        horizontal = 16.dp,
        vertical = 10.dp,
    )

    val uppercase: Boolean = true
}

@Composable
fun AlohomoraSingleChoiceToggleGroup(
    items: List<AlohomoraToggleItem>,
    selectedId: String?,
    onSelectedIdChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    spacing: Dp = 8.dp,
    uppercase: Boolean = AlohomoraToggleDefaults.uppercase,
) {
    Row(
        modifier = modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(spacing),
    ) {
        items.forEach { item ->
            val selected = item.id == selectedId
            AlohomoraToggleItem(
                item = item,
                selected = selected,
                uppercase = uppercase,
                modifier = Modifier.selectable(
                    selected = selected,
                    onClick = { onSelectedIdChange(item.id) },
                    role = Role.RadioButton,
                ),
            )
        }
    }
}

@Composable
private fun AlohomoraToggleItem(
    item: AlohomoraToggleItem,
    selected: Boolean,
    uppercase: Boolean,
    modifier: Modifier = Modifier,
) {
    val label = if (uppercase) item.label.uppercase() else item.label
    val containerColor = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surface
    val contentColor = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = modifier,
        shape = AlohomoraToggleDefaults.shape,
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(AlohomoraToggleDefaults.borderWidth, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(AlohomoraToggleDefaults.itemPadding),
        ) {
            item.leadingIcon?.invoke()
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Preview
@Composable
private fun AlohomoraSingleChoiceToggleGroupPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            AlohomoraSingleChoiceToggleGroup(
                items = listOf(
                    AlohomoraToggleItem("all", "All"),
                    AlohomoraToggleItem("errors", "Errors"),
                    AlohomoraToggleItem("warnings", "Warnings"),
                ),
                selectedId = "all",
                onSelectedIdChange = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
