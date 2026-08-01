package io.github.yashkasera.alohomora.replay

import io.ktor.client.HttpClient

/**
 * No-op mirror of `:alohomora`'s `ktorReplayHandler`.
 *
 * Returns a handler that reports failure rather than one that sends. A release build has no console to
 * trigger a replay from, so this is unreachable; making it a real sender anyway would ship a code path
 * to production whose only purpose is re-issuing arbitrary requests.
 */
@Suppress("UNUSED_PARAMETER")
fun ktorReplayHandler(client: HttpClient): TrafficReplayHandler = TrafficReplayHandler {
    ReplayOutcome.Failed("Replay is not available in release builds.")
}
