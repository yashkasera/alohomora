package io.github.yashkasera.alohomora.devtools

import io.github.yashkasera.alohomora.common.AuthChallengeMessage
import io.github.yashkasera.alohomora.common.DevToolsProtocol
import io.github.yashkasera.alohomora.common.EnvelopeRead
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest

class DevToolsProtocolTest {
    @Test
    fun encodeDecodeFrameRoundTrip() {
        val message = AuthChallengeMessage(sequence = 42)
        val frame = DevToolsProtocol.encodeEnvelope(message)
        val decoded = DevToolsProtocol.decodeFrame(frame)
        assertEquals(message, decoded)
    }

    @Test
    fun readEnvelopeReturnsTheDecodedMessage() = runTest {
        val message = AuthChallengeMessage(sequence = 7)
        val read = readFrom(DevToolsProtocol.encodeEnvelope(message))
        assertEquals(message, assertIs<EnvelopeRead.Message>(read).message)
    }

    @Test
    fun readEnvelopeReportsEndOfStreamOnAnEmptyStream() = runTest {
        assertIs<EnvelopeRead.EndOfStream>(readFrom(ByteArray(0)))
    }

    /**
     * The contract this whole result type exists for.
     *
     * A version mismatch used to be indistinguishable from a clean EOF, so the desktop reconnected
     * forever and showed a bare "disconnected" while the only explanation went to logcat. Both
     * versions have to survive the read, or neither side can name what is wrong.
     */
    @Test
    fun readEnvelopeReportsAVersionMismatchWithBothVersions() = runTest {
        val frame = DevToolsProtocol.encodeEnvelope(AuthChallengeMessage(sequence = 1))
        val peerVersion = (DevToolsProtocol.VERSION + 1).toByte()
        frame[VERSION_OFFSET] = peerVersion

        val read = assertIs<EnvelopeRead.VersionMismatch>(readFrom(frame))
        assertEquals(peerVersion.toInt(), read.peerVersion)
        assertEquals(DevToolsProtocol.VERSION.toInt(), read.localVersion)
    }

    @Test
    fun readEnvelopeReportsBadMagicAsMalformed() = runTest {
        val frame = DevToolsProtocol.encodeEnvelope(AuthChallengeMessage(sequence = 1))
        frame[0] = 0
        assertIs<EnvelopeRead.Malformed>(readFrom(frame))
    }

    /**
     * A version mismatch must win over a nonsense length.
     *
     * Once the version byte disagrees the rest of the header cannot be trusted to mean anything, so
     * checking the length first would report the more confusing of the two problems.
     */
    @Test
    fun versionIsCheckedBeforePayloadLength() = runTest {
        val frame = DevToolsProtocol.encodeEnvelope(AuthChallengeMessage(sequence = 1))
        frame[VERSION_OFFSET] = (DevToolsProtocol.VERSION + 1).toByte()
        // Length field: 0xFFFFFFFF, which on its own would be rejected as out of bounds.
        for (offset in LENGTH_OFFSET until LENGTH_OFFSET + 4) frame[offset] = 0xFF.toByte()

        assertIs<EnvelopeRead.VersionMismatch>(readFrom(frame))
    }

    private suspend fun readFrom(bytes: ByteArray): EnvelopeRead =
        DevToolsProtocol.readEnvelope(DevToolsSocket.over(FakeByteChannel(bytes)))

    /** Replays a fixed byte sequence, then reports EOF the way a closed socket does. */
    private class FakeByteChannel(private val bytes: ByteArray) : DevToolsByteChannel {
        private var position = 0

        override suspend fun readFully(dest: ByteArray, offset: Int, length: Int): Boolean {
            if (position + length > bytes.size) return false
            bytes.copyInto(dest, offset, position, position + length)
            position += length
            return true
        }

        override suspend fun write(bytes: ByteArray) = Unit

        override fun close() = Unit
    }

    private companion object {
        /** Header layout is frozen: magic (4), version (1), length (4). */
        const val VERSION_OFFSET = 4
        const val LENGTH_OFFSET = 5
    }
}
