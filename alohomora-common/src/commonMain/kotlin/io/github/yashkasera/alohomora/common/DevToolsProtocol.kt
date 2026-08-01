package io.github.yashkasera.alohomora.common

import io.github.yashkasera.alohomora.devtools.DevToolsSocket
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/**
 * The outcome of reading one frame.
 *
 * A sealed result rather than a nullable message because the three ways a read can stop needing
 * very different handling, and collapsing them into `null` cost real debugging time: a desktop
 * paired with an app speaking a different wire version looked exactly like a random disconnect,
 * and the desktop retried it forever at the backoff cap with nothing on screen to explain why.
 */
sealed interface EnvelopeRead {
    /** A frame was read and decoded. */
    data class Message(val message: DevToolsMessage) : EnvelopeRead

    /** The peer closed, or vanished mid-frame. Ordinary, and the caller should just tear down. */
    data object EndOfStream : EnvelopeRead

    /**
     * The peer speaks a different wire protocol. Terminal: retrying cannot help, only upgrading
     * one side can, so the caller must say so rather than reconnect.
     */
    data class VersionMismatch(val peerVersion: Int, val localVersion: Int) : EnvelopeRead

    /** Bad magic, an impossible payload length, or undecodable JSON. */
    data class Malformed(val reason: String) : EnvelopeRead
}

object DevToolsProtocol {
    private const val MAGIC_VALUE = 0x414C4F48

    /**
     * Wire protocol version. Bump only for a breaking change to framing or message semantics.
     *
     * Additive changes do not need it: an unknown message type deserializes to [UnknownMessage] and
     * is ignored, unknown fields are dropped by `ignoreUnknownKeys`, and a new capability should
     * default to "unsupported" the way [InitialStatePayload.replaySupported] does. Bumping this
     * breaks interop with every existing build, so it should happen close to never.
     */
    const val VERSION: Byte = 1

    /**
     * Header layout: magic (4 bytes), version (1), payload length (4). **Frozen. Never change it.**
     *
     * This is the only part of the protocol two mismatched builds are guaranteed to agree on, and
     * therefore the only reason they can discover that they disagree at all: [readEnvelope] reads
     * the version byte out of a header it can always parse, and reports [EnvelopeRead.VersionMismatch]
     * instead of dropping the connection unexplained. Change the header shape and a version
     * mismatch degrades back into an unreadable stream, which is indistinguishable from corruption.
     * A breaking change belongs in the payload, behind a [VERSION] bump.
     */
    private const val HEADER_LENGTH = 9

    /**
     * Hard ceiling on a single frame's JSON payload.
     *
     * The header's 4-byte length field is attacker-controlled and is read *before* the OTP
     * handshake, so without a cap a 9-byte frame claiming `0x7FFFFFFF` makes the peer
     * allocate 2 GB and die. 8 MiB comfortably exceeds the largest legitimate frame (an
     * INITIAL_STATE carrying 200 traces plus 500 telemetry events).
     */
    const val MAX_PAYLOAD_BYTES: Int = 8 * 1024 * 1024

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        // A newer peer may send a message type this build has never heard of. Without a
        // default deserializer that throws SerializationException out of the read loop and
        // wedges the connection; mapping it to a sentinel lets callers ignore it instead.
        serializersModule = SerializersModule {
            polymorphic(DevToolsMessage::class) {
                defaultDeserializer { UnknownMessage.serializer() }
            }
        }
    }

    fun encodeEnvelope(message: DevToolsMessage): ByteArray {
        val payload = json.encodeToString(DevToolsMessage.serializer(), message).encodeToByteArray()
        val length = payload.size
        val frame = ByteArray(HEADER_LENGTH + length)
        writeInt(frame, 0, MAGIC_VALUE)
        frame[4] = VERSION
        writeInt(frame, 5, length)
        payload.copyInto(frame, HEADER_LENGTH)
        return frame
    }

    /**
     * Decodes a payload, returning null rather than throwing on malformed input.
     *
     * Callers run this inside socket read loops; a thrown SerializationException used to
     * escape the loop and skip its teardown, leaving a leaked socket and a UI stuck on
     * "Connected".
     */
    fun decodeEnvelope(jsonBytes: ByteArray): DevToolsMessage? =
        try {
            json.decodeFromString(DevToolsMessage.serializer(), jsonBytes.decodeToString())
        } catch (e: Exception) {
            println("[Alohomora] Discarding undecodable DevTools frame: ${e.message}")
            null
        }

    fun decodeFrame(frame: ByteArray): DevToolsMessage? {
        require(frame.size >= HEADER_LENGTH) { "Frame too short" }
        val magic = readInt(frame, 0)
        require(magic == MAGIC_VALUE) { "Invalid magic" }
        val version = frame[4]
        require(version == VERSION) { "Unsupported version: $version" }
        val length = readInt(frame, 5)
        require(length in 0..MAX_PAYLOAD_BYTES) { "Invalid length: $length" }
        require(frame.size == HEADER_LENGTH + length) { "Invalid length" }
        return decodeEnvelope(frame.copyOfRange(HEADER_LENGTH, HEADER_LENGTH + length))
    }

    /**
     * Reads one framed message.
     *
     * Anything other than [EnvelopeRead.Message] means "stop reading" and the caller must tear the
     * connection down. Which of them it is decides what the user is told: see [EnvelopeRead].
     */
    suspend fun readEnvelope(socket: DevToolsSocket): EnvelopeRead {
        val header = socket.readExact(HEADER_LENGTH) ?: return EnvelopeRead.EndOfStream
        val magic = readInt(header, 0)
        if (magic != MAGIC_VALUE) {
            return EnvelopeRead.Malformed("bad magic 0x${magic.toString(16)}")
        }
        // Checked before the length so a peer whose header layout we cannot trust is reported as a
        // version mismatch rather than as a nonsense payload size.
        val version = header[4]
        if (version != VERSION) {
            return EnvelopeRead.VersionMismatch(
                peerVersion = version.toInt(),
                localVersion = VERSION.toInt(),
            )
        }
        val length = readInt(header, 5)
        // Both bounds matter: negative is a malformed/hostile header, and over-max would
        // otherwise become an unbounded ByteArray allocation. See MAX_PAYLOAD_BYTES.
        if (length !in 0..MAX_PAYLOAD_BYTES) {
            return EnvelopeRead.Malformed("payload length $length outside 0..$MAX_PAYLOAD_BYTES")
        }
        val jsonBytes = socket.readExact(length) ?: return EnvelopeRead.EndOfStream
        // decodeEnvelope has already logged the specific serialization error.
        val message = decodeEnvelope(jsonBytes)
            ?: return EnvelopeRead.Malformed("undecodable payload")
        return EnvelopeRead.Message(message)
    }

    private fun writeInt(buffer: ByteArray, offset: Int, value: Int) {
        buffer[offset] = (value shr 24).toByte()
        buffer[offset + 1] = (value shr 16).toByte()
        buffer[offset + 2] = (value shr 8).toByte()
        buffer[offset + 3] = value.toByte()
    }

    private fun readInt(buffer: ByteArray, offset: Int): Int {
        return ((buffer[offset].toInt() and 0xFF) shl 24) or
            ((buffer[offset + 1].toInt() and 0xFF) shl 16) or
            ((buffer[offset + 2].toInt() and 0xFF) shl 8) or
            (buffer[offset + 3].toInt() and 0xFF)
    }
}
