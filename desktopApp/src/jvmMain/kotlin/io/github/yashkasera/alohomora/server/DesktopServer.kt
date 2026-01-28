package io.github.yashkasera.alohomora.server

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import java.net.InetAddress
import java.time.Duration

class DesktopServer {
    private var server: NettyApplicationEngine? = null

    fun start(port: Int = 8080) {
        server = embeddedServer(Netty, port = port) {
            install(WebSockets) {
                pingPeriod = Duration.ofSeconds(15)
                timeout = Duration.ofSeconds(15)
                maxFrameSize = Long.MAX_VALUE
                masking = false
            }
            routing {
                webSocket("/") {
                    send("Welcome to Alohomora Desktop!")
                    incoming.consumeEach { frame ->
                        if (frame is Frame.Text) {
                            println("Client said: ${frame.readText()}")
                            // Here we would parse the packet and update the Desktop UI state
                        }
                    }
                }
            }
        }.start(wait = false)

        println("Server started on ${getLocalIp()}:$port")
    }

    fun stop() {
        server?.stop(1000, 2000)
    }

    fun getLocalIp(): String {
        return try {
            InetAddress.getLocalHost().hostAddress
        } catch (e: Exception) {
            "127.0.0.1"
        }
    }
}
