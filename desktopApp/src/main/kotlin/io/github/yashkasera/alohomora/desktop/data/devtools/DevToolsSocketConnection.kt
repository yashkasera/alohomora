package io.github.yashkasera.alohomora.desktop.data.devtools

interface DevToolsSocketConnection {
    fun readExact(byteCount: Int): ByteArray?
    fun write(bytes: ByteArray)
    fun close()
}
