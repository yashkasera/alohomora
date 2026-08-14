package io.github.yashkasera.alohomora.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("UnusedReceiverParameter")
val Icons.Upload: ImageVector
    get() {
        if (_upload != null) return _upload!!

        _upload = ImageVector.Builder(
            name = "upload",
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
                moveTo(12f, 15f)
                verticalLineToRelative(-12f)
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(8f, 7f)
                lineToRelative(4f, -4f)
                lineToRelative(4f, 4f)
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(3f, 17f)
                verticalLineToRelative(2f)
                arcToRelative(2f, 2f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 2f,
                    dy1 = 2f
                )
                horizontalLineToRelative(14f)
                arcToRelative(2f, 2f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 2f,
                    dy1 = -2f
                )
                verticalLineToRelative(-2f)
            }
        }.build()

        return _upload!!
    }

@Suppress("ObjectPropertyName")
private var _upload: ImageVector? = null
