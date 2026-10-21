package io.github.yashkasera.alohomora.ui.utils

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import kotlin.math.sqrt

/**
 * Draws a rotated corner ribbon carrying [text] over the composable.
 *
 * @param color fill colour of the ribbon.
 * @param textColor colour of the ribbon text. Callers pass the role that contrasts with [color]
 *   (e.g. `onPrimary` for a `primary` ribbon) so the label stays legible across every theme and in
 *   both light and dark. Falls back to the style's own colour when left [Color.Unspecified].
 * @param style text style; defaults to the `labelSmall` typography token.
 */
fun Modifier.drawDiagonalLabel(
    text: String,
    color: Color,
    textColor: Color = Color.Unspecified,
    style: TextStyle? = null,
    labelTextRatio: Float = 2f,
) = composed(
    factory = {
        val resolvedStyle = (style ?: MaterialTheme.typography.labelSmall).let {
            if (textColor != Color.Unspecified) it.copy(color = textColor) else it
        }

        val textMeasurer = rememberTextMeasurer()
        val textLayoutResult: TextLayoutResult = remember {
            textMeasurer.measure(text = AnnotatedString(text), style = resolvedStyle)
        }


        Modifier
            .clipToBounds()
            .drawWithContent {
                val canvasWidth = size.width

                val textSize = textLayoutResult.size
                val textWidth = textSize.width
                val textHeight = textSize.height

                val rectWidth = textWidth * labelTextRatio
                val rectHeight = textHeight * 1.1f

                val rect = Rect(
                    offset = Offset(canvasWidth - rectWidth, 0f),
                    size = Size(rectWidth, rectHeight),
                )

                val sqrt = sqrt(rectWidth / 2f)
                val translatePos = sqrt * sqrt

                drawContent()
                withTransform(
                    {
                        rotate(
                            degrees = 45f,
                            pivot = Offset(
                                canvasWidth - rectWidth / 2,
                                translatePos,
                            ),
                        )
                    },
                ) {
                    drawRect(
                        color = color,
                        topLeft = rect.topLeft,
                        size = rect.size,
                    )
                    drawText(
                        textMeasurer = textMeasurer,
                        text = text,
                        style = resolvedStyle,
                        topLeft = Offset(
                            rect.left + (rectWidth - textWidth) / 2f,
                            rect.top + (rect.bottom - textHeight) / 2f,
                        ),
                    )
                }

            }
    },
)
