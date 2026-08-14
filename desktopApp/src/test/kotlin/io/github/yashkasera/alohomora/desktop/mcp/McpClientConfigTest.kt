package io.github.yashkasera.alohomora.desktop.mcp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class McpClientConfigTest {

    @Test
    fun `claude code gets a CLI command and an http config`() {
        assertEquals(
            "claude mcp add --transport http alohomora http://127.0.0.1:53900/mcp",
            McpClientConfig.command(McpClient.CLAUDE_CODE, 53900),
        )
        val config = McpClientConfig.config(McpClient.CLAUDE_CODE, 53900)
        assertTrue("\"type\": \"http\"" in config)
        assertTrue("http://127.0.0.1:53900/mcp" in config)
    }

    @Test
    fun `cursor is url-only with no command`() {
        assertNull(McpClientConfig.command(McpClient.CURSOR, 7000))
        val config = McpClientConfig.config(McpClient.CURSOR, 7000)
        assertTrue("http://127.0.0.1:7000/mcp" in config)
        assertFalse("\"type\"" in config, "Cursor auto-detects; no type field")
    }

    @Test
    fun `claude desktop uses the mcp-remote bridge`() {
        assertNull(McpClientConfig.command(McpClient.CLAUDE_DESKTOP, 53900))
        val config = McpClientConfig.config(McpClient.CLAUDE_DESKTOP, 53900)
        assertTrue("mcp-remote" in config)
        assertTrue("npx" in config)
        assertTrue("http://127.0.0.1:53900/mcp" in config)
    }
}
