package io.github.yashkasera.alohomora.desktop.mcp

import io.github.yashkasera.alohomora.desktop.FakeDevToolsRepository
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.NetworkRulesViewModel
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AlohomoraMcpServerTest {

    @Test
    fun `registry defaults to the only session and requires an id when several are open`() {
        val a = handle("a")
        val b = handle("b")
        val registry = DeviceSessionRegistry()

        registry.update(listOf(a))
        assertEquals("a", registry.resolve(null)?.deviceId, "one session: deviceId is optional")
        assertEquals("a", registry.resolve("a")?.deviceId)

        registry.update(listOf(a, b))
        assertNull(registry.resolve(null), "several sessions: an absent deviceId does not resolve")
        assertEquals("b", registry.resolve("b")?.deviceId)
        assertNull(registry.resolve("missing"))
    }

    @Test
    fun `server binds loopback, mounts the route, and rejects a non-loopback origin`() {
        val server = AlohomoraMcpServer(DeviceSessionRegistry(), "test")
        val port = freePort()
        server.start(port)
        try {
            awaitListening(port)

            // DNS-rebinding protection: a browser origin outside the loopback set is refused with 403.
            assertEquals(403, post(port, origin = "http://evil.example.com"))

            // A request without an Origin header (a CLI agent like Claude Code) reaches the MCP route
            // — some non-403, non-404 status — proving the route is mounted and loopback is allowed.
            val allowed = post(port, origin = null)
            assertTrue(
                allowed != 403 && allowed != 404,
                "route should be reachable on loopback, got $allowed",
            )
        } finally {
            server.stop()
        }
    }

    private fun handle(id: String): DeviceSessionHandle {
        val repo = FakeDevToolsRepository()
        return DeviceSessionHandle(
            deviceId = id,
            model = null,
            platform = null,
            devToolsRepository = repo,
            networkRulesViewModel = NetworkRulesViewModel(repo),
        )
    }

    private fun freePort(): Int = ServerSocket(0).use { it.localPort }

    private fun awaitListening(port: Int) {
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            runCatching {
                Socket().use { it.connect(InetSocketAddress("127.0.0.1", port), 200) }
                return
            }
            Thread.sleep(50)
        }
        error("MCP server did not start listening on $port")
    }

    /**
     * Sends the POST over a raw socket rather than HttpURLConnection: `Origin` is a restricted header
     * that HttpURLConnection silently drops, which would defeat the whole point of the origin check.
     */
    private fun post(port: Int, origin: String?): Int {
        val body = "{}"
        val request = buildString {
            append("POST /mcp HTTP/1.1\r\n")
            append("Host: 127.0.0.1:$port\r\n")
            if (origin != null) append("Origin: $origin\r\n")
            append("Content-Type: application/json\r\n")
            append("Accept: application/json, text/event-stream\r\n")
            append("Content-Length: ${body.toByteArray().size}\r\n")
            append("Connection: close\r\n\r\n")
            append(body)
        }
        Socket().use { socket ->
            socket.connect(InetSocketAddress("127.0.0.1", port), 1_000)
            socket.getOutputStream().apply { write(request.toByteArray()); flush() }
            val statusLine = socket.getInputStream().bufferedReader().readLine().orEmpty()
            // "HTTP/1.1 403 Forbidden" -> 403
            return statusLine.split(" ").getOrNull(1)?.toIntOrNull() ?: -1
        }
    }
}
