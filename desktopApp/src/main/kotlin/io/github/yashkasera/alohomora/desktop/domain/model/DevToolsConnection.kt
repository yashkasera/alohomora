package io.github.yashkasera.alohomora.desktop.domain.model

sealed class DevToolsConnection {
    data object Disconnected : DevToolsConnection()
    data class Connecting(val host: String, val port: Int) : DevToolsConnection()
    data class AwaitingAuth(val host: String, val port: Int) : DevToolsConnection()
    data class Connected(val host: String, val port: Int) : DevToolsConnection()
    /**
     * The device stopped answering and we are trying to get back in.
     *
     * Distinct from [Failed], which means give up, and from [Connected], which is what the UI
     * used to keep claiming after an iOS app was suspended — the socket was dead and every panel
     * sat there looking live. Most often this is iOS suspending a backgrounded app; on Android it
     * is the process being killed.
     */
    data class Reconnecting(
        val host: String,
        val port: Int,
        val attempt: Int,
    ) : DevToolsConnection()

    data class Failed(val reason: String) : DevToolsConnection()
}
