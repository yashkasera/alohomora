package io.github.yashkasera.alohomora.desktop.mcp

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull

/** A pending write confirmation awaiting the developer's Allow/Deny in the desktop UI. */
data class PendingConfirmation(
    val title: String,
    val message: String,
    private val deferred: CompletableDeferred<Boolean>,
) {
    fun resolve(approved: Boolean) {
        deferred.complete(approved)
    }
}

/**
 * Bridges a write tool running on the server coroutine to a one-off Allow/Deny dialog in the desktop
 * UI. The destructive `clear_captured` tool calls [confirm] and suspends until the developer clicks;
 * `Main.kt` observes [pending] and renders the dialog, calling [PendingConfirmation.resolve].
 *
 * App-scoped and single-slot: a second confirmation while one is pending is denied outright rather
 * than queued, because two overlapping "clear everything?" prompts is worse than making the agent
 * retry.
 */
class McpConfirmationBroker {
    private val _pending = MutableStateFlow<PendingConfirmation?>(null)
    val pending: StateFlow<PendingConfirmation?> = _pending.asStateFlow()

    /**
     * Publishes a confirmation and suspends for the developer's click.
     *
     * Bounded and self-freeing: if nobody answers within [TIMEOUT_MILLIS] (dialog missed, client
     * disconnected), it resolves to `false` (deny) and clears the slot. The `finally` also clears on
     * cancellation, so a dropped MCP request can never wedge the single slot and block later prompts.
     */
    suspend fun confirm(title: String, message: String): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        val request = PendingConfirmation(title, message, deferred)
        // Atomic claim of the single slot: `compareAndSet` is a real CAS, so two concurrent callers
        // cannot both observe null and overwrite each other. The loser is denied rather than queued.
        if (!_pending.compareAndSet(null, request)) return false
        return try {
            withTimeoutOrNull(TIMEOUT_MILLIS) { deferred.await() } ?: false
        } finally {
            _pending.value = null
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 120_000L
    }
}
