package io.github.yashkasera.alohomora

import io.github.yashkasera.alohomora.common.Span
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.data.model.AlohomoraConfig
import org.koin.dsl.KoinAppDeclaration

internal object AlohomoraInternal {
    fun init(config: AlohomoraConfig? = null, appDeclaration: KoinAppDeclaration = {}) {
        Alohomora.initInternal(config = config, appDeclaration = appDeclaration)
    }

    /**
     * Persists a batch of already-built spans in one transaction.
     *
     * For internal capture paths that hold [Span]s directly. Host apps go through
     * `Alohomora.recordSpan`, which normalises ids and truncates attribute values first — this does
     * neither, so a caller here owns both.
     */
    suspend fun persistSpans(spans: List<Span>) {
        Alohomora.persistSpans(spans)
    }

    fun recordTraffic(trace: TrafficEntry) {
        Alohomora.recordTraffic(
            id = trace.id,
            status = trace.status,
            url = trace.url,
            message = trace.message,
            method = trace.method,
            scheme = trace.scheme,
            host = trace.host,
            path = trace.path,
            query = trace.query,
            requestBody = trace.requestBody,
            responseBody = trace.responseBody,
            time = trace.time,
            duration = trace.duration,
            requestHeaders = trace.requestHeaders,
            requestContentType = trace.requestContentType,
            responseContentType = trace.responseContentType,
            responseHeaders = trace.responseHeaders,
            requestSize = trace.requestSize,
            responseSize = trace.responseSize,
        )
    }
}
