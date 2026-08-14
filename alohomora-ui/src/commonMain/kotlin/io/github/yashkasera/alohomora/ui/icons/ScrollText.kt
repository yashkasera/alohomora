package io.github.yashkasera.alohomora.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("UnusedReceiverParameter")
val Icons.ScrollText: ImageVector
    get() {
        if (_scrollText != null) return _scrollText!!

        _scrollText = ImageVector.Builder(
            name = "ScrollText",
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
                moveTo(15f, 12f)
                horizontalLineToRelative(-5f)
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(15f, 8f)
                horizontalLineToRelative(-5f)
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(19f, 17f)
                verticalLineTo(5f)
                arcToRelative(2f, 2f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -2f,
                    dy1 = -2f
                )
                horizontalLineTo(4f)
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(8f, 21f)
                horizontalLineToRelative(12f)
                arcToRelative(2f, 2f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 2f,
                    dy1 = -2f
                )
                verticalLineToRelative(-1f)
                arcToRelative(1f, 1f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -1f,
                    dy1 = -1f
                )
                horizontalLineTo(11f)
                arcToRelative(1f, 1f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -1f,
                    dy1 = 1f
                )
                verticalLineToRelative(1f)
                arcToRelative(2f, 2f, 0f,
                    isMoreThanHalf = true,
                    isPositiveArc = true,
                    dx1 = -4f,
                    dy1 = 0f
                )
                verticalLineTo(5f)
                arcToRelative(2f, 2f, 0f,
                    isMoreThanHalf = true,
                    isPositiveArc = false,
                    dx1 = -4f,
                    dy1 = 0f
                )
                verticalLineToRelative(2f)
            }
        }.build()

        return _scrollText!!
    }

@Suppress("ObjectPropertyName")
private var _scrollText: ImageVector? = null
