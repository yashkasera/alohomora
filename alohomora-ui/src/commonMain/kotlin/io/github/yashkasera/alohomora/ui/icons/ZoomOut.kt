package io.github.yashkasera.alohomora.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("UnusedReceiverParameter")
val Icons.ZoomOut: ImageVector
    get() {
        if (_zoomOut != null) return _zoomOut!!
        _zoomOut = ImageVector.Builder(
            name = "ZoomOut",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
                strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round,
            ) {
                // Circle
                moveTo(11f, 11f)
                moveToRelative(-8f, 0f)
                arcToRelative(8f, 8f, 0f,
                    isMoreThanHalf = true,
                    isPositiveArc = true,
                    dx1 = 16f,
                    dy1 = 0f
                )
                arcToRelative(8f, 8f, 0f,
                    isMoreThanHalf = true,
                    isPositiveArc = true,
                    dx1 = -16f,
                    dy1 = 0f
                )
                // Search line
                moveTo(21f, 21f)
                lineToRelative(-4.35f, -4.35f)
                // Minus horizontal
                moveTo(8f, 11f)
                horizontalLineToRelative(6f)
            }
        }.build()
        return _zoomOut!!
    }

@Suppress("ObjectPropertyName")
private var _zoomOut: ImageVector? = null
