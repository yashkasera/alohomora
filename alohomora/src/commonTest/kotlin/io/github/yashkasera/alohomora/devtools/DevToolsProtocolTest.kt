package io.github.yashkasera.alohomora.devtools

import io.github.yashkasera.alohomora.common.AuthChallengeMessage
import io.github.yashkasera.alohomora.common.DevToolsProtocol
import kotlin.test.Test
import kotlin.test.assertEquals

class DevToolsProtocolTest {
    @Test
    fun encodeDecodeFrameRoundTrip() {
        val message = AuthChallengeMessage(sequence = 42)
        val frame = DevToolsProtocol.encodeEnvelope(message)
        val decoded = DevToolsProtocol.decodeFrame(frame)
        assertEquals(message, decoded)
    }
}
