package io.github.yashkasera.alohomora.traffic

import okhttp3.Interceptor
import okhttp3.Response

/**
 * No-op mirror of `:alohomora`'s `MockRuleInterceptor`.
 *
 * Passes the chain straight through. Present so consumers can reference the class
 * unconditionally across debug and release variants.
 */
@Suppress("unused")
class MockRuleInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = chain.proceed(chain.request())
}
