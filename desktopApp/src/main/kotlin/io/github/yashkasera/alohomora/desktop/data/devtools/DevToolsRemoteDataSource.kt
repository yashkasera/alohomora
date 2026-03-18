package io.github.yashkasera.alohomora.desktop.data.devtools

import io.github.yashkasera.alohomora.common.DevToolsMessage
import io.github.yashkasera.alohomora.common.DevToolsProtocol
import io.github.yashkasera.alohomora.devtools.DevToolsSocket
import io.github.yashkasera.alohomora.devtools.DevToolsTcpClient

open class DevToolsRemoteDataSource(
    private val tcpClient: DevToolsTcpClient = DevToolsTcpClient(),
) {
    open suspend fun connect(host: String, port: Int): DevToolsSocket {
        return tcpClient.connect(host, port, timeoutMillis = 3000)
    }

    open suspend fun processConnection(
        connection: DevToolsSocket,
        onMessage: (DevToolsMessage) -> Unit,
    ) {
        while (true) {
            val message = DevToolsProtocol.readEnvelope(connection) ?: break
            onMessage(message)
        }
    }
}
