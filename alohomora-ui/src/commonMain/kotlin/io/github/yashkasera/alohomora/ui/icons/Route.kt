package io.github.yashkasera.alohomora.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("UnusedReceiverParameter")
val Icons.Route: ImageVector
    get() {
        if (_route != null) return _route!!

        _route = ImageVector.Builder(
            name = "route",
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
                moveTo(3f, 19f)
                arcToRelative(3f, 3f, 0f,
                    isMoreThanHalf = true,
                    isPositiveArc = false,
                    dx1 = 6f,
                    dy1 = 0f
                )
                arcToRelative(3f, 3f, 0f,
                    isMoreThanHalf = true,
                    isPositiveArc = false,
                    dx1 = -6f,
                    dy1 = 0f
                )
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(9f, 19f)
                horizontalLineToRelative(8.5f)
                arcToRelative(3.5f, 3.5f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 0f,
                    dy1 = -7f
                )
                horizontalLineToRelative(-11f)
                arcToRelative(3.5f, 3.5f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 0f,
                    dy1 = -7f
                )
                horizontalLineTo(15f)
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(15f, 5f)
                arcToRelative(3f, 3f, 0f,
                    isMoreThanHalf = true,
                    isPositiveArc = false,
                    dx1 = 6f,
                    dy1 = 0f
                )
                arcToRelative(3f, 3f, 0f,
                    isMoreThanHalf = true,
                    isPositiveArc = false,
                    dx1 = -6f,
                    dy1 = 0f
                )
            }
        }.build()

        return _route!!
    }

@Suppress("ObjectPropertyName")
private var _route: ImageVector? = null
