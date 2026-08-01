package io.github.yashkasera.alohomora.desktop.domain.model

/**
 * What this window knows about replaying requests on the connected device.
 *
 * @property supported whether the app registered a replay handler. False also covers devices
 *   running a build that predates replay, so the action is hidden rather than offered and ignored.
 * @property inFlight source trace ids with a replay currently out on the wire. Keyed by trace
 *   rather than a single Boolean so replaying one request does not lock the action on every other.
 * @property errors the last failure per source trace id, cleared when that trace is replayed again.
 */
data class ReplayState(
    val supported: Boolean = false,
    val inFlight: Set<String> = emptySet(),
    val errors: Map<String, String> = emptyMap(),
) {
    fun isInFlight(sourceTraceId: String): Boolean = sourceTraceId in inFlight

    fun errorFor(sourceTraceId: String): String? = errors[sourceTraceId]
}
