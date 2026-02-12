package io.github.yashkasera.alohomora.desktop.data.devtools

import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

class DefaultDevToolsSocketConnection(
    private val socket: Socket,
) : DevToolsSocketConnection {
    private val input: InputStream = socket.getInputStream()
    private val output: OutputStream = socket.getOutputStream()

    override fun readExact(byteCount: Int): ByteArray? {
        val buffer = ByteArray(byteCount)
        var offset = 0
        while (offset < byteCount) {
            val read = input.read(buffer, offset, byteCount - offset)
            if (read == -1) return null
            offset += read
        }
        return buffer
    }

    override fun write(bytes: ByteArray) {
        output.write(bytes)
        output.flush()
    }

    override fun close() {
        socket.close()
    }
}
