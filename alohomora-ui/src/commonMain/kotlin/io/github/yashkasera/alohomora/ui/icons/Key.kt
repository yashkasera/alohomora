package io.github.yashkasera.alohomora.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("UnusedReceiverParameter")
val Icons.Key: ImageVector
    get() {
        if (_key != null) return _key!!

        _key = ImageVector.Builder(
            name = "key",
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
                moveToRelative(15.5f, 7.5f)
                lineToRelative(2.3f, 2.3f)
                arcToRelative(1f, 1f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 1.4f,
                    dy1 = 0f
                )
                lineToRelative(2.1f, -2.1f)
                arcToRelative(1f, 1f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 0f,
                    dy1 = -1.4f
                )
                lineTo(19f, 4f)
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveToRelative(21f, 2f)
                lineToRelative(-9.6f, 9.6f)
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(2.0f, 15.5f)
                arcToRelative(5.5f, 5.5f, 0f,
                    isMoreThanHalf = true,
                    isPositiveArc = false,
                    dx1 = 11.0f,
                    dy1 = 0f
                )
                arcToRelative(5.5f, 5.5f, 0f,
                    isMoreThanHalf = true,
                    isPositiveArc = false,
                    dx1 = -11.0f,
                    dy1 = 0f
                )
            }
        }.build()

        return _key!!
    }

@Suppress("ObjectPropertyName")
private var _key: ImageVector? = null
