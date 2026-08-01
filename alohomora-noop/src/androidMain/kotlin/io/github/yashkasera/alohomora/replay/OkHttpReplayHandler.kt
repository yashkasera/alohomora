package io.github.yashkasera.alohomora.replay

import okhttp3.OkHttpClient

/**
 * No-op mirror of `:alohomora`'s `okHttpReplayHandler`.
 *
 * Returns a handler that reports failure rather than one that sends the request. A release build
 * has no console to trigger a replay from, so this is unreachable; making it a sender anyway would
 * put a code path in production whose only purpose is to re-issue arbitrary requests.
 */
@Suppress("UNUSED_PARAMETER")
fun okHttpReplayHandler(client: OkHttpClient): TrafficReplayHandler = TrafficReplayHandler {
    ReplayOutcome.Failed("Replay is not available in release builds.")
}
