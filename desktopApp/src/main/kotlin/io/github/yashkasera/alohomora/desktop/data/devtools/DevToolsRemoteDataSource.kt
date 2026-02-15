package io.github.yashkasera.alohomora.desktop.data.devtools

import io.github.yashkasera.alohomora.common.DevToolsEnvelope
import io.github.yashkasera.alohomora.common.DevToolsProtocol

open class DevToolsRemoteDataSource(
    private val socketFactory: DevToolsSocketFactory = DefaultDevToolsSocketFactory(),
) {
    open fun connect(host: String, port: Int): DevToolsSocketConnection {
        return socketFactory.connect(host, port)
    }

    open suspend fun processConnection(
        connection: DevToolsSocketConnection,
        onEnvelope: (DevToolsEnvelope) -> Unit,
    ) {
        while (true) {
            val header = connection.readExact(9) ?: break
            val length = ((header[5].toInt() and 0xFF) shl 24) or
                ((header[6].toInt() and 0xFF) shl 16) or
                ((header[7].toInt() and 0xFF) shl 8) or
                (header[8].toInt() and 0xFF)
            val body = connection.readExact(length) ?: break
            val frame = header + body
            val envelope = DevToolsProtocol.decodeFrame(frame)
            onEnvelope(envelope)
        }
    }
}
