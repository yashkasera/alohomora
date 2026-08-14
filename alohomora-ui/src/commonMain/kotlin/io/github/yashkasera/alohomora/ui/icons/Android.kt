
package io.github.yashkasera.alohomora.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("UnusedReceiverParameter")
val Icons.Android: ImageVector
    get() {
        if (_android != null) return _android!!

        _android = ImageVector.Builder(
            name = "Android",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            path(
                fill = SolidColor(Color.Black),
            ) {
                moveTo(2.76f, 3.061f)
                arcToRelative(0.5f, 0.5f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 0.679f,
                    dy1 = 0.2f
                )
                lineToRelative(1.283f, 2.352f)
                arcTo(8.9f, 8.9f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    x1 = 8f,
                    y1 = 5f
                )
                arcToRelative(8.9f, 8.9f, 0f, isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 3.278f,
                    dy1 = 0.613f
                )
                lineToRelative(1.283f, -2.352f)
                arcToRelative(0.5f, 0.5f, 0f, isMoreThanHalf = true,
                    isPositiveArc = true,
                    dx1 = 0.878f,
                    dy1 = 0.478f
                )
                lineToRelative(-1.252f, 2.295f)
                curveTo(14.475f, 7.266f, 16f, 9.477f, 16f, 12f)
                horizontalLineTo(0f)
                curveToRelative(0f, -2.523f, 1.525f, -4.734f, 3.813f, -5.966f)
                lineTo(2.56f, 3.74f)
                arcToRelative(0.5f, 0.5f, 0f, isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 0.2f,
                    dy1 = -0.678f
                )
                close()
                moveTo(5f, 10f)
                arcToRelative(1f, 1f, 0f,
                    isMoreThanHalf = true,
                    isPositiveArc = false,
                    dx1 = 0f,
                    dy1 = -2f
                )
                arcToRelative(1f, 1f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 0f,
                    dy1 = 2f
                )
                moveToRelative(6f, 0f)
                arcToRelative(1f, 1f, 0f,
                    isMoreThanHalf = true,
                    isPositiveArc = false,
                    dx1 = 0f,
                    dy1 = -2f
                )
                arcToRelative(1f, 1f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 0f,
                    dy1 = 2f
                )
            }
        }.build()

        return _android!!
    }

@Suppress("ObjectPropertyName")
private var _android: ImageVector? = null

