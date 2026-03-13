package io.github.yashkasera.alohomora.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

object AlohomoraChipDefaults {
    val shape @Composable get() = MaterialTheme.shapes.extraSmall
    val uppercase: Boolean = true
}

@Composable
fun AlohomoraAssistChip(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    uppercase: Boolean = AlohomoraChipDefaults.uppercase,
    leadingIcon: (@Composable (() -> Unit))? = null,
    trailingIcon: (@Composable (() -> Unit))? = null,
) {
    val text = if (uppercase) label.uppercase() else label
    AssistChip(
        onClick = onClick,
        enabled = enabled,
        label = { Text(text = text, style = MaterialTheme.typography.labelSmall) },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = AlohomoraChipDefaults.shape,
        colors = AssistChipDefaults.assistChipColors(
            labelColor = MaterialTheme.colorScheme.onSurface,
            leadingIconContentColor = MaterialTheme.colorScheme.onSurface,
            trailingIconContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

@Composable
fun AlohomoraFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    uppercase: Boolean = AlohomoraChipDefaults.uppercase,
    leadingIcon: (@Composable (() -> Unit))? = null,
    trailingIcon: (@Composable (() -> Unit))? = null,
) {
    val text = if (uppercase) label.uppercase() else label
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = { Text(text = text, style = MaterialTheme.typography.labelSmall) },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = AlohomoraChipDefaults.shape,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.onBackground,
            selectedLabelColor = MaterialTheme.colorScheme.background,
            selectedLeadingIconColor = MaterialTheme.colorScheme.background,
            selectedTrailingIconColor = MaterialTheme.colorScheme.background,
            labelColor = MaterialTheme.colorScheme.onSurface,
            iconColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

@Composable
fun AlohomoraChip(
    label: String,
    uppercase: Boolean = AlohomoraChipDefaults.uppercase,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = contentColorFor(containerColor),
    shape: Shape = AlohomoraChipDefaults.shape,
    borderStroke: BorderStroke? = null
) {
    val text = if (uppercase) label.uppercase() else label
    val modifier = if (borderStroke != null){
        Modifier.border(borderStroke, shape = shape)
    } else Modifier
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = contentColor,
        modifier = modifier
            .background(
                color = containerColor,
                shape = AlohomoraChipDefaults.shape
            )
            .padding(
                horizontal = 6.dp,
                vertical = 2.dp
            )
    )
}
