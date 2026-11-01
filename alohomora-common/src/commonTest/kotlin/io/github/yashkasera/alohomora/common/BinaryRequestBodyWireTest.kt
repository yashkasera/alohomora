package io.github.yashkasera.alohomora.common

import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Wire contract for [TrafficEntry.requestBodyBase64]. It ships without a protocol version bump, so
 * both mixed-version directions have to keep decoding.
 */
class BinaryRequestBodyWireTest {

    private val bytes = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 0, 0xFF.toByte())

    private fun roundTrip(message: DevToolsMessage): DevToolsMessage =
        assertNotNull(DevToolsProtocol.decodeFrame(DevToolsProtocol.encodeEnvelope(message)))

    @Test
    fun `a binary request body survives the wire byte for byte`() {
        val entry = TrafficEntry(
            id = "a",
            url = "https://example.com/upload",
            method = "POST",
            requestBody = TrafficEntry.UNABLE_PARSE_MESSAGE,
            requestBodyBase64 = Base64.encode(bytes),
            requestContentType = "image/png",
        )

        val decoded = assertIs<StreamTrafficMessage>(roundTrip(StreamTrafficMessage(1, entry)))

        assertEquals(entry, decoded.traffic)
        assertContentEquals(bytes, Base64.decode(decoded.traffic.requestBodyBase64!!))
    }

    /** A newer desktop against an app that predates binary capture: the key is simply absent. */
    @Test
    fun `an entry from an older app decodes without the field`() {
        val json = """
            {
              "type": "STREAM_TRAFFIC",
              "sequence": 1,
              "traffic": { "id": "a", "method": "POST", "requestBody": "Cannot parse body" }
            }
        """.trimIndent()

        val decoded = assertIs<StreamTrafficMessage>(
            assertNotNull(DevToolsProtocol.decodeEnvelope(json.encodeToByteArray())),
        )
        assertNull(decoded.traffic.requestBodyBase64)
        assertEquals(TrafficEntry.UNABLE_PARSE_MESSAGE, decoded.traffic.requestBody)
    }

    /**
     * An older desktop against a newer app sees an unknown key. It decodes because the protocol's
     * Json ignores unknown keys, and what it renders is the placeholder `requestBody` still carries.
     */
    @Test
    fun `an unknown key alongside the body does not break decoding`() {
        val json = """
            {
              "type": "STREAM_TRAFFIC",
              "sequence": 1,
              "traffic": { "id": "a", "requestBody": "Cannot parse body", "requestBodyBase64": "AAE=", "fromTheFuture": 1 }
            }
        """.trimIndent()

        val decoded = assertIs<StreamTrafficMessage>(
            assertNotNull(DevToolsProtocol.decodeEnvelope(json.encodeToByteArray())),
        )
        assertEquals("AAE=", decoded.traffic.requestBodyBase64)
    }
}
