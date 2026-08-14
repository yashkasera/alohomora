package io.github.yashkasera.alohomora.desktop.data.ios

import java.io.File
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.SocketChannel

/** An iOS device as reported by usbmuxd. */
data class UsbmuxDevice(
    /** usbmuxd's numeric handle. Tunnels are opened against this, not the serial. */
    val deviceId: Int,
    val serialNumber: String,
    /** "USB" or "Network" (a Wi-Fi-paired device). */
    val connectionType: String,
) {
    val isUsb: Boolean get() = connectionType.equals("USB", ignoreCase = true)
}

/**
 * Client for macOS's usbmuxd — the iOS equivalent of `adb forward`.
 *
 * There is no adb on iOS. The mechanism Xcode, Safari Web Inspector and the RN/Flutter
 * tooling all use is `usbmuxd`, a daemon that multiplexes TCP connections to a device over
 * USB. It is reachable at `/var/run/usbmuxd` and ships with macOS as part of the Apple Mobile
 * Device service, so this needs no third-party dependency:
 *  - Unix domain sockets are native to the JDK (16+); this project targets 21.
 *  - usbmuxd replies with **XML** plists (verified against the live socket), so [Plist]
 *    suffices and no binary-plist library is required.
 *
 * Note `xcrun devicectl` deliberately is not used for this: as of Xcode 26 it exposes no
 * port-forwarding subcommand, so Apple's supported CLI cannot tunnel a TCP port at all.
 *
 * Wire format: a 16-byte little-endian header (length, version, message type, tag) followed
 * by an XML plist body. `version = 1` and `type = 8` select the modern "plist protocol".
 */
class UsbmuxClient(
    private val socketPath: String = DEFAULT_SOCKET_PATH,
) {

    /** True when this host has a usbmuxd socket at all (i.e. is a Mac with the service). */
    fun isAvailable(): Boolean = File(socketPath).exists()

    /**
     * Lists attached iOS devices.
     *
     * An empty list is a legitimate answer meaning "nothing plugged in", not an error.
     */
    fun listDevices(): List<UsbmuxDevice> {
        if (!isAvailable()) return emptyList()
        return openChannel().use { channel ->
            val reply = request(
                channel,
                mapOf(
                    "MessageType" to "ListDevices",
                    "ClientVersionString" to CLIENT_VERSION,
                    "ProgName" to PROG_NAME,
                ),
            ) ?: return emptyList()

            @Suppress("UNCHECKED_CAST")
            val list = (reply["DeviceList"] as? List<Any?>).orEmpty()
            list.mapNotNull { entry ->
                val properties = ((entry as? Map<*, *>)?.get("Properties") as? Map<*, *>)
                    ?: return@mapNotNull null
                val deviceId = (properties["DeviceID"] as? Long)?.toInt() ?: return@mapNotNull null
                UsbmuxDevice(
                    deviceId = deviceId,
                    serialNumber = properties["SerialNumber"] as? String ?: return@mapNotNull null,
                    connectionType = properties["ConnectionType"] as? String ?: "USB",
                )
            }
        }
    }

    /**
     * Opens a tunnel to [port] on the device identified by [deviceId].
     *
     * On success the returned channel is a *raw byte pipe* to the device port — the plist
     * framing applies only to the handshake, after which usbmuxd steps out of the way. The
     * caller owns the channel and must close it.
     *
     * @return the connected channel, or null if usbmuxd refused (nothing listening on the
     *   device, device unplugged, or the app not running).
     */
    fun connect(deviceId: Int, port: Int): SocketChannel? {
        if (!isAvailable()) return null
        val channel = openChannel()
        return try {
            val reply = request(
                channel,
                mapOf(
                    "MessageType" to "Connect",
                    "ClientVersionString" to CLIENT_VERSION,
                    "ProgName" to PROG_NAME,
                    "DeviceID" to deviceId,
                    // The classic usbmuxd trap: PortNumber is a 16-bit value in NETWORK byte
                    // order, even though every surrounding field is little-endian. Passing the
                    // port host-ordered "works" for palindromic ports and fails for all others.
                    "PortNumber" to port.toNetworkOrderShort(),
                ),
            )
            val resultCode = (reply?.get("Number") as? Long)?.toInt()
            if (resultCode == RESULT_OK) {
                channel
            } else {
                channel.close()
                null
            }
        } catch (e: Exception) {
            runCatching { channel.close() }
            null
        }
    }

    /** Reinterprets [this] as a big-endian 16-bit value carried in an int field. */
    private fun Int.toNetworkOrderShort(): Int = ((this and 0xFF) shl 8) or ((this shr 8) and 0xFF)

    private fun openChannel(): SocketChannel =
        SocketChannel.open(StandardProtocolFamily.UNIX).apply {
            connect(UnixDomainSocketAddress.of(socketPath))
        }

    private fun request(channel: SocketChannel, payload: Map<String, Any?>): Map<*, *>? {
        val body = Plist.encode(payload)
        val header = ByteBuffer.allocate(HEADER_LENGTH).order(ByteOrder.LITTLE_ENDIAN).apply {
            putInt(HEADER_LENGTH + body.size)
            putInt(PROTOCOL_VERSION)
            putInt(MESSAGE_TYPE_PLIST)
            putInt(TAG)
            flip()
        }
        channel.writeFully(header)
        channel.writeFully(ByteBuffer.wrap(body))

        val responseHeader = channel.readFully(HEADER_LENGTH) ?: return null
        responseHeader.order(ByteOrder.LITTLE_ENDIAN)
        val totalLength = responseHeader.getInt(0)
        if (totalLength !in HEADER_LENGTH..MAX_REPLY_BYTES) return null
        val responseBody = channel.readFully(totalLength - HEADER_LENGTH) ?: return null
        return Plist.decode(responseBody.array()) as? Map<*, *>
    }

    private fun SocketChannel.writeFully(buffer: ByteBuffer) {
        while (buffer.hasRemaining()) {
            if (write(buffer) < 0) error("usbmuxd socket closed while writing")
        }
    }

    private fun SocketChannel.readFully(byteCount: Int): ByteBuffer? {
        val buffer = ByteBuffer.allocate(byteCount)
        while (buffer.hasRemaining()) {
            if (read(buffer) < 0) return null
        }
        buffer.flip()
        return buffer
    }

    companion object {
        const val DEFAULT_SOCKET_PATH = "/var/run/usbmuxd"

        private const val HEADER_LENGTH = 16
        private const val PROTOCOL_VERSION = 1
        private const val MESSAGE_TYPE_PLIST = 8
        private const val TAG = 1
        private const val RESULT_OK = 0

        /** Guards against a hostile or corrupt length field, as on the DevTools protocol. */
        private const val MAX_REPLY_BYTES = 4 * 1024 * 1024

        private const val CLIENT_VERSION = "alohomora-devtools/1.0"
        private const val PROG_NAME = "Alohomora"
    }
}
