package io.github.yashkasera.alohomora.ui.components.jsoneditor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import io.github.yashkasera.alohomora.ui.theme.JsonColors

data class JsonEditorColors(
    val key: Color = JsonColors.key,
    val string: Color = JsonColors.string,
    val number: Color = JsonColors.number,
    val boolean: Color = JsonColors.boolean,
    val nullValue: Color = JsonColors.nullValue,
    val bracket: Color = JsonColors.bracket,
    val template: Color = JsonColors.template,
    val templateBackground: Color = JsonColors.templateBackground,
) {
    companion object {
        val Light = JsonEditorColors()
        val Dark = JsonEditorColors(
            key = JsonColors.keyDark,
            string = JsonColors.stringDark,
            number = JsonColors.numberDark,
            boolean = JsonColors.booleanDark,
            nullValue = JsonColors.nullValueDark,
            bracket = JsonColors.bracketDark,
            template = JsonColors.templateDark,
            templateBackground = JsonColors.templateBackgroundDark,
        )
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
