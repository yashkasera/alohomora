package io.github.yashkasera.alohomora.replay

import io.github.yashkasera.alohomora.replay.ReplayMarker.HEADER

/**
 * How a replayed request identifies itself to Alohomora's capture layer.
 *
 * A replay is executed by the *host app's* client, so the library has no handle on the call and
 * cannot tell the resulting trace apart from organic traffic. Without a marker every replay shows up
 * in the console as an unexplained duplicate of the request above it.
 *
 * The handlers Alohomora ships do **not** use [HEADER]. `okHttpReplayHandler` carries the marker in
 * an OkHttp request tag and `ktorReplayHandler` in a Ktor attribute, both of which stay client-side
 * and never reach the wire. That is deliberate: see [HEADER] for what goes wrong otherwise.
 */
object ReplayMarker {

    /**
     * Last-resort marker for a custom handler with no out-of-band channel of its own.
     *
     * **Prefer a tag or attribute.** Capture strips this header before the request goes out, which
     * is safe only when Alohomora's interceptor runs *before* whatever signs the request. Get that
     * order wrong and the signature is computed over a header that is then removed, so every replay
     * fails authentication for a reason nothing on screen explains.
     *
     * If you must use it, register Alohomora's interceptor ahead of your signing interceptor.
     */
    const val HEADER: String = "X-Alohomora-Replay-Of"
}
