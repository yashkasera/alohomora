package io.github.yashkasera.alohomora.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("UnusedReceiverParameter")
val Icons.Save: ImageVector
    get() {
        if (_save != null) return _save!!

        _save = ImageVector.Builder(
            name = "save",
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
                moveTo(15.2f, 3f)
                arcToRelative(2f, 2f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 1.4f,
                    dy1 = 0.6f
                )
                lineToRelative(3.8f, 3.8f)
                arcToRelative(2f, 2f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 0.6f,
                    dy1 = 1.4f
                )
                verticalLineTo(19f)
                arcToRelative(2f, 2f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -2f,
                    dy1 = 2f
                )
                horizontalLineTo(5f)
                arcToRelative(2f, 2f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -2f,
                    dy1 = -2f
                )
                verticalLineTo(5f)
                arcToRelative(2f, 2f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 2f,
                    dy1 = -2f
                )
                close()
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(17f, 21f)
                verticalLineToRelative(-7f)
                arcToRelative(1f, 1f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -1f,
                    dy1 = -1f
                )
                horizontalLineTo(8f)
                arcToRelative(1f, 1f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -1f,
                    dy1 = 1f
                )
                verticalLineToRelative(7f)
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(7f, 3f)
                verticalLineToRelative(4f)
                arcToRelative(1f, 1f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 1f,
                    dy1 = 1f
                )
                horizontalLineToRelative(7f)
            }
        }.build()

        return _save!!
    }

@Suppress("ObjectPropertyName")
private var _save: ImageVector? = null
