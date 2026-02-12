package io.github.yashkasera.alohomora.ui.components

import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

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
