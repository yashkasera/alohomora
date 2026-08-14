package io.github.yashkasera.alohomora.desktop.mcp

/** MCP clients we generate connect snippets for. */
enum class McpClient(val label: String) {
    CLAUDE_CODE("Claude Code"),
    CURSOR("Cursor"),
    CLAUDE_DESKTOP("Claude Desktop"),
}

/**
 * Copy-paste connection snippets for pointing an MCP client at this loopback server. Pure string
 * building from the port so it is trivially testable and the Settings UI just renders + copies.
 *
 * The server speaks Streamable HTTP at [AlohomoraMcpServer.endpointUrl]. Claude Code and Cursor accept
 * a url-based server natively; Claude Desktop has no native loopback-HTTP config and needs the
 * `mcp-remote` stdio bridge.
 */
object McpClientConfig {
    const val SERVER_NAME = "alohomora"

    fun endpoint(port: Int): String = AlohomoraMcpServer.endpointUrl(port)

    /** A one-line CLI command, where the client has one. Only Claude Code does. */
    fun command(client: McpClient, port: Int): String? = when (client) {
        McpClient.CLAUDE_CODE -> "claude mcp add --transport http $SERVER_NAME ${endpoint(port)}"
        McpClient.CURSOR, McpClient.CLAUDE_DESKTOP -> null
    }

    /** The JSON config snippet for the client's MCP config file. */
    fun config(client: McpClient, port: Int): String {
        val url = endpoint(port)
        return when (client) {
            McpClient.CLAUDE_CODE -> """
                {
                  "mcpServers": {
                    "$SERVER_NAME": {
                      "type": "http",
                      "url": "$url"
                    }
                  }
                }
            """.trimIndent()

            McpClient.CURSOR -> """
                {
                  "mcpServers": {
                    "$SERVER_NAME": {
                      "url": "$url"
                    }
                  }
                }
            """.trimIndent()

            McpClient.CLAUDE_DESKTOP -> """
                {
                  "mcpServers": {
                    "$SERVER_NAME": {
                      "command": "npx",
                      "args": ["-y", "mcp-remote", "$url"]
                    }
                  }
                }
            """.trimIndent()
        }
    }

    /** One-line hint about where the snippet goes / which mechanism it uses. */
    fun hint(client: McpClient): String = when (client) {
        McpClient.CLAUDE_CODE -> "Run the command, or add the snippet to .mcp.json."
        McpClient.CURSOR -> "Add to ~/.cursor/mcp.json (or .cursor/mcp.json in a project)."
        McpClient.CLAUDE_DESKTOP -> "Add to claude_desktop_config.json. Needs Node (npx) for the mcp-remote bridge."
    }
}
