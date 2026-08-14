package io.github.yashkasera.alohomora.desktop.mcp

import io.github.yashkasera.alohomora.desktop.FakeDevToolsRepository
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.NetworkRulesViewModel
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * End-to-end MCP protocol tests: start the real server and drive it over HTTP through the actual
 * handshake (initialize -> initialized -> tools/list / prompts/list / tools/call). This is the only
 * layer that proves the SDK registration, the write-tool gating, and a tool call all round-trip on
 * the wire; the pure-data tests cover the projections beneath.
 */
class AlohomoraMcpServerHttpTest {

    @Test
    fun `start moves to listening, stop back to stopped`() {
        val server = AlohomoraMcpServer(DeviceSessionRegistry(), "test")
        val port = freePort()
        try {
            assertEquals(McpServerStatus.Stopped, server.status.value)
            server.start(port)
            awaitListening(port)
            val status = server.status.value
            assertTrue(
                status is McpServerStatus.Listening && status.port == port,
                "expected Listening($port), was $status",
            )
        } finally {
            server.stop()
        }
        assertEquals(McpServerStatus.Stopped, server.status.value)
    }

    @Test
    fun `read tools are listed and write tools are hidden when writes are off`() =
        withServer(writeEnabled = false, devices = listOf("dev-1")) { client ->
            val names = client.toolNames()
            // A sample of the read surface is present…
            listOf(
                "list_devices",
                "get_attention",
                "list_traffic",
                "get_trace",
                "get_build_metadata",
            )
                .forEach { assertTrue(it in names, "read tool $it should be listed") }
            // …and no write tool leaks when the toggle is off.
            listOf(
                "replay_traffic",
                "set_mock_rules",
                "clear_mock_rules",
                "set_throttle",
                "clear_captured",
                "create_mock_from_traffic",
                "run_adb_command",
            )
                .forEach {
                    assertFalse(
                        it in names,
                        "write tool $it must be hidden when writes are off",
                    )
                }
            assertEquals(18, names.size, "18 read-only tools when writes are off")
        }

    @Test
    fun `write tools appear only when writes are enabled`() =
        withServer(writeEnabled = true) { client ->
            val names = client.toolNames()
            listOf(
                "replay_traffic",
                "list_mock_rules",
                "set_mock_rules",
                "clear_mock_rules",
                "get_throttle",
                "set_throttle",
                "clear_captured",
                "create_mock_from_traffic",
                "run_adb_command",
            )
                .forEach {
                    assertTrue(
                        it in names,
                        "write tool $it should be listed when writes are on",
                    )
                }
            assertEquals(27, names.size, "18 read + 9 write tools when writes are on")
        }

    @Test
    fun `the canned prompts are listed`() = withServer(writeEnabled = false) { client ->
        assertEquals(setOf("triage", "debug_request", "explain_screen"), client.promptNames())
    }

    @Test
    fun `list_devices round-trips the connected device`() =
        withServer(writeEnabled = false, devices = listOf("dev-1")) { client ->
            val text = client.callToolText("list_devices", "{}")
            val devices = Json.parseToJsonElement(text).jsonArray
            assertEquals("dev-1", devices.single().jsonObject["deviceId"]!!.jsonPrimitive.content)
        }

    // --- harness ----------------------------------------------------------------------------------

    private fun withServer(
        writeEnabled: Boolean,
        devices: List<String> = emptyList(),
        block: (McpHttpClient) -> Unit,
    ) {
        val registry = DeviceSessionRegistry()
        registry.update(
            devices.map { id ->
                val repo = FakeDevToolsRepository()
                DeviceSessionHandle(id, "model", "ANDROID", repo, NetworkRulesViewModel(repo))
            },
        )
        val server = AlohomoraMcpServer(registry, "test", writeEnabled = { writeEnabled })
        val port = freePort()
        server.start(port)
        try {
            awaitListening(port)
            val client = McpHttpClient(port).also { it.initialize() }
            block(client)
        } finally {
            server.stop()
        }
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
}

/**
 * A minimal Streamable-HTTP MCP client for the tests: the server runs with `enableJsonResponse`, so
 * POST responses come back as plain JSON we can parse directly. Just enough to do the handshake and a
 * couple of RPCs — not a general client.
 */
private class McpHttpClient(port: Int) {
    private val endpoint = URI("http://127.0.0.1:$port/mcp")
    private val http = HttpClient.newHttpClient()
    private var sessionId: String? = null

    fun initialize() {
        val response = post(
            """{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"test","version":"1"}}}""",
        )
        sessionId = response.headers().firstValue("mcp-session-id").orElse(null)
            ?: error("server did not return an Mcp-Session-Id on initialize")
        // Required before the server will serve requests.
        post("""{"jsonrpc":"2.0","method":"notifications/initialized"}""")
    }

    fun toolNames(): Set<String> =
        rpc("tools/list")["result"]!!.jsonObject["tools"]!!.jsonArray
            .map { it.jsonObject["name"]!!.jsonPrimitive.content }.toSet()

    fun promptNames(): Set<String> =
        rpc("prompts/list")["result"]!!.jsonObject["prompts"]!!.jsonArray
            .map { it.jsonObject["name"]!!.jsonPrimitive.content }.toSet()

    fun callToolText(name: String, arguments: String): String =
        rpc(
            "tools/call",
            """{"name":"$name","arguments":$arguments}""",
        )["result"]!!.jsonObject["content"]!!
            .jsonArray[0].jsonObject["text"]!!.jsonPrimitive.content

    private fun rpc(method: String, params: String = "{}") =
        Json.parseToJsonElement(
            post("""{"jsonrpc":"2.0","id":9,"method":"$method","params":$params}""").body(),
        ).jsonObject

    private fun post(body: String): HttpResponse<String> {
        val builder = HttpRequest.newBuilder(endpoint)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json, text/event-stream")
        sessionId?.let { builder.header("mcp-session-id", it) }
        builder.POST(HttpRequest.BodyPublishers.ofString(body))
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString())
    }
}
