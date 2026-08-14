package io.github.yashkasera.alohomora.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("UnusedReceiverParameter")
val Icons.Filter: ImageVector
    get() {
        if (_filter != null) return _filter!!

        _filter = ImageVector.Builder(
            name = "filter",
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
                moveTo(22f, 3f)
                horizontalLineTo(2f)
                lineToRelative(8f, 9.46f)
                verticalLineTo(19f)
                lineToRelative(4f, 2f)
                verticalLineToRelative(-8.54f)
                close()
            }
        }.build()

        return _filter!!
    }

@Suppress("ObjectPropertyName")
private var _filter: ImageVector? = null
