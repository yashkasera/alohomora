package io.github.yashkasera.alohomora.replay

/**
 * A [TrafficReplayHandler] that can send [ReplayRequest.bodyBase64] byte for byte.
 *
 * A marker, not a new member on [TrafficReplayHandler]: that interface reaches Swift as a protocol,
 * and a new requirement would stop every existing Swift conformer compiling. A handler that does
 * not implement this is never handed a binary body, and both consoles refuse such a replay up front.
 *
 * The built-in `okHttpReplayHandler`, `ktorReplayHandler` and Swift `urlSessionReplayHandler` all
 * implement it. A custom handler opts in by implementing it and sending the decoded bytes with
 * [ReplayRequest.contentType] untouched; a multipart body's boundary lives in that content type.
 *
 * ```kotlin
 * Alohomora.registerReplayHandler(BinaryReplayHandler { request -> sendThroughMyClient(request) })
 * ```
 *
 * Declared in `:alohomora` rather than beside [TrafficReplayHandler] in `:alohomora-common` for
 * the reason [ReplayOutcomes] is: the framework exports a common declaration only when an
 * `:alohomora` signature mentions it, and nothing would mention this one, so Swift could not
 * conform to it.
 */
fun interface BinaryReplayHandler : TrafficReplayHandler
