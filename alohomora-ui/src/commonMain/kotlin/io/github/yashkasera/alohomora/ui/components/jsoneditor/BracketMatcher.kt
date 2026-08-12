package io.github.yashkasera.alohomora.ui.components.jsoneditor

internal fun findMatchingBracket(text: String, cursorPos: Int): Pair<Int, Int>? {
    if (cursorPos < 0 || cursorPos >= text.length) return null
    val c = text[cursorPos]
    return when (c) {
        '{' -> findForward(text, cursorPos, '{', '}')
        '[' -> findForward(text, cursorPos, '[', ']')
        '}' -> findBackward(text, cursorPos, '}', '{')
        ']' -> findBackward(text, cursorPos, ']', '[')
        else -> {
            if (cursorPos > 0) {
                val prev = text[cursorPos - 1]
                when (prev) {
                    '{' -> findForward(text, cursorPos - 1, '{', '}')
                    '[' -> findForward(text, cursorPos - 1, '[', ']')
                    '}' -> findBackward(text, cursorPos - 1, '}', '{')
                    ']' -> findBackward(text, cursorPos - 1, ']', '[')
                    else -> null
                }
            } else null
        }
    }
}

private fun findForward(text: String, start: Int, open: Char, close: Char): Pair<Int, Int>? {
    var depth = 0
    var inString = false
    for (i in start until text.length) {
        val c = text[i]
        if (c == '"' && (i == 0 || text[i - 1] != '\\')) {
            inString = !inString
            continue
        }
        if (inString) continue
        if (c == open) depth++
        if (c == close) {
            depth--
            if (depth == 0) return start to i
        }
    }
    return null
}

private fun findBackward(text: String, start: Int, close: Char, open: Char): Pair<Int, Int>? {
    var depth = 0
    var inString = false
    for (i in start downTo 0) {
        val c = text[i]
        if (c == '"' && (i == 0 || text[i - 1] != '\\')) {
            inString = !inString
            continue
        }
        if (inString) continue
        if (c == close) depth++
        if (c == open) {
            depth--
            if (depth == 0) return i to start
        }
    }
    return null
}
