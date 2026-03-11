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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.io.EOFException

private object DevToolsSocketSelector {
    val manager: SelectorManager = SelectorManager(Dispatchers.IO)
}

class DevToolsSocket internal constructor(
    private val socket: Socket,
) {
    private val input: ByteReadChannel = socket.openReadChannel()
    private val output: ByteWriteChannel = socket.openWriteChannel(autoFlush = false)

    suspend fun readExact(byteCount: Int): ByteArray? {
        if (byteCount <= 0) return ByteArray(0)
        val buffer = ByteArray(byteCount)
        return try {
            input.readFully(buffer, 0, byteCount)
            buffer
        } catch (e: EOFException) {
            null
        }
    }

    suspend fun write(bytes: ByteArray) {
        if (bytes.isEmpty()) return
        output.writeFully(bytes, 0, bytes.size)
        output.flush()
    }

    fun close() {
        socket.close()
    }
}

class DevToolsTcpServer(
    private val selector: SelectorManager = DevToolsSocketSelector.manager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null

    fun start(port: Int, onClient: (DevToolsSocket) -> Unit) {
        if (serverSocket != null) return
        val server = try {
            runBlocking {
                aSocket(selector).tcp().bind(InetSocketAddress("0.0.0.0", port))
            }
        } catch (e: Throwable) {
            val message = e.message.orEmpty()
            if (message.contains("address already in use", ignoreCase = true) ||
                message.contains("eaddrinuse", ignoreCase = true)
            ) {
                println("Alohomora DevTools server not started; port $port already in use.")
            } else {
                println("Alohomora DevTools server failed to start on port $port: ${e.message}")
            }
            return
        }
        serverSocket = server
        acceptJob = scope.launch {
            while (true) {
                val socket = try {
                    server.accept()
                } catch (_: Throwable) {
                    break
                }
                onClient(DevToolsSocket(socket))
            }
        }
    }

    fun stop() {
        serverSocket?.close()
        serverSocket = null
        acceptJob?.cancel()
        acceptJob = null
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
        val socket = withTimeout(timeoutMillis) {
            aSocket(selector).tcp().connect(InetSocketAddress(host, port))
        }
        return DevToolsSocket(socket)
    }
}
