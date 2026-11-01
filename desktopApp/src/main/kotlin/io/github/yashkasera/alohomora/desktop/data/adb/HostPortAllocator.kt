package io.github.yashkasera.alohomora.desktop.data.adb

import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket

/**
 * Picks the host side of a device's `adb forward`.
 *
 * Every Android session used to forward host port 53999, so a second device took over the first
 * one's tunnel (see [AdbRepositoryImpl]'s `forwards`). The device side stays on the app's DevTools
 * port; only the host side has to be unique, and it has to be unique against more than this
 * window's sessions — an iOS Simulator app listens on the host's 53999 directly, and a forward
 * left behind by a crashed or second Alohomora instance still holds its port in the adb server.
 */
internal object HostPortAllocator {
    /** The DevTools server's default port, tried first so a single-device setup is unchanged. */
    const val DEFAULT_PORT = 53999

    /** How far past [DEFAULT_PORT] to search before giving up. */
    const val SEARCH_SPAN = 100

    /**
     * The first port from [preferred] upward that is neither in [reserved] nor refused by
     * [isFree], or null if all [span] candidates are taken.
     *
     * [reserved] is the ports this process has already handed to a session. [isFree] alone would
     * catch most of them, since adb holds every live forward's listener, but not one that adb has
     * dropped with a briefly offline device while its window still expects to re-forward it.
     */
    fun allocate(
        reserved: Set<Int>,
        preferred: Int = DEFAULT_PORT,
        span: Int = SEARCH_SPAN,
        isFree: (Int) -> Boolean = ::isLoopbackPortFree,
    ): Int? = (preferred until (preferred + span).coerceAtMost(MAX_PORT + 1))
        .firstOrNull { it !in reserved && isFree(it) }

    /**
     * Whether nothing is listening on loopback [port]. A bind probe rather than `adb forward
     * --list`, because the listeners that collide are not all adb's — the iOS Simulator case is
     * a plain socket. `reuseAddress` is off because on macOS it lets the probe bind 127.0.0.1
     * alongside a listener on the wildcard address, reporting a taken port as free.
     */
    fun isLoopbackPortFree(port: Int): Boolean = try {
        ServerSocket().use { socket ->
            socket.reuseAddress = false
            socket.bind(InetSocketAddress(InetAddress.getLoopbackAddress(), port))
        }
        true
    } catch (_: IOException) {
        false
    }

    private const val MAX_PORT = 65535
}
