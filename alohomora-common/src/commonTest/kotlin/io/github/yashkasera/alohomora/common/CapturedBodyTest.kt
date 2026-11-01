package io.github.yashkasera.alohomora.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A body decoded leniently is both corrupted (so its replay sends different bytes) and inflated
 * (so a snapshot carrying it can pass the frame cap). These pin the strict decode every capture
 * path now shares.
 */
class CapturedBodyTest {

    @Test
    fun `json decodes unchanged`() {
        val json = """{"amount":100,"note":"chai ☕"}"""

        assertEquals(json, CapturedBody.decodeText(json.encodeToByteArray()))
    }

    @Test
    fun `invalid utf8 is not text`() {
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

        assertNull(CapturedBody.decodeText(png))
    }

    @Test
    fun `a nul byte is not text`() {
        // Valid UTF-8 byte for byte, as short protobuf messages often are, but for the NUL.
        assertNull(CapturedBody.decodeText(byteArrayOf(0x08, 0x16, 0x00, 0x01)))
    }

    @Test
    fun `truncation never splits a multibyte character`() {
        // "₹" is three bytes. Cutting after four bytes would land inside the second one.
        val bytes = "₹₹".encodeToByteArray()

        assertEquals("₹", CapturedBody.decodeText(bytes, maxBytes = 4))
    }

    @Test
    fun `truncation on a boundary keeps everything before it`() {
        assertEquals("abc", CapturedBody.decodeText("abcdef".encodeToByteArray(), maxBytes = 3))
    }

    @Test
    fun `known binary content types`() {
        assertTrue(CapturedBody.isKnownBinary("image/png"))
        assertTrue(CapturedBody.isKnownBinary("application/x-protobuf; charset=utf-8"))
        assertTrue(CapturedBody.isKnownBinary("application/grpc+proto"))
        assertTrue(CapturedBody.isKnownBinary("Application/Octet-Stream"))
    }

    @Test
    fun `text and unknown content types are left to the decoder`() {
        assertFalse(CapturedBody.isKnownBinary("application/json"))
        assertFalse(CapturedBody.isKnownBinary("text/plain; charset=utf-8"))
        assertFalse(CapturedBody.isKnownBinary("multipart/form-data; boundary=x"))
        assertFalse(CapturedBody.isKnownBinary(null))
    }
}
