package io.github.yashkasera.alohomora.traffic

import okhttp3.Interceptor
import okhttp3.Response

/**
 * No-op mirror of `:alohomora`'s `TrafficInterceptor`.
 *
 * Passes the chain straight through without touching the request or response body, so a
 * release build pays one virtual call per request and captures nothing.
 */
class TrafficInterceptor(
    @Suppress("UNUSED_PARAMETER") collector: TrafficCollector = TrafficCollector(),
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = chain.proceed(chain.request())
}

/**
 * No-op mirror of `:alohomora`'s `TrafficCollector`, present only so the [TrafficInterceptor]
 * constructor signature matches across the two artifacts.
 */
class TrafficCollector(
    @Suppress("UNUSED_PARAMETER") showNotification: Boolean = true,
) {
    fun hasNotificationPermission(): Boolean = false
}
