package io.github.yashkasera.alohomora.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.ZoomIn: ImageVector
    get() {
        if (_zoomIn != null) return _zoomIn!!
        _zoomIn = ImageVector.Builder(
            name = "ZoomIn",
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
                arcToRelative(8f, 8f, 0f, true, true, 16f, 0f)
                arcToRelative(8f, 8f, 0f, true, true, -16f, 0f)
                // Search line
                moveTo(21f, 21f)
                lineToRelative(-4.35f, -4.35f)
                // Plus horizontal
                moveTo(8f, 11f)
                horizontalLineToRelative(6f)
                // Plus vertical
                moveTo(11f, 8f)
                verticalLineToRelative(6f)
            }
        }.build()
        return _zoomIn!!
    }

private var _zoomIn: ImageVector? = null
