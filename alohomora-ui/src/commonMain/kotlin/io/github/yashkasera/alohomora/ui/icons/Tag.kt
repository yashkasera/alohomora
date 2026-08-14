package io.github.yashkasera.alohomora.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("UnusedReceiverParameter")
val Icons.Tag: ImageVector
    get() {
        if (_Tag != null) return _Tag!!

        _Tag = ImageVector.Builder(
            name = "Tag",
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
                moveTo(12.586f, 2.586f)
                arcTo(2f, 2f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    x1 = 11.172f,
                    y1 = 2f
                )
                horizontalLineTo(4f)
                arcToRelative(2f, 2f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -2f,
                    dy1 = 2f
                )
                verticalLineToRelative(7.172f)
                arcToRelative(2f, 2f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 0.586f,
                    dy1 = 1.414f
                )
                lineToRelative(8.704f, 8.704f)
                arcToRelative(2.426f, 2.426f, 0f, isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 3.42f,
                    dy1 = 0f
                )
                lineToRelative(6.58f, -6.58f)
                arcToRelative(2.426f, 2.426f, 0f, isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 0f,
                    dy1 = -3.42f
                )
                close()
            }
            path(
                fill = SolidColor(Color.Black),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(8f, 7.5f)
                arcTo(0.5f, 0.5f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    x1 = 7.5f,
                    y1 = 8f
                )
                arcTo(0.5f, 0.5f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    x1 = 7f,
                    y1 = 7.5f
                )
                arcTo(0.5f, 0.5f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    x1 = 8f,
                    y1 = 7.5f
                )
                close()
            }
        }.build()

        return _Tag!!
    }

@Suppress("ObjectPropertyName")
private var _Tag: ImageVector? = null

