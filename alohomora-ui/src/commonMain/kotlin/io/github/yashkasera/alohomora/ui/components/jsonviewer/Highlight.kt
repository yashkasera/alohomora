package io.github.yashkasera.alohomora.ui.components.jsonviewer

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

internal fun highlightText(
    text: String,
    query: String,
    normal: Color,
    highlight: Color = Color.Blue,
): AnnotatedString {

    if (query.isBlank()) {
        return AnnotatedString(text, SpanStyle(color = normal))
    }

    val lower = text.lowercase()
    val q = query.lowercase()

    val start = lower.indexOf(q)

    if (start == -1) {
        return AnnotatedString(text, SpanStyle(color = normal))
    }

    return buildAnnotatedString {

        append(text.substring(0, start))

        withStyle(
            SpanStyle(
                color = highlight,
                textDecoration = TextDecoration.Underline,
            ),
        ) {
            append(text.substring(start, start + q.length))
        }

        append(text.substring(start + q.length))
    }
}
