package io.github.yashkasera.alohomora.network

import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.common.TraceEntry
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.statement.bodyAsText
import io.ktor.http.encodedPath
import io.ktor.util.AttributeKey
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private val AlohomoraRequestKey = AttributeKey<TraceEntry>("AlohomoraRequest")

@OptIn(ExperimentalUuidApi::class)
val AlohomoraInspector = createClientPlugin("AlohomoraInspector") {
    onRequest { request, content ->
        val query = request.url.parameters.entries()
            .joinToString("&") { "${it.key}=${it.value.joinToString(",")}" }
            .takeIf { it.isNotEmpty() }

        val entity = TraceEntry(
            id = Uuid.random().toString(),
            url = request.url.buildString(),
            method = request.method.value,
            scheme = request.url.protocol.name,
            host = request.url.host,
            path = request.url.encodedPath,
            query = query,
            requestHeaders = request.headers.entries().associate { it.toPair() },
            time = Clock.System.now().toEpochMilliseconds(),
        )

        entity.request = when (content) {
            is String -> content
            is ByteArray -> content.decodeToString()
            else -> content.toString()
        }

        request.attributes.put(AlohomoraRequestKey, entity)
    }

    onResponse { response ->
        val entity = response.call.request.attributes.getOrNull(AlohomoraRequestKey)
            ?: return@onResponse

        val startTime = entity.time ?: 0L
        val endTime = Clock.System.now().toEpochMilliseconds()

        entity.status = response.status.value
        entity.message = response.status.description
        entity.responseHeaders = response.headers.entries().associate { it.toPair() }
        entity.duration = (endTime - startTime).takeUnless { it <= 0 }
        entity.response = try {
            response.bodyAsText()
        } catch (e: Exception) {
            "<binary or unreadable body: ${e.message}>"
        }

        Alohomora.log(apiRequest = entity)
    }
}
