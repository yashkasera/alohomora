package io.github.yashkasera.alohomora.ui.components.jsoneditor

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

@Stable
class JsonEditorState(
    initialText: String = "",
    private val onTextChange: ((String) -> Unit)? = null,
) {
    var textFieldValue by mutableStateOf(TextFieldValue(initialText))
        private set

    val text: String get() = textFieldValue.text

    var validationError by mutableStateOf<String?>(null)
        private set

    var isValidJson by mutableStateOf(validate(initialText))
        private set

    val lineCount: Int get() = text.count { it == '\n' } + 1

    fun onValueChange(value: TextFieldValue) {
        val oldText = textFieldValue.text
        val newText = value.text
        val cursor = value.selection.start

        if (newText.length == oldText.length + 1 && value.selection.collapsed && cursor > 0) {
            val inserted = newText[cursor - 1]
            val oldCursor = textFieldValue.selection.start

            if (inserted in CLOSERS && oldCursor < oldText.length && oldText[oldCursor] == inserted) {
                textFieldValue = TextFieldValue(oldText, TextRange(oldCursor + 1))
                isValidJson = validate(oldText)
                onTextChange?.invoke(oldText)
                return
            }

            val closer = BRACKET_PAIRS[inserted]
            if (closer != null) {
                val withCloser = newText.substring(0, cursor) + closer + newText.substring(cursor)
                textFieldValue = TextFieldValue(withCloser, TextRange(cursor))
                isValidJson = validate(withCloser)
                onTextChange?.invoke(withCloser)
                return
            }
        }

        textFieldValue = value
        isValidJson = validate(value.text)
        onTextChange?.invoke(value.text)
    }

    fun setText(newText: String) {
        textFieldValue = TextFieldValue(newText, TextRange(newText.length))
        isValidJson = validate(newText)
        onTextChange?.invoke(newText)
    }

    fun format() {
        val formatted = try {
            val json = Json { prettyPrint = true; prettyPrintIndent = "  " }
            val element = json.decodeFromString(JsonElement.serializer(), text)
            json.encodeToString(JsonElement.serializer(), element)
        } catch (_: Exception) {
            return
        }
        val cursor = formatted.length.coerceAtMost(textFieldValue.selection.start)
        textFieldValue = TextFieldValue(formatted, TextRange(cursor))
        isValidJson = true
        validationError = null
        onTextChange?.invoke(formatted)
    }

    fun minify() {
        val minified = try {
            val json = Json { prettyPrint = false }
            val element = json.decodeFromString(JsonElement.serializer(), text)
            json.encodeToString(JsonElement.serializer(), element)
        } catch (_: Exception) {
            return
        }
        textFieldValue = TextFieldValue(minified, TextRange(minified.length))
        isValidJson = true
        validationError = null
        onTextChange?.invoke(minified)
    }

    fun insertNewline() {
        val pos = textFieldValue.selection.start
        val end = textFieldValue.selection.end
        val before = text.substring(0, pos)
        val after = text.substring(end)

        val lineStart = before.lastIndexOf('\n') + 1
        val currentLine = before.substring(lineStart)
        val baseIndent = currentLine.takeWhile { it == ' ' || it == '\t' }

        val charBefore = before.lastOrNull()
        val charAfter = after.firstOrNull()
        val openedBracket = charBefore == '{' || charBefore == '['
        val closerFollows = openedBracket && (charAfter == '}' || charAfter == ']')

        val newText: String
        val newPos: Int
        if (closerFollows) {
            val innerIndent = baseIndent + INDENT
            newText = before + "\n" + innerIndent + "\n" + baseIndent + after
            newPos = pos + 1 + innerIndent.length
        } else if (openedBracket) {
            val innerIndent = baseIndent + INDENT
            newText = before + "\n" + innerIndent + after
            newPos = pos + 1 + innerIndent.length
        } else {
            newText = before + "\n" + baseIndent + after
            newPos = pos + 1 + baseIndent.length
        }

        textFieldValue = TextFieldValue(newText, TextRange(newPos))
        isValidJson = validate(newText)
        onTextChange?.invoke(newText)
    }

    fun insertTab() {
        val pos = textFieldValue.selection.start
        val end = textFieldValue.selection.end
        if (pos != end) {
            val newText = text.substring(0, pos) + "\t" + text.substring(end)
            val newPos = pos + 1
            textFieldValue = TextFieldValue(newText, TextRange(newPos))
            isValidJson = validate(newText)
            onTextChange?.invoke(newText)
        } else {
            val newText = text.substring(0, pos) + "\t" + text.substring(pos)
            val newPos = pos + 1
            textFieldValue = TextFieldValue(newText, TextRange(newPos))
            isValidJson = validate(newText)
            onTextChange?.invoke(newText)
        }
    }

    fun insertTemplate(generator: String) {
        val placeholder = "{{$generator}}"
        val pos = textFieldValue.selection.start
        val newText = text.substring(0, pos) + placeholder + text.substring(pos)
        val newPos = pos + placeholder.length
        textFieldValue = TextFieldValue(newText, TextRange(newPos))
        isValidJson = validate(newText)
        onTextChange?.invoke(newText)
    }

    companion object {
        private const val INDENT = "  "
        private val BRACKET_PAIRS = mapOf('{' to '}', '[' to ']', '(' to ')', '"' to '"')
        private val CLOSERS = setOf('}', ']', ')', '"')
    }

    private fun validate(value: String): Boolean {
        if (value.isBlank()) {
            validationError = null
            return true
        }
        val stripped = stripTemplates(value)
        return try {
            Json.parseToJsonElement(stripped)
            validationError = null
            true
        } catch (e: Exception) {
            validationError = e.message?.lineSequence()?.firstOrNull()
            false
        }
    }
}

private fun stripTemplates(text: String): String {
    if (!text.contains("{{")) return text
    val sb = StringBuilder(text.length)
    var i = 0
    while (i < text.length) {
        if (text[i] == '{' && i + 1 < text.length && text[i + 1] == '{') {
            sb.append("\"__tpl__\"")
            i += 2
            while (i < text.length) {
                if (text[i] == '}' && i + 1 < text.length && text[i + 1] == '}') {
                    i += 2
                    break
                }
                i++
            }
        } else {
            sb.append(text[i])
            i++
        }
    }
    return sb.toString()
}
