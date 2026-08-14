package io.github.yashkasera.alohomora.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
    val shape @Composable get() = MaterialTheme.shapes.small
    val uppercase: Boolean = true
    val iconSpacing: Dp = 8.dp

    fun contentPadding(size: AlohomoraButtonSize): PaddingValues = when (size) {
        AlohomoraButtonSize.SMALL -> PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        AlohomoraButtonSize.MEDIUM -> PaddingValues(horizontal = 20.dp, vertical = 10.dp)
        AlohomoraButtonSize.LARGE -> PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    }

    fun minHeight(size: AlohomoraButtonSize): Dp = when (size) {
        AlohomoraButtonSize.SMALL -> 32.dp
        AlohomoraButtonSize.MEDIUM -> 40.dp
        AlohomoraButtonSize.LARGE -> 48.dp
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
    shape: Shape = AlohomoraButtonDefaults.shape,
    containerColor: Color = MaterialTheme.colorScheme.onBackground,
    contentColor: Color = MaterialTheme.colorScheme.background,
    disabledContainerColor: Color = containerColor.copy(alpha = 0.12f),
    disabledContentColor: Color = contentColor.copy(alpha = 0.38f),
    leadingIcon: (@Composable (() -> Unit))? = null,
    trailingIcon: (@Composable (() -> Unit))? = null,
    content: (@Composable RowScope.() -> Unit)? = null,
) {
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
            modifier = modifier.defaultMinSize(minHeight = AlohomoraButtonDefaults.minHeight(size)),
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = disabledContainerColor,
                disabledContentColor = disabledContentColor,
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
    shape: Shape = AlohomoraButtonDefaults.shape,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    disabledContentColor: Color = contentColor.copy(alpha = 0.38f),
    borderColor: Color = MaterialTheme.colorScheme.outline,
    borderWidth: Dp = 1.dp,
    leadingIcon: (@Composable (() -> Unit))? = null,
    trailingIcon: (@Composable (() -> Unit))? = null,
    content: (@Composable RowScope.() -> Unit)? = null,
) {
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
            modifier = modifier.defaultMinSize(minHeight = AlohomoraButtonDefaults.minHeight(size)),
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = contentColor,
                disabledContentColor = disabledContentColor,
            ),
            border = BorderStroke(
                borderWidth,
                if (enabled) borderColor else borderColor.copy(alpha = 0.12f),
            ),
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
    shape: Shape = AlohomoraButtonDefaults.shape,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    disabledContentColor: Color = contentColor.copy(alpha = 0.38f),
    leadingIcon: (@Composable (() -> Unit))? = null,
    trailingIcon: (@Composable (() -> Unit))? = null,
    content: (@Composable RowScope.() -> Unit)? = null,
) {
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
            modifier = modifier.defaultMinSize(minHeight = AlohomoraButtonDefaults.minHeight(size)),
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.textButtonColors(
                contentColor = contentColor,
                disabledContentColor = disabledContentColor,
            ),
            contentPadding = contentPadding,
            content = contentSlot,
        )
    }
}

@Composable
fun AlohomoraFilledButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: AlohomoraButtonSize = AlohomoraButtonSize.MEDIUM,
    shape: Shape = AlohomoraButtonDefaults.shape,
    containerColor: Color = MaterialTheme.colorScheme.onBackground,
    contentColor: Color = MaterialTheme.colorScheme.background,
    disabledContainerColor: Color = containerColor.copy(alpha = 0.12f),
    disabledContentColor: Color = contentColor.copy(alpha = 0.38f),
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = AlohomoraButtonDefaults.minHeight(size)),
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
        ),
        contentPadding = AlohomoraButtonDefaults.contentPadding(size),
        content = content,
    )
}

@Composable
fun AlohomoraOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: AlohomoraButtonSize = AlohomoraButtonSize.MEDIUM,
    shape: Shape = AlohomoraButtonDefaults.shape,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    disabledContentColor: Color = contentColor.copy(alpha = 0.38f),
    borderColor: Color = MaterialTheme.colorScheme.outline,
    borderWidth: Dp = 1.dp,
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = AlohomoraButtonDefaults.minHeight(size)),
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = contentColor,
            disabledContentColor = disabledContentColor,
        ),
        border = BorderStroke(
            borderWidth,
            if (enabled) borderColor else borderColor.copy(alpha = 0.12f),
        ),
        contentPadding = AlohomoraButtonDefaults.contentPadding(size),
        content = content,
    )
}

@Composable
fun AlohomoraTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: AlohomoraButtonSize = AlohomoraButtonSize.MEDIUM,
    shape: Shape = AlohomoraButtonDefaults.shape,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    disabledContentColor: Color = contentColor.copy(alpha = 0.38f),
    content: @Composable RowScope.() -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = AlohomoraButtonDefaults.minHeight(size)),
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.textButtonColors(
            contentColor = contentColor,
            disabledContentColor = disabledContentColor,
        ),
        contentPadding = AlohomoraButtonDefaults.contentPadding(size),
        content = content,
    )
}

@Composable
fun AlohomoraIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: AlohomoraIconButtonStyle = AlohomoraIconButtonStyle.DEFAULT,
    shape: Shape = MaterialTheme.shapes.medium,
    content: @Composable () -> Unit,
) {
    when (style) {
        AlohomoraIconButtonStyle.DEFAULT -> IconButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            content = content,
            shape = shape,
        )
        AlohomoraIconButtonStyle.FILLED -> FilledIconButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.onBackground,
                contentColor = MaterialTheme.colorScheme.background,
            ),
            shape = shape,
            content = content,
        )
        AlohomoraIconButtonStyle.OUTLINED -> OutlinedIconButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = IconButtonDefaults.outlinedIconButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            shape = shape,
            content = content,
        )
        AlohomoraIconButtonStyle.TONAL -> FilledTonalIconButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
            shape = shape,
            content = content,
        )
    }
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
                start = if (leadingIcon != null) AlohomoraButtonDefaults.iconSpacing else 0.dp,
                end = if (trailingIcon != null) AlohomoraButtonDefaults.iconSpacing else 0.dp,
            ),
        )
        if (trailingIcon != null) {
            trailingIcon()
        }
    }
    button(contentPadding, baseContent)
}
