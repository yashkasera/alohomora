package io.github.yashkasera.alohomora.api

import io.github.yashkasera.alohomora.data.entity.ApiRequest
import java.net.URLDecoder
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer

class AlohomoraInterceptor : Interceptor {

    val chuckerCollector by lazy { ChuckerCollector() }

    @OptIn(ExperimentalUuidApi::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        try {
            val requestBody = request.body?.toJsonString()
            val api = ApiRequest(
                id = Uuid.random().toString(),
                url = URLDecoder.decode(request.url.toString(), "UTF-8"),
                method = request.method,
                scheme = request.url.scheme,
                host = request.url.host,
                path = request.url.encodedPath,
                query = request.url.query,
                request = requestBody,
                time = Clock.System.now().toEpochMilliseconds(),
                requestHeaders = request.headers.toMultimap(),
            )
            chuckerCollector.onRequestSent(api)
            val response = chain.proceed(request)
            val responseFormatted = try {
                val responseBody = response.body.string()
                Json.encodeToString(responseBody)
            } catch (e: Exception) {
                response.peekBody(123454).string()
            }
            api.response = responseFormatted
            api.status = response.code
            api.message = response.message
            api.size = response.body.contentLength()
            api.duration = response.receivedResponseAtMillis.minus(response.sentRequestAtMillis)
            api.responseHeaders = response.headers.toMultimap()
            chuckerCollector.onResponseReceived(api)
            return response

            /*CoroutineScope(Dispatchers.IO).launch {
                val response = chain.proceed(request)

                NetworkInjector.dao.update(api)
            }*/

            /* val headers = request.headers.toString()
             CoroutineScope(Dispatchers.IO).launch {
                 val id = NetworkInjector.dao.insert(api)
                 api.id = id
                 if (!api.isSuccessful) {
 //                    ApiRequestNotification.createErrorNotification(api)
                 }
 //                dao.getLatest().let {
 //                    ApiRequestNotification.createNotification(it)
 //                }
             }*/
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    private fun RequestBody.toJsonString(): String {
        val buffer = Buffer()
        this.writeTo(buffer)
        return buffer.readUtf8()
    }
}
