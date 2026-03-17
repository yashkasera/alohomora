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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Search
import io.github.yashkasera.alohomora.ui.icons.X

/**
 * A compact custom text field built on BasicTextField for full control.
 *
 * @param value The current text value
 * @param onValueChange Callback when the text changes
 * @param modifier Modifier for the text field
 * @param placeholder Placeholder text to display when empty
 * @param leadingIcon Optional leading icon composable
 * @param trailingIcon Optional trailing icon composable
 * @param singleLine Whether the field is single line (default: true)
 * @param enabled Whether the field is enabled
 * @param readOnly Whether the field is read-only
 * @param keyboardOptions Keyboard configuration options
 * @param keyboardActions Keyboard action handlers
 * @param visualTransformation Visual transformation for the text
 * @param shape The shape of the text field
 * @param textStyle The text style for the input text
 * @param containerColor Background color of the text field
 * @param borderColor Border color when unfocused
 * @param focusedBorderColor Border color when focused
 * @param cursorColor Cursor color
 */
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

    Column{
        label?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
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
                        .border(1.dp, currentBorderColor, shape)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (leadingIcon != null) {
                        Box(modifier = Modifier.size(16.dp)) {
                            leadingIcon()
                        }
                        Spacer(modifier = Modifier.width(8.dp))
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
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.size(20.dp)) {
                            trailingIcon()
                        }
                    }
                }
            },
        )
    }
}

/**
 * A compact search text field with built-in search icon and clear button.
 *
 * @param query The current search query
 * @param onQueryChange Callback when the query changes
 * @param modifier Modifier for the text field
 * @param placeholder Placeholder text (default: "Search...")
 * @param onSearch Callback when search action is triggered (e.g., keyboard search button)
 * @param onClear Callback when clear button is clicked (defaults to clearing the query)
 * @param enabled Whether the field is enabled
 */
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
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(
                    onClick = {
                        if (onClear != null) {
                            onClear()
                        } else {
                            onQueryChange("")
                        }
                    },
                    modifier = Modifier.size(20.dp),
                ) {
                    Icon(
                        imageVector = Icons.X,
                        contentDescription = "Clear search",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else null,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search,
        ),
        keyboardActions = KeyboardActions(
            onSearch = {
                onSearch?.invoke()
            },
        ),
    )
}
