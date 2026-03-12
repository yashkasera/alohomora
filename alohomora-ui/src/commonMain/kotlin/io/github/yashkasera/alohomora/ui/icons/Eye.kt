package io.github.yashkasera.alohomora.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Eye: ImageVector
    get() {
        if (_eye != null) return _eye!!

        _eye = ImageVector.Builder(
            name = "eye",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(1f, 12f)
                quadTo(4.5f, 5f, 12f, 5f)
                quadTo(19.5f, 5f, 23f, 12f)
                quadTo(19.5f, 19f, 12f, 19f)
                quadTo(4.5f, 19f, 1f, 12f)
                close()
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 9f)
                arcTo(3f, 3f, 0f, true, true, 12f, 15f)
                arcTo(3f, 3f, 0f, true, true, 12f, 9f)
                close()
            }
        }.build()

        return _eye!!
    }

private var _eye: ImageVector? = null
