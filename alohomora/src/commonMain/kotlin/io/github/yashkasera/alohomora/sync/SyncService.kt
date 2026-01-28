package io.github.yashkasera.alohomora.sync

import io.github.yashkasera.alohomora.domain.repository.LogRepository
import io.github.yashkasera.alohomora.domain.repository.NetworkRepository
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*

// Simple data class for transport (can be expanded)
@kotlinx.serialization.Serializable
data class SyncPacket(
    val type: String, // "LOG", "NETWORK", "EVENT"
    val payload: String // JSON of the entity
)

internal class SyncService(
    private val logRepository: LogRepository,
    private val networkRepository: NetworkRepository
) {
    private val client = HttpClient {
        install(WebSockets)
    }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isConnected = false

    fun connect(url: String) {
        if (isConnected) return
        scope.launch {
            try {
                client.webSocket(url) {
                    isConnected = true
                    val sendJob = launch {
                        // Observe Repos and send data
                        // NOTE: In a real app we'd want to only send *new* items or diffs.
                        // For this demo, we can just send "Hello" or stream logs as they come.
                        // Implementing full state sync is complex, so we'll start with basic "Log Stream".
/*
                        logRepository.getAllLogs().collect { logs ->
                            if (logs.isNotEmpty()) {
                                val latest = logs.first() // Just send the latest for now or all?
                                // Let's just send the COUNT for now to prove connectivity
                                send(Frame.Text("LOG_COUNT:${logs.size}"))
                            }
                        }*/
                    }

                    try {
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                println("Received: ${frame.readText()}")
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        sendJob.cancel()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isConnected = false
            }
        }
    }
}
