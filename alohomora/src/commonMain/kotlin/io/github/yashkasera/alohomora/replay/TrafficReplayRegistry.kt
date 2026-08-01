package io.github.yashkasera.alohomora.replay

import co.touchlab.kermit.Logger
import kotlin.concurrent.Volatile

/**
 * Holds the single [TrafficReplayHandler] the host app registered, and runs replays through it.
 *
 * Kept as an object rather than a Koin binding because both consoles need it and neither owns it:
 * the mobile console resolves it from a ViewModel, and the desktop drives it over TCP from
 * `DevToolsRuntime`. Registration also happens during app startup, before Koin is guaranteed to be
 * up, so a container lookup would be a race.
 */
internal object TrafficReplayRegistry {

    @Volatile
    private var handler: TrafficReplayHandler? = null

    /** True when the host app has supplied a handler, so replay can be offered at all. */
    val isSupported: Boolean get() = handler != null

    fun register(handler: TrafficReplayHandler) {
        this.handler = handler
    }

    fun clear() {
        handler = null
    }

    /**
     * Runs [request] through the registered handler.
     *
     * Every failure path returns [ReplayOutcome.Failed] rather than throwing. A replay is triggered
     * from a debug console, often against a URL that was edited by hand, so a refused connection or
     * a malformed URL is an expected outcome that belongs on screen — not an exception that takes
     * down the console or the DevTools connection with it.
     */
    suspend fun replay(request: ReplayRequest): ReplayOutcome {
        val handler = handler ?: return ReplayOutcome.Failed(NO_HANDLER)
        return try {
            handler.replay(request)
        } catch (e: Throwable) {
            Logger.d { "[Alohomora] Replay handler threw: ${e.message}" }
            ReplayOutcome.Failed(e.message ?: e::class.simpleName ?: "Replay failed")
        }
    }

    const val NO_HANDLER: String =
        "No replay handler registered. Call Alohomora.registerReplayHandler(...) at startup so " +
            "replays go through your own HTTP client."
}
