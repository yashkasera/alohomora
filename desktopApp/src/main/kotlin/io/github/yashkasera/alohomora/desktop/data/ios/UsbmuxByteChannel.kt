package io.github.yashkasera.alohomora.desktop.data.ios

import io.github.yashkasera.alohomora.devtools.DevToolsByteChannel
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * [DevToolsByteChannel] over an established usbmuxd tunnel.
 *
 * After `UsbmuxClient.connect` succeeds, usbmuxd steps out of the way and the Unix domain
 * socket becomes a raw byte pipe to the port on the device — so from here on this behaves
 * exactly like a TCP socket, and the DevTools framing above it is unchanged.
 *
 * The channel is blocking, so every operation is confined to [Dispatchers.IO].
 */
class UsbmuxByteChannel(
    private val channel: SocketChannel,
) : DevToolsByteChannel {

    override suspend fun readFully(dest: ByteArray, offset: Int, length: Int): Boolean =
        withContext(Dispatchers.IO) {
            val buffer = ByteBuffer.wrap(dest, offset, length)
            while (buffer.hasRemaining()) {
                val read = try {
                    channel.read(buffer)
                } catch (e: Exception) {
                    return@withContext false
                }
                // -1 is a clean EOF (device unplugged, or the app was suspended/killed — the
                // normal way an iOS session ends, since iOS freezes backgrounded apps).
                if (read < 0) return@withContext false
            }
            true
        }

    override suspend fun write(bytes: ByteArray) {
        withContext(Dispatchers.IO) {
            val buffer = ByteBuffer.wrap(bytes)
            while (buffer.hasRemaining()) {
                if (channel.write(buffer) < 0) error("usbmux tunnel closed while writing")
            }
        }
    }

    override fun close() {
        runCatching { channel.close() }
    }
}
