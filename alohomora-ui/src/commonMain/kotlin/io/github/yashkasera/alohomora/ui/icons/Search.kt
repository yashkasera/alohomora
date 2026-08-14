package io.github.yashkasera.alohomora.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("UnusedReceiverParameter")
val Icons.Search: ImageVector
    get() {
        if (_search != null) return _search!!

        _search = ImageVector.Builder(
            name = "search",
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
                moveToRelative(21f, 21f)
                lineToRelative(-4.34f, -4.34f)
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(19f, 11f)
                arcTo(8f, 8f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 11f, y1 = 19f)
                arcTo(8f, 8f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 3f, y1 = 11f)
                arcTo(8f, 8f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 19f, y1 = 11f)
                close()
            }
        }.build()

        return _search!!
    }

@Suppress("ObjectPropertyName")
private var _search: ImageVector? = null

