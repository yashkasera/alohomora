package io.github.yashkasera.alohomora.desktop.data.devtools

import java.net.InetSocketAddress
import java.net.Socket

class DefaultDevToolsSocketFactory : DevToolsSocketFactory {
    override fun connect(host: String, port: Int): DevToolsSocketConnection {
        val socket = Socket()
        socket.connect(InetSocketAddress(host, port), 3000)
        return DefaultDevToolsSocketConnection(socket)
    }
}
