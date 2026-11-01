package io.github.yashkasera.alohomora.traffic

import io.github.yashkasera.alohomora.devtools.MOCK_ID_HEADER
import io.github.yashkasera.alohomora.devtools.NetworkRuleEngine
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * OkHttp interceptor that checks mock rules and short-circuits matching requests.
 *
 * Use this alongside [AlohomoraInspector][io.github.yashkasera.alohomora.network.AlohomoraInspector]
 * when the Ktor client uses the OkHttp engine. The Ktor plugin handles traffic capture and
 * throttling; this interceptor handles mocking at the OkHttp level where response
 * short-circuiting is possible.
 *
 * ```kotlin
 * HttpClient(OkHttp) {
 *     engine { addInterceptor(MockRuleInterceptor()) }
 *     install(AlohomoraInspector)
 * }
 * ```
 */
class MockRuleInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        // A failed lookup falls through to the network rather than failing the app's request.
        val mockRule = runCatching { NetworkRuleEngine.findMatch(request.url.toString(), request.method) }
            .getOrNull()
            ?: return chain.proceed(request)

        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(mockRule.statusCode)
            .message("Mocked by Alohomora")
            .header(MOCK_ID_HEADER, mockRule.id)
            .body(
                mockRule.responseBody.toResponseBody(
                    mockRule.contentType.toMediaTypeOrNull(),
                ),
            )
            .build()
    }
}
