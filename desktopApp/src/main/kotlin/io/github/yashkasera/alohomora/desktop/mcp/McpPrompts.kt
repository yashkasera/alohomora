package io.github.yashkasera.alohomora.desktop.mcp

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.GetPromptResult
import io.modelcontextprotocol.kotlin.sdk.types.PromptArgument
import io.modelcontextprotocol.kotlin.sdk.types.PromptMessage
import io.modelcontextprotocol.kotlin.sdk.types.Role
import io.modelcontextprotocol.kotlin.sdk.types.TextContent

/**
 * Canned MCP prompts that orchestrate the read tools into a debugging flow. A prompt is just a
 * pre-written user message telling the agent which tools to chain — cheap, and it keeps the "how to
 * debug with this data" knowledge in one place instead of every developer re-deriving it.
 *
 * Registered whether or not write tools are on; they only reference read tools.
 */
fun registerAlohomoraPrompts(server: Server) {
    server.addPrompt(
        name = "triage",
        description = "Prioritise what's wrong on the connected device right now.",
        arguments = emptyList(),
    ) { _ ->
        promptResult(
            "Triage the connected Alohomora device.",
            """
            You are debugging a running app through Alohomora's MCP tools.

            1. Call `get_attention` to get the current unviewed errors and failed traffic.
            2. Group duplicate errors (same exceptionTypeName + place) and count them.
            3. For the most significant items, pull detail: `get_error` for stack traces, `get_traffic`
               for failed requests, and `get_trace` when a request is part of a trace.
            4. Call `get_git_history` and note any recent commit that plausibly relates — but flag
               correlation as a hypothesis, not proof.
            5. Output a short ranked list: what's broken, how often, the likely cause, and the single
               next step for each. Lead with the highest-impact item.
            """.trimIndent(),
        )
    }

    server.addPrompt(
        name = "debug_request",
        description = "Explain why one captured request is failing and propose a fix.",
        arguments = listOf(
            PromptArgument(
                name = "id",
                description = "The traffic entry id (from list_traffic / get_attention).",
                required = true,
                title = null,
            ),
        ),
    ) { request ->
        val id = request.arguments?.get("id") ?: "the failing request"
        promptResult(
            "Debug request $id.",
            """
            Debug the captured request `$id` using Alohomora's MCP tools.

            1. `get_traffic` with id `$id` — read status, headers, request/response bodies, and the
               curl. Note any truncation flags.
            2. Look for correlated failures: `list_errors` around the same time, and if the request is
               part of a trace, `get_trace` to see which span carries the ERROR.
            3. If the failure looks state-dependent, inspect `query_database_table` / `get_cache_value`
               and `list_feature_flags`.
            4. Explain the root cause, then locate it in this repository's source and propose a concrete
               fix. Verify the fix against the actual code, not just the captured data.
            """.trimIndent(),
        )
    }

    server.addPrompt(
        name = "explain_screen",
        description = "Describe what a screen actually does on the network and in events.",
        arguments = listOf(
            PromptArgument(
                name = "name",
                description = "The screen or event name to focus on.",
                required = true,
                title = null,
            ),
        ),
    ) { request ->
        val name = request.arguments?.get("name") ?: "the screen"
        promptResult(
            "Explain $name.",
            """
            Explain what "$name" does at runtime, from Alohomora's captured data.

            1. `list_events` filtered by name `$name` to get the ordered user/system events.
            2. Correlate each event with `list_traffic` in the same time window to see the network
               calls it triggers.
            3. Describe the real behaviour as a short sequence: user action -> events -> requests ->
               effects, calling out anything surprising or undocumented.
            """.trimIndent(),
        )
    }
}

private fun promptResult(description: String, text: String): GetPromptResult =
    GetPromptResult(
        messages = listOf(PromptMessage(role = Role.User, content = TextContent(text))),
        description = description,
    )
