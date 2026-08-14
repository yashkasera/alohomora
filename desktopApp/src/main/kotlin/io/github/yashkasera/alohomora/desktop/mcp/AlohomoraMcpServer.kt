package io.github.yashkasera.alohomora.desktop.mcp

import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcpStreamableHttp
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What the Settings status line reports about the MCP server. */
sealed interface McpServerStatus {
    data object Stopped : McpServerStatus
    data class Listening(val port: Int, val connectedClients: Int) : McpServerStatus
    data class Error(val message: String) : McpServerStatus
}

/**
 * The read-only MCP server: the desktop app's only inbound listener.
 *
 * App-scoped — one instance for the whole application regardless of how many device windows are
 * open — so `deviceId` is a tool argument resolved through the shared [DeviceSessionRegistry] rather
 * than a per-window server. Bound to loopback with the SDK's DNS-rebinding protection on (Host and
 * Origin validated against `localhost`/`127.0.0.1`/`[::1]`), matching the `adb forward` trust
 * boundary; nothing here can mutate device state.
 *
 * [start] and [stop] are idempotent and driven from application scope by the Settings toggle.
 */
class AlohomoraMcpServer(
    private val registry: DeviceSessionRegistry,
    private val serverVersion: String,
    /** Read at each session create, so flipping the Settings toggle affects the next connection. */
    private val writeEnabled: () -> Boolean = { false },
    /** Gates the one destructive write tool behind a desktop Allow/Deny dialog. */
    private val confirmation: McpConfirmationBroker = McpConfirmationBroker(),
) {
    private val _status = MutableStateFlow<McpServerStatus>(McpServerStatus.Stopped)
    val status: StateFlow<McpServerStatus> = _status.asStateFlow()

    private var engine: EmbeddedServer<*, *>? = null
    private var currentPort: Int = 0
    private val activeClients = AtomicInteger(0)

    @Synchronized
    fun start(port: Int) {
        stop()
        activeClients.set(0)
        currentPort = port
        try {
            engine = embeddedServer(CIO, host = LOOPBACK_HOST, port = port) {
                // DNS-rebinding protection on: Host defaults to the loopback set, and allowedOrigins
                // is set explicitly (compared by hostname only) so a browser Origin outside loopback
                // is refused with 403. Passing it is required — a null allowedOrigins skips Origin
                // validation entirely. Origin-less requests (a CLI agent like Claude Code) are still
                // allowed. Mounts at /mcp.
                mcpStreamableHttp(allowedOrigins = LOOPBACK_ORIGINS) {
                    buildServer()
                }
            }.also { it.start(wait = false) }
            _status.value = McpServerStatus.Listening(port, 0)
        } catch (t: Throwable) {
            engine = null
            _status.value = McpServerStatus.Error(t.message ?: "Failed to start MCP server")
        }
    }

    @Synchronized
    fun stop() {
        engine?.let { runCatching { it.stop(STOP_GRACE_MILLIS, STOP_TIMEOUT_MILLIS) } }
        engine = null
        activeClients.set(0)
        // Always land on Stopped: a stale start() error must not survive a disable/shutdown. Any new
        // failure is surfaced by the next start().
        _status.value = McpServerStatus.Stopped
    }

    /**
     * Builds a fresh [Server] per client session: read tools + prompts always, write tools only when
     * the developer has opted in. Reading [writeEnabled] here (not at construction) means the Settings
     * toggle takes effect on the next connection; a live agent reconnects to see the write tools.
     *
     * A new instance per session keeps sessions from sharing mutable protocol state.
     */
    private fun buildServer(): Server {
        val server = Server(
            serverInfo = Implementation(name = SERVER_NAME, version = serverVersion),
            options = ServerOptions(
                capabilities = ServerCapabilities(
                    tools = ServerCapabilities.Tools(listChanged = false),
                    prompts = ServerCapabilities.Prompts(listChanged = false),
                ),
            ),
        )
        registerAlohomoraTools(server, registry, serverVersion)
        registerAlohomoraPrompts(server)
        if (writeEnabled()) {
            registerAlohomoraWriteTools(server, registry, confirmation)
        }
        onClientConnected()
        server.onClose { onClientDisconnected() }
        return server
    }

    private fun onClientConnected() {
        val count = activeClients.incrementAndGet()
        updateClientCount(count)
    }

    private fun onClientDisconnected() {
        val count = activeClients.updateAndGet { (it - 1).coerceAtLeast(0) }
        updateClientCount(count)
    }

    private fun updateClientCount(count: Int) {
        val current = _status.value
        if (current is McpServerStatus.Listening) {
            _status.value = current.copy(connectedClients = count)
        }
    }

    companion object {
        private const val LOOPBACK_HOST = "127.0.0.1"
        private const val SERVER_NAME = "alohomora"

        // Compared by hostname only (scheme/port ignored), so these cover any loopback origin a local
        // browser-based agent might send. A CLI agent sends no Origin and is allowed regardless.
        private val LOOPBACK_ORIGINS = listOf("http://localhost", "http://127.0.0.1", "http://[::1]")
        private const val STOP_GRACE_MILLIS = 200L
        private const val STOP_TIMEOUT_MILLIS = 1_000L

        /** The endpoint an agent points at. Path matches the SDK's `mcpStreamableHttp` default. */
        fun endpointUrl(port: Int): String = "http://$LOOPBACK_HOST:$port/mcp"
    }
}
