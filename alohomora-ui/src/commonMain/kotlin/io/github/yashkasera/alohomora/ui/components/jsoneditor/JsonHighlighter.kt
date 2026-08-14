package io.github.yashkasera.alohomora.ui.components.jsoneditor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import io.github.yashkasera.alohomora.ui.theme.AlohomoraColorTheme

data class JsonEditorColors(
    val key: Color,
    val string: Color,
    val number: Color,
    val boolean: Color,
    val nullValue: Color,
    val bracket: Color,
    val template: Color,
    val templateBackground: Color,
) {
    companion object {
        fun forTheme(theme: AlohomoraColorTheme): JsonEditorColors {
            val outline = theme.materialColorScheme.outline
            return JsonEditorColors(
                key = theme.accent,
                string = theme.warning,
                number = theme.success,
                boolean = theme.fatal,
                nullValue = outline,
                bracket = outline,
                template = theme.info,
                templateBackground = theme.info.copy(alpha = 0.1f),
            )
        }
    }
}

internal fun highlight(
    text: String,
    tokens: List<JsonToken>,
    colors: JsonEditorColors,
): AnnotatedString = buildAnnotatedString {
    append(text)
    for (token in tokens) {
        val style = when (token.kind) {
            TokenKind.KEY -> SpanStyle(color = colors.key)
            TokenKind.STRING -> SpanStyle(color = colors.string)
            TokenKind.NUMBER -> SpanStyle(color = colors.number)
            TokenKind.BOOLEAN -> SpanStyle(color = colors.boolean, fontStyle = FontStyle.Italic)
            TokenKind.NULL -> SpanStyle(color = colors.nullValue, fontStyle = FontStyle.Italic)
            TokenKind.BRACE, TokenKind.BRACKET -> SpanStyle(color = colors.bracket)
            TokenKind.TEMPLATE -> SpanStyle(
                color = colors.template,
                background = colors.templateBackground,
            )

            TokenKind.COLON, TokenKind.COMMA, TokenKind.WHITESPACE -> continue
        }
        addStyle(style, token.range.first, token.range.last + 1)
    }
}
