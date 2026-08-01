package io.github.yashkasera.alohomora.devtools

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.ServerSocket
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readFully
import io.ktor.utils.io.writeFully
import kotlin.concurrent.Volatile
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.io.EOFException

private object DevToolsSocketSelector {
    val manager: SelectorManager = SelectorManager(Dispatchers.IO)
}

/** The only address the DevTools server may bind. See the bind call in [DevToolsTcpServer]. */
internal const val LOOPBACK_HOST = "127.0.0.1"

/**
 * The byte transport underneath the DevTools protocol.
 *
 * Abstracted because the protocol is transport-agnostic but reaching a device is not:
 *  - Android and iOS in-app servers, and the desktop talking to an `adb forward` or an iOS
 *    Simulator, are plain TCP ([KtorByteChannel]).
 *  - A physical iOS device is reached through a usbmuxd tunnel over USB, which is a Unix
 *    domain socket the desktop app has already handshaked — not a TCP socket at all.
 */
interface DevToolsByteChannel {
    /** Fills [length] bytes into [dest] at [offset]. Returns false on clean EOF. */
    suspend fun readFully(dest: ByteArray, offset: Int, length: Int): Boolean

    suspend fun write(bytes: ByteArray)

    fun close()
}

/** [DevToolsByteChannel] over a Ktor TCP socket. */
private class KtorByteChannel(private val socket: Socket) : DevToolsByteChannel {
    private val input: ByteReadChannel = socket.openReadChannel()
    private val output: ByteWriteChannel = socket.openWriteChannel(autoFlush = false)

    override suspend fun readFully(dest: ByteArray, offset: Int, length: Int): Boolean =
        try {
            input.readFully(dest, offset, length)
            true
        } catch (e: EOFException) {
            false
        }

    override suspend fun write(bytes: ByteArray) {
        output.writeFully(bytes, 0, bytes.size)
        output.flush()
    }

    override fun close() {
        socket.close()
    }
}

class DevToolsSocket internal constructor(
    private val channel: DevToolsByteChannel,
) {
    internal constructor(socket: Socket) : this(KtorByteChannel(socket))

    suspend fun readExact(byteCount: Int): ByteArray? {
        if (byteCount <= 0) return ByteArray(0)
        val buffer = ByteArray(byteCount)
        return if (channel.readFully(buffer, 0, byteCount)) buffer else null
    }

    suspend fun write(bytes: ByteArray) {
        if (bytes.isEmpty()) return
        channel.write(bytes)
    }

    fun close() {
        channel.close()
    }

    companion object {
        /**
         * Wraps a caller-supplied transport.
         *
         * Exists so the desktop app can drive an already-established usbmuxd tunnel, whose
         * bytes are a raw pipe rather than a Ktor socket.
         */
        fun over(channel: DevToolsByteChannel): DevToolsSocket = DevToolsSocket(channel)
    }
}

class DevToolsTcpServer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // @Volatile so reads/writes from different threads (main, IO) are consistent.
    @Volatile private var serverSocket: ServerSocket? = null
    @Volatile private var serverJob: Job? = null

    fun start(port: Int, onClient: (DevToolsSocket) -> Unit): Boolean {
        if (serverSocket != null) return true
        // All heavy work — SelectorManager init, TCP bind, accept loop — runs on the IO
        // dispatcher. On iOS, accessing SelectorManager or calling runBlocking from the main
        // thread before the run loop starts aborts the process; launching on scope avoids this.
        serverJob = scope.launch {
            val server = try {
                // Loopback ONLY, never 0.0.0.0. The desktop client always reaches this
                // through `adb forward`, which originates on the device's loopback, so
                // binding the wildcard address bought nothing and put the debug server —
                // captured request/response bodies, auth headers, arbitrary app-DB table
                // dumps — on the LAN behind a 4-digit OTP.
                aSocket(DevToolsSocketSelector.manager).tcp()
                    .bind(InetSocketAddress(LOOPBACK_HOST, port))
            } catch (e: Throwable) {
                val message = e.message.orEmpty()
                if (message.contains("address already in use", ignoreCase = true) ||
                    message.contains("eaddrinuse", ignoreCase = true)
                ) {
                    println("Alohomora DevTools server not started; port $port already in use.")
                } else {
                    println("Alohomora DevTools server failed to start on port $port: ${e.message}")
                }
                return@launch
            }
            serverSocket = server
            while (true) {
                val socket = try {
                    server.accept()
                } catch (_: Throwable) {
                    break
                }
                onClient(DevToolsSocket(socket))
            }
        }
        return true
    }

    fun stop() {
        serverJob?.cancel()
        serverJob = null
        serverSocket?.close()
        serverSocket = null
    }
}

class DevToolsTcpClient(
    private val selector: SelectorManager = DevToolsSocketSelector.manager,
) {
    suspend fun connect(
        host: String,
        port: Int,
        timeoutMillis: Long = 3000,
    ): DevToolsSocket {
        val socket = withTimeout(timeoutMillis.milliseconds) {
            aSocket(selector).tcp().connect(InetSocketAddress(host, port))
        }
        return DevToolsSocket(socket)
    }
}
