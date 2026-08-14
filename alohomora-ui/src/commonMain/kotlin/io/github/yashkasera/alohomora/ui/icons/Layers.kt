package io.github.yashkasera.alohomora.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("UnusedReceiverParameter")
val Icons.Layers: ImageVector
    get() {
        if (_layers != null) return _layers!!

        _layers = ImageVector.Builder(
            name = "layers",
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
                moveToRelative(12.83f, 2.18f)
                arcToRelative(2f, 2f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -1.66f,
                    dy1 = 0f
                )
                lineTo(2.6f, 6.08f)
                arcToRelative(1f, 1f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 0f,
                    dy1 = 1.83f
                )
                lineToRelative(8.58f, 3.91f)
                arcToRelative(2f, 2f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 1.66f,
                    dy1 = 0f
                )
                lineToRelative(8.58f, -3.9f)
                arcToRelative(1f, 1f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 0f,
                    dy1 = -1.83f
                )
                close()
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveToRelative(2f, 12.87f)
                lineToRelative(9.17f, 4.18f)
                arcToRelative(2f, 2f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 1.66f,
                    dy1 = 0f
                )
                lineTo(22f, 12.87f)
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveToRelative(2f, 17.87f)
                lineToRelative(9.17f, 4.18f)
                arcToRelative(2f, 2f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 1.66f,
                    dy1 = 0f
                )
                lineTo(22f, 17.87f)
            }
        }.build()

        return _layers!!
    }

@Suppress("ObjectPropertyName")
private var _layers: ImageVector? = null
