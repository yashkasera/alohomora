package io.github.yashkasera.alohomora.replay

/**
 * OkHttp request tag identifying a call as the replay of [sourceTraceId].
 *
 * A tag rather than a header because a tag never reaches the wire. A header would have to be
 * stripped by Alohomora's interceptor on the way out, and that only works if the interceptor runs
 * before whatever signs the request — otherwise the signature covers a header that is then removed
 * and every replay fails auth for a reason nothing on screen explains. A tag sidesteps the ordering
 * question entirely.
 *
 * Set it yourself if you write a custom OkHttp handler instead of using `okHttpReplayHandler`:
 * ```kotlin
 * Request.Builder().tag(ReplayTag::class.java, ReplayTag(request.sourceTraceId))
 * ```
 */
data class ReplayTag(val sourceTraceId: String)
