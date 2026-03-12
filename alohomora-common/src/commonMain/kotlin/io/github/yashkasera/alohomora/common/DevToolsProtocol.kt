package io.github.yashkasera.alohomora.common

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

object DevToolsProtocol {
    private const val MAGIC_VALUE = 0x414C4F48 // "ALOH"
    const val VERSION: Byte = 1
    private const val HEADER_LENGTH = 9

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encodeEnvelope(envelope: DevToolsEnvelope): ByteArray {
        val payload =
            json.encodeToString(DevToolsEnvelope.serializer(), envelope).encodeToByteArray()
        val length = payload.size
        val frame = ByteArray(HEADER_LENGTH + length)
        writeInt(frame, 0, MAGIC_VALUE)
        frame[4] = VERSION
        writeInt(frame, 5, length)
        payload.copyInto(frame, HEADER_LENGTH)
        return frame
    }

    fun decodeEnvelope(jsonBytes: ByteArray): DevToolsEnvelope {
        val jsonString = jsonBytes.decodeToString()
        return json.decodeFromString(DevToolsEnvelope.serializer(), jsonString)
    }

    fun decodeFrame(frame: ByteArray): DevToolsEnvelope {
        require(frame.size >= HEADER_LENGTH) { "Frame too short" }
        val magic = readInt(frame, 0)
        require(magic == MAGIC_VALUE) { "Invalid magic" }
        val version = frame[4]
        require(version == VERSION) { "Unsupported version: $version" }
        val length = readInt(frame, 5)
        require(frame.size == HEADER_LENGTH + length) { "Invalid length" }
        val jsonBytes = frame.copyOfRange(HEADER_LENGTH, HEADER_LENGTH + length)
        return decodeEnvelope(jsonBytes)
    }

    suspend fun readEnvelope(socket: io.github.yashkasera.alohomora.devtools.DevToolsSocket): DevToolsEnvelope? {
        val header = socket.readExact(HEADER_LENGTH) ?: return null
        val magic = readInt(header, 0)
        if (magic != MAGIC_VALUE) return null
        val version = header[4]
        if (version != VERSION) return null
        val length = readInt(header, 5)
        if (length < 0) return null
        val jsonBytes = socket.readExact(length) ?: return null
        return decodeEnvelope(jsonBytes)
    }

    inline fun <reified T> encodePayload(value: T): JsonElement {
        return json.encodeToJsonElement(value)
    }

    inline fun <reified T> decodePayload(payload: JsonElement): T {
        return json.decodeFromJsonElement(payload)
    }

    private fun writeInt(buffer: ByteArray, offset: Int, value: Int) {
        buffer[offset] = (value shr 24).toByte()
        buffer[offset + 1] = (value shr 16).toByte()
        buffer[offset + 2] = (value shr 8).toByte()
        buffer[offset + 3] = value.toByte()
    }

    private fun readInt(buffer: ByteArray, offset: Int): Int {
        return (buffer[offset].toInt() shl 24) or
            ((buffer[offset + 1].toInt() and 0xFF) shl 16) or
            ((buffer[offset + 2].toInt() and 0xFF) shl 8) or
            (buffer[offset + 3].toInt() and 0xFF)
    }
}
