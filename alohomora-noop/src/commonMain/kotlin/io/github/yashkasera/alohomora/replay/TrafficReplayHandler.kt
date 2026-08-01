package io.github.yashkasera.alohomora.replay

/**
 * No-op mirror of `:alohomora`'s replay contract.
 *
 * Duplicated rather than shared because `:alohomora-noop` deliberately does not depend on
 * `:alohomora-common` — the same reason `CustomScreenPlugin` is duplicated here. What matters for a
 * consumer compiling one call site against both artifacts is the fully qualified name and shape,
 * and those are identical. **Keep in lockstep with the real declarations.**
 */
data class ReplayRequest(
    val sourceTraceId: String,
    val method: String,
    val url: String,
    val headers: Map<String, List<String>> = emptyMap(),
    val body: String? = null,
    val contentType: String? = null,
)

/** No-op mirror of `:alohomora`'s `ReplayOutcome`. */
sealed interface ReplayOutcome {
    data class Sent(val traceId: String? = null) : ReplayOutcome
    data class Failed(val reason: String) : ReplayOutcome
}

/**
 * No-op mirror of `:alohomora`'s `TrafficReplayHandler`.
 *
 * A release build registers one and it is never invoked, so the app's client is never reached from
 * here. R8 removes both the registration call and the lambda.
 */
fun interface TrafficReplayHandler {
    suspend fun replay(request: ReplayRequest): ReplayOutcome
}
