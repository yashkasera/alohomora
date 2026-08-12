package io.github.yashkasera.alohomora.ui.components.jsoneditor

enum class TokenKind {
    KEY,
    STRING,
    NUMBER,
    BOOLEAN,
    NULL,
    BRACE,
    BRACKET,
    COLON,
    COMMA,
    TEMPLATE,
    WHITESPACE,
}

data class JsonToken(val range: IntRange, val kind: TokenKind)

internal fun tokenize(text: String): List<JsonToken> {
    val tokens = mutableListOf<JsonToken>()
    var i = 0
    val len = text.length
    var expectKey = true

    while (i < len) {
        val c = text[i]
        when {
            c.isWhitespace() -> {
                val start = i
                while (i < len && text[i].isWhitespace()) i++
                tokens.add(JsonToken(start until i, TokenKind.WHITESPACE))
            }

            c == '{' && i + 1 < len && text[i + 1] == '{' -> {
                val start = i
                i = scanTemplate(text, i)
                tokens.add(JsonToken(start until i, TokenKind.TEMPLATE))
            }

            c == '{' -> {
                tokens.add(JsonToken(i..i, TokenKind.BRACE))
                expectKey = true
                i++
            }

            c == '}' -> {
                tokens.add(JsonToken(i..i, TokenKind.BRACE))
                expectKey = false
                i++
            }

            c == '[' -> {
                tokens.add(JsonToken(i..i, TokenKind.BRACKET))
                expectKey = false
                i++
            }

            c == ']' -> {
                tokens.add(JsonToken(i..i, TokenKind.BRACKET))
                expectKey = false
                i++
            }

            c == ':' -> {
                tokens.add(JsonToken(i..i, TokenKind.COLON))
                expectKey = false
                i++
            }

            c == ',' -> {
                tokens.add(JsonToken(i..i, TokenKind.COMMA))
                expectKey = isInsideObject(text, tokens)
                i++
            }

            c == '"' -> {
                val start = i
                i++
                while (i < len) {
                    if (text[i] == '\\' && i + 1 < len) {
                        i += 2
                        continue
                    }
                    if (text[i] == '"') {
                        i++
                        break
                    }
                    i++
                }
                val kind = if (expectKey) TokenKind.KEY else TokenKind.STRING
                tokens.add(JsonToken(start until i, kind))
                if (expectKey) expectKey = false
            }

            c == 't' || c == 'f' -> {
                val start = i
                val word = if (c == 't') "true" else "false"
                if (text.regionMatches(i, word, 0, word.length)) {
                    i += word.length
                    tokens.add(JsonToken(start until i, TokenKind.BOOLEAN))
                } else {
                    i++
                }
            }

            c == 'n' -> {
                val start = i
                if (text.regionMatches(i, "null", 0, 4)) {
                    i += 4
                    tokens.add(JsonToken(start until i, TokenKind.NULL))
                } else {
                    i++
                }
            }

            c == '-' || c.isDigit() -> {
                val start = i
                if (c == '-') i++
                while (i < len && text[i].isDigit()) i++
                if (i < len && text[i] == '.') {
                    i++
                    while (i < len && text[i].isDigit()) i++
                }
                if (i < len && (text[i] == 'e' || text[i] == 'E')) {
                    i++
                    if (i < len && (text[i] == '+' || text[i] == '-')) i++
                    while (i < len && text[i].isDigit()) i++
                }
                tokens.add(JsonToken(start until i, TokenKind.NUMBER))
            }

            else -> i++
        }
    }
    return tokens
}

private fun scanTemplate(text: String, start: Int): Int {
    var i = start + 2
    while (i < text.length) {
        if (text[i] == '}' && i + 1 < text.length && text[i + 1] == '}') return i + 2
        i++
    }
    return i
}

private fun isInsideObject(text: String, tokens: List<JsonToken>): Boolean {
    var depth = 0
    for (j in tokens.indices.reversed()) {
        val token = tokens[j]
        val ch = text[token.range.first]
        when {
            (ch == '}' || ch == ']') && (token.kind == TokenKind.BRACE || token.kind == TokenKind.BRACKET) -> depth++
            ch == '{' && token.kind == TokenKind.BRACE -> {
                if (depth == 0) return true
                depth--
            }
            ch == '[' && token.kind == TokenKind.BRACKET -> {
                if (depth == 0) return false
                depth--
            }
        }
    }
    return false
}
