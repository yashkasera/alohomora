package io.github.yashkasera.alohomora.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("UnusedReceiverParameter")
val Icons.EyeOff: ImageVector
    get() {
        if (_eyeOff != null) return _eyeOff!!

        _eyeOff = ImageVector.Builder(
            name = "eyeOff",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(9.88f, 9.88f)
                arcTo(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = false, x1 = 12f, y1 = 15f)
                arcTo(3f, 3f, 0f,
                    isMoreThanHalf = true,
                    isPositiveArc = false,
                    x1 = 9.88f,
                    y1 = 9.88f
                )
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(10.73f, 5.08f)
                arcTo(10.66f, 10.66f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    x1 = 12f,
                    y1 = 5f
                )
                quadTo(19.5f, 5f, 23f, 12f)
                quadTo(22.04f, 13.82f, 20.64f, 15.36f)
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(17.94f, 17.94f)
                arcTo(10.07f, 10.07f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    x1 = 12f,
                    y1 = 19f
                )
                quadTo(4.5f, 19f, 1f, 12f)
                quadTo(2.73f, 8.39f, 6f, 6.27f)
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(1f, 1f)
                lineTo(23f, 23f)
            }
        }.build()

        return _eyeOff!!
    }

@Suppress("ObjectPropertyName")
private var _eyeOff: ImageVector? = null
