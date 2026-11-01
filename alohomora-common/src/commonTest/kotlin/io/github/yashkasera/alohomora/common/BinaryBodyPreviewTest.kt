package io.github.yashkasera.alohomora.common

import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** What every console shows in place of a binary request body. */
class BinaryBodyPreviewTest {

    @Test
    fun `decoded size is read from the base64 length`() {
        for (size in 0..10) {
            val base64 = Base64.encode(ByteArray(size))
            assertEquals(size.toLong(), BinaryBodyPreview.decodedSize(base64), "for $size bytes")
        }
    }

    @Test
    fun `hex shows the leading bytes and marks that there are more`() {
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

        assertEquals("89 50 4E 47 0D 0A 1A 0A", BinaryBodyPreview.hex(Base64.encode(png)))
        assertEquals("89 50 4E …", BinaryBodyPreview.hex(Base64.encode(png), maxBytes = 3))
    }

    @Test
    fun `hex of a long body decodes only the prefix`() {
        val body = ByteArray(100_000) { it.toByte() }

        val hex = BinaryBodyPreview.hex(Base64.encode(body))

        assertEquals(BinaryBodyPreview.HEX_PREVIEW_BYTES, hex.removeSuffix(" …").split(' ').size)
        assertTrue(hex.startsWith("00 01 02 03"))
        assertTrue(hex.endsWith(" …"))
    }

    @Test
    fun `sizes are printed in the unit that fits`() {
        assertEquals("512 B", BinaryBodyPreview.formatSize(512))
        assertEquals("1.5 KB", BinaryBodyPreview.formatSize(1536))
        assertEquals("2.0 MB", BinaryBodyPreview.formatSize(2L * 1024 * 1024))
    }

    @Test
    fun `describe names size and type before the hex`() {
        val base64 = Base64.encode(byteArrayOf(0x00, 0xFF.toByte()))

        assertEquals(
            "Binary body, 2 B, application/octet-stream\n\n00 FF",
            BinaryBodyPreview.describe(base64, "application/octet-stream"),
        )
        assertEquals("Binary body, 2 B\n\n00 FF", BinaryBodyPreview.describe(base64, null))
    }

    @Test
    fun `a multipart boundary is recognised in the content type`() {
        assertTrue(BinaryBodyPreview.hasMultipartBoundary("multipart/form-data; boundary=abc"))
        assertTrue(BinaryBodyPreview.hasMultipartBoundary("multipart/mixed;Boundary=\"abc\""))
        assertFalse(BinaryBodyPreview.hasMultipartBoundary("application/octet-stream"))
        assertFalse(BinaryBodyPreview.hasMultipartBoundary(null))
    }
}
