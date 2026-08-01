package io.github.yashkasera.alohomora.common

import io.github.yashkasera.alohomora.devtools.DevToolsSocket
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

object DevToolsProtocol {
    private const val MAGIC_VALUE = 0x414C4F48
    const val VERSION: Byte = 1
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
     * Reads one framed message, or null on clean EOF / unusable frame.
     *
     * A null return means "stop reading" — the caller must tear the connection down.
     */
    suspend fun readEnvelope(socket: DevToolsSocket): DevToolsMessage? {
        val header = socket.readExact(HEADER_LENGTH) ?: return null
        val magic = readInt(header, 0)
        if (magic != MAGIC_VALUE) return null
        val version = header[4]
        if (version != VERSION) {
            println("[Alohomora] DevTools protocol mismatch: peer sent v$version, expected v$VERSION")
            return null
        }
        val length = readInt(header, 5)
        // Both bounds matter: negative is a malformed/hostile header, and over-max would
        // otherwise become an unbounded ByteArray allocation. See MAX_PAYLOAD_BYTES.
        if (length !in 0..MAX_PAYLOAD_BYTES) {
            println("[Alohomora] Rejecting DevTools frame with payload length $length")
            return null
        }
        val jsonBytes = socket.readExact(length) ?: return null
        return decodeEnvelope(jsonBytes)
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
