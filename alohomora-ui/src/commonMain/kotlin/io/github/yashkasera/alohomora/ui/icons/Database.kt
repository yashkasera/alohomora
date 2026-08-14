package io.github.yashkasera.alohomora.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("UnusedReceiverParameter")
val Icons.Database: ImageVector
    get() {
        if (_database != null) return _database!!

        _database = ImageVector.Builder(
            name = "database",
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
                moveTo(21f, 5f)
                arcTo(9f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 12f, y1 = 8f)
                arcTo(9f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 3f, y1 = 5f)
                arcTo(9f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 21f, y1 = 5f)
                close()
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(3f, 5f)
                verticalLineTo(19f)
                arcTo(9f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 21f, y1 = 19f)
                verticalLineTo(5f)
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(3f, 12f)
                arcTo(9f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 21f, y1 = 12f)
            }
        }.build()

        return _database!!
    }

@Suppress("ObjectPropertyName")
private var _database: ImageVector? = null

