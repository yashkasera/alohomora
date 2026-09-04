package io.github.yashkasera.alohomora.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import io.github.yashkasera.alohomora.ui.theme.alohomoraColors

object AlohomoraChipDefaults {
    val shape @Composable get() = MaterialTheme.shapes.extraSmall
    const val uppercase: Boolean = true
    val contentPadding: PaddingValues = PaddingValues(horizontal = 6.dp, vertical = 2.dp)

    @Composable
    fun getContentColorFor(containerColor: Color): Color =
        contentColorFor(containerColor).takeOrElse { LocalContentColor.current }
}

@Composable
fun AlohomoraAssistChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    uppercase: Boolean = AlohomoraChipDefaults.uppercase,
    leadingIcon: (@Composable (() -> Unit))? = null,
    trailingIcon: (@Composable (() -> Unit))? = null,
) {
    val text = if (uppercase) label.uppercase() else label
    AssistChip(
        onClick = onClick,
        modifier = modifier,
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
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    uppercase: Boolean = AlohomoraChipDefaults.uppercase,
    leadingIcon: (@Composable (() -> Unit))? = null,
    trailingIcon: (@Composable (() -> Unit))? = null,
) {
    val text = if (uppercase) label.uppercase() else label
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
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

/**
 * [modifier] exists so callers can attach a test tag. Before it did, the only way to address a chip
 * was to wrap it in a tagged `Box` — see the note this replaced in `NavigationHistoryScreen`.
 */
@Composable
fun AlohomoraChip(
    label: String,
    modifier: Modifier = Modifier,
    uppercase: Boolean = AlohomoraChipDefaults.uppercase,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = AlohomoraChipDefaults.getContentColorFor(containerColor),
    shape: Shape = AlohomoraChipDefaults.shape,
    border: BorderStroke? = null,
) {
    val text = if (uppercase) label.uppercase() else label
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = contentColor,
        // A chip is a single-line token. Without this, a chip squeezed for width wraps its label
        // across lines and renders as a tall broken box; a caller with genuinely long labels
        // should lay chips out in a FlowRow so each keeps its intrinsic width.
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(shape)
            .background(color = containerColor)
            .then(
                if (border != null) Modifier.border(border, shape) else Modifier,
            )
            .padding(AlohomoraChipDefaults.contentPadding),
    )
}

@Preview
@Composable
private fun AlohomoraChipsPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AlohomoraChip(label = "GET")
                AlohomoraChip(
                    label = "200 OK",
                    containerColor = MaterialTheme.alohomoraColors.successContainer,
                    contentColor = MaterialTheme.alohomoraColors.success,
                )
                AlohomoraFilterChip(label = "Errors", selected = true, onClick = {})
                AlohomoraFilterChip(label = "Warnings", selected = false, onClick = {})
                AlohomoraAssistChip(label = "Copy", onClick = {})
            }
        }
    }
}
