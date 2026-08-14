package io.github.yashkasera.alohomora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import io.github.yashkasera.alohomora.ui.theme.dimens
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * A lightweight, reusable code block component for displaying text and JSON content.
 *
 * Features:
 * - `surfaceContainerLow` background with `outlineVariant` border
 * - `typography.bodySmall` (JetBrains Mono, 12sp) with 18sp line height for code readability
 * - Optional left accent border for error highlighting
 * - Optional vertical scrolling
 * - Optional JSON prettification
 *
 * For advanced JSON viewing with line numbers, search, and expand/collapse,
 * use [AlohomoraJsonViewer] instead.
 *
 * @param content The text or JSON content to display
 * @param modifier Modifier for the component
 * @param isScrollable Whether the content should be vertically scrollable
 * @param accentBorder Whether to show a left accent border (useful for errors/exceptions)
 * @param accentColor The color of the accent border (defaults to error color)
 * @param jsonPrettify Whether to auto-format the content as pretty-printed JSON
 */
@Composable
fun AlohomoraCodeBlock(
    content: String,
    modifier: Modifier = Modifier,
    isScrollable: Boolean = true,
    accentBorder: Boolean = false,
    accentColor: Color = MaterialTheme.colorScheme.error,
    jsonPrettify: Boolean = false,
) {
    val displayContent = remember(content, jsonPrettify) {
        if (jsonPrettify) prettifyJson(content) else content
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small),
    ) {
        // Left accent border for errors/exceptions
        if (accentBorder) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 4.dp)
                    .width(3.dp)
                    .height(24.dp)
                    .background(accentColor),
            )

        }

        val textModifier = if (isScrollable) {
            Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.dimens.margin.lg)
                .padding(start = if (accentBorder) 20.dp else 0.dp)
                .verticalScroll(rememberScrollState())
        } else {
            Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.dimens.margin.lg)
                .padding(start = if (accentBorder) 20.dp else 0.dp)
        }

        SelectionContainer {
            BasicText(
                text = displayContent,
                style = MaterialTheme.typography.bodySmall.copy(
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = textModifier,
            )
        }
    }
}

private fun prettifyJson(content: String): String {
    return try {
        val json = Json { prettyPrint = true; prettyPrintIndent = "  " }
        val element = json.decodeFromString(JsonElement.serializer(), content)
        json.encodeToString(JsonElement.serializer(), element)
    } catch (e: Exception) {
        try {
            manualPrettifyJson(content)
        } catch (e: Exception) {
            content
        }
    }
}

/**
 * Manual JSON prettification for cases where kotlinx.serialization fails.
 * Handles basic JSON formatting with indentation.
 */
private fun manualPrettifyJson(json: String): String {
    val result = StringBuilder()
    var indent = 0
    var inString = false

    json.forEach { char ->
        when {
            char == '"' && result.lastOrNull() != '\\' -> {
                inString = !inString
                result.append(char)
            }

            !inString && (char == '{' || char == '[') -> {
                result.append(char)
                result.append('\n')
                indent++
                result.append("  ".repeat(indent))
            }

            !inString && (char == '}' || char == ']') -> {
                result.append('\n')
                indent--
                result.append("  ".repeat(indent))
                result.append(char)
            }

            !inString && char == ',' -> {
                result.append(char)
                result.append('\n')
                result.append("  ".repeat(indent))
            }

            !inString && char == ':' -> {
                result.append(char)
                result.append(' ')
            }

            char == ' ' && !inString -> { }

            else -> result.append(char)
        }
    }

    return result.toString()
}

@Preview
@Composable
private fun AlohomoraCodeBlockPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            AlohomoraCodeBlock(
                content = """{"id":42,"status":"ok"}""",
                modifier = Modifier.padding(16.dp),
                isScrollable = false,
                jsonPrettify = true,
            )
        }
    }
}
