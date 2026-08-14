package io.github.yashkasera.alohomora.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("UnusedReceiverParameter")
val Icons.SlidersHorizontal: ImageVector
    get() {
        if (_slidersHorizontal != null) return _slidersHorizontal!!

        _slidersHorizontal = ImageVector.Builder(
            name = "slidersHorizontal",
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
                moveTo(10f, 5f)
                horizontalLineTo(3f)
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 19f)
                horizontalLineTo(3f)
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(14f, 12f)
                horizontalLineTo(3f)
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(21f, 19f)
                horizontalLineToRelative(-5f)
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(21f, 12f)
                horizontalLineToRelative(-7f)
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(21f, 5f)
                horizontalLineToRelative(-9f)
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 5f)
                arcToRelative(2f, 2f, 0f,
                    isMoreThanHalf = true,
                    isPositiveArc = false,
                    dx1 = 4f,
                    dy1 = 0f
                )
                arcToRelative(2f, 2f, 0f,
                    isMoreThanHalf = true,
                    isPositiveArc = false,
                    dx1 = -4f,
                    dy1 = 0f
                )
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(16f, 12f)
                arcToRelative(2f, 2f, 0f,
                    isMoreThanHalf = true,
                    isPositiveArc = false,
                    dx1 = 4f,
                    dy1 = 0f
                )
                arcToRelative(2f, 2f, 0f,
                    isMoreThanHalf = true,
                    isPositiveArc = false,
                    dx1 = -4f,
                    dy1 = 0f
                )
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(14f, 19f)
                arcToRelative(2f, 2f, 0f,
                    isMoreThanHalf = true,
                    isPositiveArc = false,
                    dx1 = 4f,
                    dy1 = 0f
                )
                arcToRelative(2f, 2f, 0f,
                    isMoreThanHalf = true,
                    isPositiveArc = false,
                    dx1 = -4f,
                    dy1 = 0f
                )
            }
        }.build()

        return _slidersHorizontal!!
    }

@Suppress("ObjectPropertyName")
private var _slidersHorizontal: ImageVector? = null
