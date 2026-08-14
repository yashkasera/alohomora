package io.github.yashkasera.alohomora.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("UnusedReceiverParameter")
val Icons.Settings: ImageVector
    get() {
        if (_settings != null) return _settings!!

        _settings = ImageVector.Builder(
            name = "settings",
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
                moveTo(9.671f, 4.136f)
                arcToRelative(2.34f, 2.34f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 4.659f,
                    dy1 = 0f
                )
                arcToRelative(2.34f, 2.34f, 0f, isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 3.319f,
                    dy1 = 1.915f
                )
                arcToRelative(2.34f, 2.34f, 0f, isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 2.33f,
                    dy1 = 4.033f
                )
                arcToRelative(2.34f, 2.34f, 0f, isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 0f,
                    dy1 = 3.831f
                )
                arcToRelative(2.34f, 2.34f, 0f, isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -2.33f,
                    dy1 = 4.033f
                )
                arcToRelative(2.34f, 2.34f, 0f, isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -3.319f,
                    dy1 = 1.915f
                )
                arcToRelative(2.34f, 2.34f, 0f, isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -4.659f,
                    dy1 = 0f
                )
                arcToRelative(2.34f, 2.34f, 0f, isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -3.32f,
                    dy1 = -1.915f
                )
                arcToRelative(2.34f, 2.34f, 0f, isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -2.33f,
                    dy1 = -4.033f
                )
                arcToRelative(2.34f, 2.34f, 0f, isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 0f,
                    dy1 = -3.831f
                )
                arcTo(2.34f, 2.34f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    x1 = 6.35f,
                    y1 = 6.051f
                )
                arcToRelative(2.34f, 2.34f, 0f, isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 3.319f,
                    dy1 = -1.915f
                )
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(15f, 12f)
                arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 12f, y1 = 15f)
                arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 9f, y1 = 12f)
                arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 15f, y1 = 12f)
                close()
            }
        }.build()

        return _settings!!
    }

@Suppress("ObjectPropertyName")
private var _settings: ImageVector? = null

