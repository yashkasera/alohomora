package io.github.yashkasera.alohomora.desktop.data.devtools

import io.github.yashkasera.alohomora.common.DevToolsMessage
import io.github.yashkasera.alohomora.common.DevToolsProtocol
import io.github.yashkasera.alohomora.common.EnvelopeRead
import io.github.yashkasera.alohomora.desktop.data.ios.UsbmuxByteChannel
import io.github.yashkasera.alohomora.desktop.data.ios.UsbmuxClient
import io.github.yashkasera.alohomora.devtools.DevToolsSocket
import io.github.yashkasera.alohomora.devtools.DevToolsTcpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

open class DevToolsRemoteDataSource(
    private val tcpClient: DevToolsTcpClient = DevToolsTcpClient(),
    private val usbmux: UsbmuxClient = UsbmuxClient(),
) {
    /**
     * Connects over TCP.
     *
     * Covers Android (through `adb forward`) and the iOS Simulator, which needs no tunnel at
     * all because it shares the host's network stack — a server bound to 127.0.0.1 inside the
     * simulator *is* the host's 127.0.0.1.
     */
    open suspend fun connect(host: String, port: Int): DevToolsSocket {
        return tcpClient.connect(host, port, timeoutMillis = CONNECT_TIMEOUT_MILLIS)
    }

    /**
     * Connects to a physical iOS device through a usbmuxd tunnel.
     *
     * This is the `adb forward` equivalent for iOS. Unlike adb there is no host-side port to
     * reserve: usbmuxd hands back a socket already wired to [port] on the device, so nothing
     * needs allocating or tearing down afterwards.
     *
     * @param usbmuxDeviceId usbmuxd's numeric device handle (not the serial).
     */
    open suspend fun connectOverUsbmux(usbmuxDeviceId: Int, port: Int): DevToolsSocket =
        withContext(Dispatchers.IO) {
            val channel = usbmux.connect(usbmuxDeviceId, port)
                ?: error(
                    "Could not open a usbmux tunnel to port $port. Is the app running and " +
                        "is the DevTools server enabled? (iOS suspends backgrounded apps.)",
                )
            DevToolsSocket.over(UsbmuxByteChannel(channel))
        }

    /**
     * Pumps frames until the stream stops, and returns why.
     *
     * The reason is the return value because the caller has to choose between reconnecting and
     * giving up: an ordinary drop is worth retrying, a wire-version mismatch never is.
     */
    open suspend fun processConnection(
        connection: DevToolsSocket,
        onMessage: suspend (DevToolsMessage) -> Unit,
    ): EnvelopeRead {
        while (true) {
            when (val read = DevToolsProtocol.readEnvelope(connection)) {
                is EnvelopeRead.Message -> onMessage(read.message)
                else -> return read
            }
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 3000L
    }
}
