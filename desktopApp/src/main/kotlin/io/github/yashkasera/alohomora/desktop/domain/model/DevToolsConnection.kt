package io.github.yashkasera.alohomora.desktop.domain.model

sealed class DevToolsConnection {
    data object Disconnected : DevToolsConnection()
    data class Connecting(val host: String, val port: Int) : DevToolsConnection()
    data class AwaitingAuth(val host: String, val port: Int) : DevToolsConnection()
    data class Connected(val host: String, val port: Int) : DevToolsConnection()
    data class Failed(val reason: String) : DevToolsConnection()
}
