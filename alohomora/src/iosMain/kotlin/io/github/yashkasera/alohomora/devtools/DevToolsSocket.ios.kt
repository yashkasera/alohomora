package io.github.yashkasera.alohomora.devtools

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.posix.AF_INET
import platform.posix.INADDR_ANY
import platform.posix.SOCK_STREAM
import platform.posix.accept
import platform.posix.bind
import platform.posix.close
import platform.posix.htonl
import platform.posix.htons
import platform.posix.listen
import platform.posix.recv
import platform.posix.send
import platform.posix.socket
import platform.posix.sockaddr
import platform.posix.sockaddr_in
import platform.posix.socklen_t
import kotlin.concurrent.thread

internal actual class DevToolsSocket(
    private val fd: Int,
) {
    actual fun readExact(byteCount: Int): ByteArray? {
        val buffer = ByteArray(byteCount)
        var offset = 0
        while (offset < byteCount) {
            val read = buffer.usePinned { pinned ->
                recv(fd, pinned.addressOf(offset), (byteCount - offset).toULong(), 0)
            }
            if (read <= 0) return null
            offset += read.toInt()
        }
        return buffer
    }

    actual fun write(bytes: ByteArray) {
        var offset = 0
        while (offset < bytes.size) {
            val sent = bytes.usePinned { pinned ->
                send(fd, pinned.addressOf(offset), (bytes.size - offset).toULong(), 0)
            }
            if (sent <= 0) break
            offset += sent.toInt()
        }
    }

    actual fun close() {
        close(fd)
    }
}

internal actual class DevToolsTcpServer {
    private var serverFd: Int = -1
    private var acceptThread: Thread? = null

    actual fun start(port: Int, onClient: (DevToolsSocket) -> Unit) {
        if (serverFd != -1) return
        val fd = socket(AF_INET, SOCK_STREAM, 0)
        if (fd < 0) return
        memScoped {
            val address = alloc<sockaddr_in>()
            address.sin_family = AF_INET.convert()
            address.sin_port = htons(port.toUShort())
            address.sin_addr.s_addr = htonl(INADDR_ANY)
            val result = bind(
                fd,
                address.reinterpret<sockaddr>(),
                sizeOf<sockaddr_in>().toUInt()
            )
            if (result != 0) {
                close(fd)
                return
            }
        }
        if (listen(fd, 4) != 0) {
            close(fd)
            return
        }
        serverFd = fd
        acceptThread = thread(name = "AlohomoraDevToolsServer") {
            while (serverFd != -1) {
                val clientFd = memScoped {
                    val clientAddr = alloc<sockaddr>()
                    val addrLen = alloc<socklen_t>()
                    addrLen.value = sizeOf<sockaddr_in>().toUInt()
                    accept(serverFd, clientAddr.ptr, addrLen.ptr)
                }
                if (clientFd < 0) continue
                onClient(DevToolsSocket(clientFd))
            }
        }
    }

    actual fun stop() {
        if (serverFd != -1) {
            close(serverFd)
            serverFd = -1
        }
        acceptThread?.interrupt()
        acceptThread = null
    }
}
