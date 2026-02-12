package io.github.yashkasera.alohomora.desktop.data.devtools

interface DevToolsSocketFactory {
    fun connect(host: String, port: Int): DevToolsSocketConnection
}
