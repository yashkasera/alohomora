package io.github.yashkasera.alohomora.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Alohomora: ImageVector
    get() {
        if (_Alohomora != null) return _Alohomora!!

        _Alohomora = ImageVector.Builder(
            name = "A",
            defaultWidth = 175.dp,
            defaultHeight = 183.dp,
            viewportWidth = 175f,
            viewportHeight = 183f,
        ).apply {
            path(
                fill = SolidColor(Color(0xFFFFFFFF)),
            ) {
                moveTo(157.636f, 167.013f)
                horizontalLineTo(174.779f)
                verticalLineTo(182.078f)
                horizontalLineTo(157.636f)
                verticalLineTo(167.013f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFFFFFFFF)),
            ) {
                moveTo(25.7143f, 174.286f)
                horizontalLineTo(121.299f)
                lineTo(66.2338f, 22.8571f)
                lineTo(8.31169f, 182.078f)
                horizontalLineTo(0f)
                lineTo(66.2338f, 0f)
                lineTo(132.468f, 182.078f)
                horizontalLineTo(25.7143f)
                verticalLineTo(174.286f)
                close()
            }
        }.build()

        return _Alohomora!!
    }

private var _Alohomora: ImageVector? = null

