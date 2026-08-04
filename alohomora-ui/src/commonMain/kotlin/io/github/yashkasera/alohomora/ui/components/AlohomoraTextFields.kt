package io.github.yashkasera.alohomora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Search
import io.github.yashkasera.alohomora.ui.icons.X
import io.github.yashkasera.alohomora.ui.theme.dimens

@Composable
fun AlohomoraTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    label: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    shape: Shape = MaterialTheme.shapes.small,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    focusedBorderColor: Color = MaterialTheme.colorScheme.outline,
    cursorColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val currentBorderColor = if (isFocused) focusedBorderColor else borderColor

    // The caret is remembered here; the text is not.
    //
    // `BasicTextField(value: String)` carries no selection, so it has to guess where the caret went
    // whenever the value it is handed differs from what it last emitted. Every caller drives this field
    // from a `StateFlow`, so the new value arrives a frame or a dispatcher hop late — and the guess lands
    // on index 0. The symptom is a caret that jumps left on every keystroke, typing a word out backwards.
    //
    // Keeping only the selection local means [value] stays the single source of truth for the text, so
    // there is no second copy to diverge from it, while the caret survives the round trip.
    var selection by remember { mutableStateOf(TextRange(value.length)) }
    // Coerced because the remembered caret can briefly outrun a lagging value, and a selection past the
    // end of the text throws.
    val fieldValue = TextFieldValue(text = value, selection = selection.coerceAtMostLength(value.length))

    Column(modifier = modifier) {
        label?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = MaterialTheme.dimens.margin.xs),
            )
        }
        BasicTextField(
            value = fieldValue,
            onValueChange = { updated ->
                selection = updated.selection
                // Only a text change is worth reporting; a bare caret move is nobody else's business.
                if (updated.text != value) onValueChange(updated.text)
            },
            enabled = enabled,
            readOnly = readOnly,
            singleLine = singleLine,
            textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            cursorBrush = SolidColor(cursorColor),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(shape)
                        .background(containerColor)
                        .border(MaterialTheme.dimens.stroke.small, currentBorderColor, shape)
                        .padding(horizontal = MaterialTheme.dimens.margin.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (leadingIcon != null) {
                        Box(modifier = Modifier.size(MaterialTheme.dimens.icon.md)) {
                            leadingIcon()
                        }
                        Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.sm))
                    }

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                        innerTextField()
                    }

                    if (trailingIcon != null) {
                        Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.sm))
                        Box(modifier = Modifier.size(MaterialTheme.dimens.icon.lg)) {
                            trailingIcon()
                        }
                    }
                }
            },
        )
    }
}

@Composable
fun AlohomoraSearchTextField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search...",
    onSearch: (() -> Unit)? = null,
    onClear: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    AlohomoraTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = placeholder,
        enabled = enabled,
        leadingIcon = {
            Icon(
                imageVector = Icons.Search,
                contentDescription = null,
                modifier = Modifier.size(MaterialTheme.dimens.icon.md),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(
                    onClick = {
                        if (onClear != null) onClear() else onQueryChange("")
                    },
                    modifier = Modifier.size(MaterialTheme.dimens.icon.lg),
                ) {
                    Icon(
                        imageVector = Icons.X,
                        contentDescription = "Clear search",
                        modifier = Modifier.size(MaterialTheme.dimens.icon.sm),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else null,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
    )
}

/**
 * Clamps a remembered caret to [length].
 *
 * The caret is remembered independently of the text, so while a hoisted value is catching up the caret can
 * point past the end of what is currently rendered — and `TextFieldValue` throws on an out-of-range
 * selection rather than clamping.
 */
private fun TextRange.coerceAtMostLength(length: Int): TextRange =
    if (start <= length && end <= length) {
        this
    } else {
        TextRange(start.coerceAtMost(length), end.coerceAtMost(length))
    }
