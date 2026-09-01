package io.github.yashkasera.alohomora.desktop.presentation.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder

/**
 * Encodes [content] into the raw QR module grid (true NxN, no quiet zone or pixel scaling).
 *
 * Uses the low-level [Encoder] rather than `QRCodeWriter` on purpose: the writer returns a
 * pixel-scaled bitmap, but the grid is what draws crisply at any size in a [Canvas].
 */
internal fun encodeQrModules(content: String): Array<BooleanArray> {
    val qr = Encoder.encode(
        content,
        ErrorCorrectionLevel.M,
        mapOf<EncodeHintType, Any>(EncodeHintType.CHARACTER_SET to "UTF-8"),
    )
    val matrix = qr.matrix
    return Array(matrix.height) { y ->
        BooleanArray(matrix.width) { x -> matrix.get(x, y).toInt() == 1 }
    }
}

/**
 * Draws a QR code for [content]. Always black-on-white regardless of app theme — a themed QR
 * risks low contrast and fails to scan.
 */
@Composable
fun QrCode(
    content: String,
    modifier: Modifier = Modifier,
    quietZone: Int = 2,
) {
    val modules = remember(content) { encodeQrModules(content) }
    Canvas(modifier) {
        val count = modules.size
        if (count == 0) return@Canvas
        val totalModules = count + quietZone * 2
        val cell = size.minDimension / totalModules
        drawRect(Color.White, size = Size(cell * totalModules, cell * totalModules))
        for (y in 0 until count) {
            val row = modules[y]
            for (x in row.indices) {
                if (row[x]) {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset((x + quietZone) * cell, (y + quietZone) * cell),
                        size = Size(cell, cell),
                    )
                }
            }
        }
    }
}
