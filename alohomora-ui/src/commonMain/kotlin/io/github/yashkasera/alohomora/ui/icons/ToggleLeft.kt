package io.github.yashkasera.alohomora.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("UnusedReceiverParameter")
val Icons.ToggleLeft: ImageVector
    get() {
        if (_toggleLeft != null) return _toggleLeft!!

        _toggleLeft = ImageVector.Builder(
            name = "toggleLeft",
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
                // rect x=1 y=5 width=22 height=14 rx=7 ry=7
                moveTo(8f, 5f)
                horizontalLineTo(16f)
                arcToRelative(7f, 7f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 7f,
                    dy1 = 7f
                )
                arcToRelative(7f, 7f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -7f,
                    dy1 = 7f
                )
                horizontalLineTo(8f)
                arcToRelative(7f, 7f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -7f,
                    dy1 = -7f
                )
                arcToRelative(7f, 7f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 7f,
                    dy1 = -7f
                )
                close()
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // circle cx=8 cy=12 r=3
                moveTo(8f, 9f)
                arcToRelative(3f, 3f, 0f,
                    isMoreThanHalf = true,
                    isPositiveArc = false,
                    dx1 = 0f,
                    dy1 = 6f
                )
                arcToRelative(3f, 3f, 0f,
                    isMoreThanHalf = true,
                    isPositiveArc = false,
                    dx1 = 0f,
                    dy1 = -6f
                )
            }
        }.build()

        return _toggleLeft!!
    }

@Suppress("ObjectPropertyName")
private var _toggleLeft: ImageVector? = null
