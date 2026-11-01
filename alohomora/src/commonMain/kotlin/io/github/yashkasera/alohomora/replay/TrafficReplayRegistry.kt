package io.github.yashkasera.alohomora.replay

import co.touchlab.kermit.Logger
import kotlin.concurrent.Volatile
import kotlin.io.encoding.Base64

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

    /** True when the registered handler can send a binary body ([BinaryReplayHandler]). */
    val isBinarySupported: Boolean get() = handler is BinaryReplayHandler

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
     *
     * Headers are sanitised again here, on the device, because this is the one point both consoles
     * share. A desktop replay was prepared in the desktop process, which never sees the app's
     * [ReplayHeaders.additionalStripList], so its headers have only been through the built-in
     * list. [ReplayHeaders.sanitize] is idempotent, so the mobile path paying for it twice is
     * harmless — and a hand-edited header named in the policy is dropped either way, which is the
     * point of having one.
     *
     * A binary body is refused here for a handler that cannot send one, rather than handed over
     * for it to send [ReplayRequest.body] instead: that is null, so the server would get an empty
     * body and the failure would look like the server's. Its base64 is validated here too, once,
     * so a handler can decode it without guarding.
     */
    suspend fun replay(request: ReplayRequest): ReplayOutcome {
        val handler = handler ?: return ReplayOutcome.Failed(NO_HANDLER)
        val base64 = request.bodyBase64
        if (base64 != null) {
            if (handler !is BinaryReplayHandler) return ReplayOutcome.Failed(BINARY_UNSUPPORTED)
            if (runCatching { Base64.decode(base64) }.isFailure) {
                return ReplayOutcome.Failed(INVALID_BINARY_BODY)
            }
        }
        val sanitized = request.copy(
            headers = ReplayHeaders.sanitize(request.headers),
            // The bytes win. A request carrying both would otherwise send whichever one the
            // handler happened to read first.
            body = if (base64 != null) null else request.body,
        )
        return try {
            handler.replay(sanitized)
        } catch (e: Throwable) {
            Logger.d { "[Alohomora] Replay handler threw: ${e.message}" }
            ReplayOutcome.Failed(e.message ?: e::class.simpleName ?: "Replay failed")
        }
    }

    const val BINARY_UNSUPPORTED: String =
        "This request has a binary body, and the registered replay handler can only send text. " +
            "Register okHttpReplayHandler, ktorReplayHandler, urlSessionReplayHandler or a " +
            "BinaryReplayHandler."

    const val INVALID_BINARY_BODY: String = "The binary request body is not valid base64."

    const val NO_HANDLER: String =
        "No replay handler registered. Call Alohomora.registerReplayHandler(...) at startup so " +
            "replays go through your own HTTP client."
}
