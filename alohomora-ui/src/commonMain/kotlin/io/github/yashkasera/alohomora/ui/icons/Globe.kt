package io.github.yashkasera.alohomora.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("UnusedReceiverParameter")
val Icons.Globe: ImageVector
    get() {
        if (_globe != null) return _globe!!

        _globe = ImageVector.Builder(
            name = "globe",
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
                moveTo(2f, 12f)
                arcToRelative(10f, 10f, 0f,
                    isMoreThanHalf = true,
                    isPositiveArc = false,
                    dx1 = 20f,
                    dy1 = 0f
                )
                arcToRelative(10f, 10f, 0f,
                    isMoreThanHalf = true,
                    isPositiveArc = false,
                    dx1 = -20f,
                    dy1 = 0f
                )
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 2f)
                arcToRelative(14.5f, 14.5f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 0f,
                    dy1 = 20f
                )
                arcToRelative(14.5f, 14.5f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 0f,
                    dy1 = -20f
                )
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(2f, 12f)
                horizontalLineToRelative(20f)
            }
        }.build()

        return _globe!!
    }

@Suppress("ObjectPropertyName")
private var _globe: ImageVector? = null
