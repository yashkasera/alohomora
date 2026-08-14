package io.github.yashkasera.alohomora.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("UnusedReceiverParameter")
val Icons.HardDrive: ImageVector
    get() {
        if (_hardDrive != null) return _hardDrive!!

        _hardDrive = ImageVector.Builder(
            name = "hardDrive",
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
                lineTo(2f, 12f)
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(5.45f, 5.11f)
                lineTo(2f, 12f)
                verticalLineToRelative(6f)
                arcToRelative(2f, 2f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 2f,
                    dy1 = 2f
                )
                horizontalLineToRelative(16f)
                arcToRelative(2f, 2f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 2f,
                    dy1 = -2f
                )
                verticalLineToRelative(-6f)
                lineToRelative(-3.45f, -6.89f)
                arcTo(2f, 2f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    x1 = 16.76f,
                    y1 = 4f
                )
                horizontalLineTo(7.24f)
                arcToRelative(2f, 2f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -1.79f,
                    dy1 = 1.11f
                )
                close()
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(6f, 16f)
                lineTo(6.01f, 16f)
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(10f, 16f)
                lineTo(10.01f, 16f)
            }
        }.build()

        return _hardDrive!!
    }

@Suppress("ObjectPropertyName")
private var _hardDrive: ImageVector? = null

