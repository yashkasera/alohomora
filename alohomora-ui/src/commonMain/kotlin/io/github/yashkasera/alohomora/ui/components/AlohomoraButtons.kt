package io.github.yashkasera.alohomora.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class AlohomoraButtonSize {
    SMALL,
    MEDIUM,
    LARGE,
}

enum class AlohomoraIconButtonStyle {
    DEFAULT,
    FILLED,
    OUTLINED,
    TONAL,
}

object AlohomoraButtonDefaults {
    val shape @Composable get() = MaterialTheme.shapes.extraSmall
    val uppercase: Boolean = true
    val iconSpacing: Dp = 8.dp

    fun contentPadding(size: AlohomoraButtonSize): PaddingValues = when (size) {
        AlohomoraButtonSize.SMALL -> PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        AlohomoraButtonSize.MEDIUM -> PaddingValues(horizontal = 20.dp, vertical = 10.dp)
        AlohomoraButtonSize.LARGE -> PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    }

    fun minHeight(size: AlohomoraButtonSize): Int = when (size) {
        AlohomoraButtonSize.SMALL -> 32
        AlohomoraButtonSize.MEDIUM -> 40
        AlohomoraButtonSize.LARGE -> 48
    }

    @Composable
    fun textStyle(size: AlohomoraButtonSize): TextStyle = when (size) {
        AlohomoraButtonSize.SMALL -> MaterialTheme.typography.labelSmall
        AlohomoraButtonSize.MEDIUM -> MaterialTheme.typography.labelMedium
        AlohomoraButtonSize.LARGE -> MaterialTheme.typography.labelLarge
    }
}

@Composable
fun AlohomoraFilledButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: AlohomoraButtonSize = AlohomoraButtonSize.MEDIUM,
    uppercase: Boolean = AlohomoraButtonDefaults.uppercase,
    shape: Shape? = null,
    containerColor: Color = MaterialTheme.colorScheme.onBackground,
    contentColor: Color = MaterialTheme.colorScheme.background,
    leadingIcon: (@Composable (() -> Unit))? = null,
    trailingIcon: (@Composable (() -> Unit))? = null,
    content: (@Composable RowScope.() -> Unit)? = null,
) {
    val resolvedShape = shape ?: MaterialTheme.shapes.extraSmall
    AlohomoraButtonBase(
        text = text,
        size = size,
        uppercase = uppercase,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        content = content,
    ) { contentPadding, contentSlot ->
        Button(
            onClick = onClick,
            modifier = modifier.defaultMinSize(minHeight = AlohomoraButtonDefaults.minHeight(size).dp),
            enabled = enabled,
            shape = resolvedShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor,
            ),
            contentPadding = contentPadding,
            content = contentSlot,
        )
    }
}

@Composable
fun AlohomoraElevatedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: AlohomoraButtonSize = AlohomoraButtonSize.MEDIUM,
    uppercase: Boolean = AlohomoraButtonDefaults.uppercase,
    shape: Shape? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    leadingIcon: (@Composable (() -> Unit))? = null,
    trailingIcon: (@Composable (() -> Unit))? = null,
    content: (@Composable RowScope.() -> Unit)? = null,
) {
    val resolvedShape = shape ?: MaterialTheme.shapes.extraSmall
    AlohomoraButtonBase(
        text = text,
        size = size,
        uppercase = uppercase,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        content = content,
    ) { contentPadding, contentSlot ->
        ElevatedButton(
            onClick = onClick,
            modifier = modifier.defaultMinSize(minHeight = AlohomoraButtonDefaults.minHeight(size).dp),
            enabled = enabled,
            shape = resolvedShape,
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = containerColor,
                contentColor = contentColor,
            ),
            contentPadding = contentPadding,
            content = contentSlot,
        )
    }
}

@Composable
fun AlohomoraOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: AlohomoraButtonSize = AlohomoraButtonSize.MEDIUM,
    uppercase: Boolean = AlohomoraButtonDefaults.uppercase,
    shape: Shape? = null,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    borderWidth: Dp = 1.dp,
    leadingIcon: (@Composable (() -> Unit))? = null,
    trailingIcon: (@Composable (() -> Unit))? = null,
    content: (@Composable RowScope.() -> Unit)? = null,
) {
    val resolvedShape = shape ?: MaterialTheme.shapes.extraSmall
    AlohomoraButtonBase(
        text = text,
        size = size,
        uppercase = uppercase,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        content = content,
    ) { contentPadding, contentSlot ->
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.defaultMinSize(minHeight = AlohomoraButtonDefaults.minHeight(size).dp),
            enabled = enabled,
            shape = resolvedShape,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = contentColor,
            ),
            border = BorderStroke(borderWidth, borderColor),
            contentPadding = contentPadding,
            content = contentSlot,
        )
    }
}

@Composable
fun AlohomoraTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: AlohomoraButtonSize = AlohomoraButtonSize.MEDIUM,
    uppercase: Boolean = AlohomoraButtonDefaults.uppercase,
    shape: Shape? = null,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    leadingIcon: (@Composable (() -> Unit))? = null,
    trailingIcon: (@Composable (() -> Unit))? = null,
    content: (@Composable RowScope.() -> Unit)? = null,
) {
    val resolvedShape = shape ?: MaterialTheme.shapes.extraSmall
    AlohomoraButtonBase(
        text = text,
        size = size,
        uppercase = uppercase,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        content = content,
    ) { contentPadding, contentSlot ->
        TextButton(
            onClick = onClick,
            modifier = modifier.defaultMinSize(minHeight = AlohomoraButtonDefaults.minHeight(size).dp),
            enabled = enabled,
            shape = resolvedShape,
            colors = ButtonDefaults.textButtonColors(
                contentColor = contentColor,
            ),
            contentPadding = contentPadding,
            content = contentSlot,
        )
    }
}

@Composable
fun AlohomoraIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: AlohomoraIconButtonStyle = AlohomoraIconButtonStyle.DEFAULT,
    content: @Composable () -> Unit,
) {
    when (style) {
        AlohomoraIconButtonStyle.DEFAULT -> IconButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            content = content,
        )
        AlohomoraIconButtonStyle.FILLED -> FilledIconButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.onBackground,
                contentColor = MaterialTheme.colorScheme.background,
            ),
            content = content,
        )
        AlohomoraIconButtonStyle.OUTLINED -> OutlinedIconButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = IconButtonDefaults.outlinedIconButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            content = content,
        )
        AlohomoraIconButtonStyle.TONAL -> FilledTonalIconButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            content = content,
        )
    }
}

@Deprecated(
    "Use AlohomoraFilledButton instead.",
    ReplaceWith("AlohomoraFilledButton(text = text, onClick = onClick, modifier = modifier, enabled = enabled)")
)
@Composable
fun AlohomoraButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    AlohomoraFilledButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    )
}

@Composable
private fun AlohomoraButtonBase(
    text: String,
    size: AlohomoraButtonSize,
    uppercase: Boolean,
    leadingIcon: (@Composable (() -> Unit))?,
    trailingIcon: (@Composable (() -> Unit))?,
    content: (@Composable RowScope.() -> Unit)?,
    button: @Composable (PaddingValues, @Composable RowScope.() -> Unit) -> Unit,
) {
    val label = if (uppercase) text.uppercase() else text
    val contentPadding = AlohomoraButtonDefaults.contentPadding(size)
    val baseContent: @Composable RowScope.() -> Unit = content ?: {
        if (leadingIcon != null) {
            leadingIcon()
        }
        Text(
            text = label,
            style = AlohomoraButtonDefaults.textStyle(size),
            modifier = Modifier.padding(
                horizontal = if (leadingIcon != null || trailingIcon != null) {
                    AlohomoraButtonDefaults.iconSpacing
                } else {
                    0.dp
                }
            ),
        )
        if (trailingIcon != null) {
            trailingIcon()
        }
    }
    button(contentPadding, baseContent)
}
