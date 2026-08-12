package io.github.yashkasera.alohomora.ui.components.jsoneditor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextButton
import io.github.yashkasera.alohomora.ui.icons.Check
import io.github.yashkasera.alohomora.ui.icons.CircleAlert
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.theme.LocalThemeIsDark
import io.github.yashkasera.alohomora.ui.theme.dimens

@Composable
fun JsonEditor(
    state: JsonEditorState,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    minLines: Int = 6,
    label: String? = null,
) {
    val isDark = LocalThemeIsDark.current.value
    val colors = remember(isDark) {
        if (isDark) JsonEditorColors.Dark else JsonEditorColors.Light
    }

    val codeStyle = MaterialTheme.typography.bodySmall.copy(
        lineHeight = 18.sp,
    )
    val minHeightDp = (codeStyle.lineHeight.value * minLines).dp

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xs),
    ) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val scrollState = rememberScrollState()
        val borderColor = if (state.text.isNotBlank() && !state.isValidJson) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.outlineVariant
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .defaultMinSize(minHeight = minHeightDp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .border(1.dp, borderColor, MaterialTheme.shapes.small),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = minHeightDp)
                    .verticalScroll(scrollState),
            ) {
                val lineCount = state.lineCount
                val dividerColor = MaterialTheme.colorScheme.outlineVariant
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .defaultMinSize(minHeight = minHeightDp)
                        .padding(vertical = MaterialTheme.dimens.margin.sm)
                        .drawBehind {
                            drawLine(
                                color = dividerColor,
                                start = Offset(size.width - 0.5f, 0f),
                                end = Offset(size.width - 0.5f, size.height),
                                strokeWidth = 1f,
                            )
                        },
                    contentAlignment = Alignment.TopEnd,
                ) {
                    Column(
                        modifier = Modifier.padding(end = MaterialTheme.dimens.margin.sm),
                    ) {
                        for (line in 1..lineCount) {
                            Text(
                                text = line.toString(),
                                style = codeStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            )
                        }
                    }
                }

                val tokens = remember(state.text) { tokenize(state.text) }
                val annotated = remember(state.text, colors) {
                    highlight(state.text, tokens, colors)
                }
                val bracketMatch = remember(state.text, state.textFieldValue.selection) {
                    findMatchingBracket(state.text, state.textFieldValue.selection.start)
                }
                val displayText = remember(annotated, bracketMatch) {
                    if (bracketMatch != null) {
                        buildAnnotatedString {
                            append(annotated)
                            val matchStyle = SpanStyle(
                                background = colors.bracket.copy(alpha = 0.25f),
                            )
                            addStyle(matchStyle, bracketMatch.first, bracketMatch.first + 1)
                            addStyle(matchStyle, bracketMatch.second, bracketMatch.second + 1)
                        }
                    } else {
                        annotated
                    }
                }

                BasicTextField(
                    value = TextFieldValue(
                        annotatedString = displayText,
                        selection = state.textFieldValue.selection,
                        composition = state.textFieldValue.composition,
                    ),
                    onValueChange = { newValue ->
                        state.onValueChange(
                            TextFieldValue(
                                text = newValue.text,
                                selection = newValue.selection,
                                composition = newValue.composition,
                            ),
                        )
                    },
                    readOnly = readOnly,
                    textStyle = codeStyle.merge(
                        TextStyle(color = MaterialTheme.colorScheme.onSurface),
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = minHeightDp)
                        .padding(MaterialTheme.dimens.margin.sm)
                        .onPreviewKeyEvent { event ->
                            if (!readOnly && event.type == KeyEventType.KeyDown) {
                                when (event.key) {
                                    Key.Tab -> { state.insertTab(); true }
                                    Key.Enter -> { state.insertNewline(); true }
                                    else -> false
                                }
                            } else {
                                false
                            }
                        },
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.text.isNotBlank()) {
                    if (state.isValidJson) {
                        Icon(
                            Icons.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(14.dp),
                        )
                        Text(
                            "Valid JSON",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Icon(
                            Icons.CircleAlert,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.width(14.dp),
                        )
                        Text(
                            state.validationError ?: "Invalid JSON",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1,
                        )
                    }
                }
            }

            if (!readOnly && state.text.isNotBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xs),
                ) {
                    AlohomoraTextButton(
                        text = "Format",
                        onClick = { state.format() },
                        enabled = state.isValidJson,
                    )
                    AlohomoraTextButton(
                        text = "Minify",
                        onClick = { state.minify() },
                        enabled = state.isValidJson,
                    )
                }
            }
        }
    }
}
