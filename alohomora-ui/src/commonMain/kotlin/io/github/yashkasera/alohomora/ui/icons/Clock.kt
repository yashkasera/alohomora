package io.github.yashkasera.alohomora.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("UnusedReceiverParameter")
val Icons.Clock: ImageVector
    get() {
        if (_clock != null) return _clock!!

        _clock = ImageVector.Builder(
            name = "clock",
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
                moveTo(12f, 6f)
                verticalLineToRelative(6f)
                lineToRelative(4f, 2f)
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(22f, 12f)
                arcTo(10f, 10f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    x1 = 12f,
                    y1 = 22f
                )
                arcTo(10f, 10f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 2f, y1 = 12f)
                arcTo(10f, 10f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    x1 = 22f,
                    y1 = 12f
                )
                close()
            }
        }.build()

        return _clock!!
    }

@Suppress("ObjectPropertyName")
private var _clock: ImageVector? = null

