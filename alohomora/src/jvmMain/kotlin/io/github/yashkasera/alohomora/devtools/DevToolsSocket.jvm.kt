package io.github.yashkasera.alohomora.devtools

import java.net.BindException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

internal actual class DevToolsSocket(
    private val socket: Socket,
) {
    private val input = socket.getInputStream()
    private val output = socket.getOutputStream()

    actual fun readExact(byteCount: Int): ByteArray? {
        val buffer = ByteArray(byteCount)
        var offset = 0
        while (offset < byteCount) {
            val read = input.read(buffer, offset, byteCount - offset)
            if (read == -1) return null
            offset += read
        }
        return buffer
    }

    actual fun write(bytes: ByteArray) {
        output.write(bytes)
        output.flush()
    }

    actual fun close() {
        socket.close()
    }
}

internal actual class DevToolsTcpServer {
    private var serverSocket: ServerSocket? = null
    private var acceptThread: Thread? = null

    actual fun start(port: Int, onClient: (DevToolsSocket) -> Unit) {
        if (serverSocket != null) return
        val server = ServerSocket()
        server.reuseAddress = true
        try {
            server.bind(InetSocketAddress(port))
        } catch (e: BindException) {
            System.err.println("Alohomora DevTools server not started; port $port already in use.")
            server.close()
            return
        } catch (e: Exception) {
            System.err.println("Alohomora DevTools server failed to start on port $port: ${e.message}")
            server.close()
            return
        }
        serverSocket = server
        acceptThread = thread(name = "AlohomoraDevToolsServer") {
            while (!server.isClosed) {
                val socket = try {
                    server.accept()
                } catch (e: Exception) {
                    break
                }
                onClient(DevToolsSocket(socket))
            }
        }
    }

    actual fun stop() {
        serverSocket?.close()
        serverSocket = null
        acceptThread?.interrupt()
        acceptThread = null
    }
}
