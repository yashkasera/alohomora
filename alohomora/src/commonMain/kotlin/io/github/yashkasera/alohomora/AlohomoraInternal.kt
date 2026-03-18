package io.github.yashkasera.alohomora

import io.github.yashkasera.alohomora.common.TraceEntry
import io.github.yashkasera.alohomora.data.model.AlohomoraConfig
import org.koin.dsl.KoinAppDeclaration

internal object AlohomoraInternal {
    fun init(config: AlohomoraConfig? = null, appDeclaration: KoinAppDeclaration = {}) {
        Alohomora.initInternal(config = config, appDeclaration = appDeclaration)
    }

    fun recordTrace(trace: TraceEntry) {
        Alohomora.recordTrace(
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
