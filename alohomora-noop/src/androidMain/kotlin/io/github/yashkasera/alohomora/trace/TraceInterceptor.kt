package io.github.yashkasera.alohomora.trace

import okhttp3.Interceptor
import okhttp3.Response

/**
 * No-op mirror of `:alohomora`'s `TraceInterceptor`.
 *
 * Passes the chain straight through without touching the request or response body, so a
 * release build pays one virtual call per request and captures nothing.
 */
class TraceInterceptor(
    @Suppress("UNUSED_PARAMETER") collector: TraceCollector = TraceCollector(),
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = chain.proceed(chain.request())
}

/**
 * No-op mirror of `:alohomora`'s `TraceCollector`, present only so the [TraceInterceptor]
 * constructor signature matches across the two artifacts.
 */
class TraceCollector(
    @Suppress("UNUSED_PARAMETER") showNotification: Boolean = true,
) {
    fun hasNotificationPermission(): Boolean = false
}
