package io.github.yashkasera.alohomora.devtools

import kotlinx.serialization.json.JsonNull
import kotlin.test.Test
import kotlin.test.assertEquals

class DevToolsProtocolTest {
    @Test
    fun encodeDecodeFrameRoundTrip() {
        val envelope = DevToolsEnvelope(
            type = DevToolsMessageType.STREAM_EVENT,
            sequence = 42,
            payload = JsonNull,
        )
        val frame = DevToolsProtocol.encodeEnvelope(envelope)
        val decoded = DevToolsProtocol.decodeFrame(frame)
        assertEquals(envelope.type, decoded.type)
        assertEquals(envelope.sequence, decoded.sequence)
        assertEquals(envelope.payload, decoded.payload)
    }
}
