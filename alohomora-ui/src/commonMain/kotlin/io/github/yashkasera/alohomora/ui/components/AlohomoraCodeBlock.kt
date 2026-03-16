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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.yashkasera.alohomora.ui.theme.CanvasDarkGray
import io.github.yashkasera.alohomora.ui.theme.CanvasLightGray
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * A lightweight, reusable code block component for displaying text and JSON content.
 *
 * Features:
 * - Fixed light gray background (CanvasLightGray)
 * - Fixed 1dp border (CanvasLightGray)
 * - Fixed font styling (12.sp, monospace, 18.sp line height)
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
            .background(CanvasLightGray)
            .border(1.dp, CanvasLightGray),
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
                .padding(16.dp)
                .padding(start = if (accentBorder) 20.dp else 0.dp)
                .verticalScroll(rememberScrollState())
        } else {
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(start = if (accentBorder) 20.dp else 0.dp)
        }

        SelectionContainer {
            BasicText(
                text = displayContent,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = CanvasDarkGray,
                ),
                modifier = textModifier,
            )
        }
    }
}

/**
 * Attempts to prettify a JSON string. If the content is not valid JSON,
 * returns the original string unchanged.
 */
private fun prettifyJson(content: String): String {
    return try {
        // Try to parse and format as JSON
        val json = Json { prettyPrint = true; prettyPrintIndent = "  " }
        val element = json.decodeFromString(JsonElement.serializer(), content)
        json.encodeToString(JsonElement.serializer(), element)
    } catch (e: Exception) {
        // If parsing fails, try manual prettification for common JSON patterns
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

            char == ' ' && !inString -> {
                // Skip spaces outside strings
            }

            else -> result.append(char)
        }
    }

    return result.toString()
}
