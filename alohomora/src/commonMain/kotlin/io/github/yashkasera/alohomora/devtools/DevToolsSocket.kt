package io.github.yashkasera.alohomora.devtools

internal expect class DevToolsSocket {
    fun readExact(byteCount: Int): ByteArray?
    fun write(bytes: ByteArray)
    fun close()
}

internal expect class DevToolsTcpServer() {
    fun start(port: Int, onClient: (DevToolsSocket) -> Unit)
    fun stop()
}
