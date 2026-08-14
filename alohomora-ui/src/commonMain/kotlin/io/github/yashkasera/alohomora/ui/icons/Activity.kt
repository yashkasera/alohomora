package io.github.yashkasera.alohomora.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("UnusedReceiverParameter")
val Icons.Activity: ImageVector
    get() {
        if (_activity != null) return _activity!!

        _activity = ImageVector.Builder(
            name = "activity",
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
                moveTo(22f, 12f)
                horizontalLineToRelative(-2.48f)
                arcToRelative(2f, 2f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -1.93f,
                    dy1 = 1.46f
                )
                lineToRelative(-2.35f, 8.36f)
                arcToRelative(0.25f, 0.25f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -0.48f,
                    dy1 = 0f
                )
                lineTo(9.24f, 2.18f)
                arcToRelative(0.25f, 0.25f, 0f, isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -0.48f,
                    dy1 = 0f
                )
                lineToRelative(-2.35f, 8.36f)
                arcTo(2f, 2f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    x1 = 4.49f,
                    y1 = 12f
                )
                horizontalLineTo(2f)
            }
        }.build()

        return _activity!!
    }

@Suppress("ObjectPropertyName")
private var _activity: ImageVector? = null
