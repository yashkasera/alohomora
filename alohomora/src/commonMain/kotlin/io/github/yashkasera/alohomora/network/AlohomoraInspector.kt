package io.github.yashkasera.alohomora.network

import io.github.yashkasera.alohomora.data.entity.ApiRequest
import io.github.yashkasera.alohomora.domain.repository.NetworkRepository
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.statement.bodyAsText
import io.ktor.util.AttributeKey
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// Simple Koin helper
private object NetworkInjector : KoinComponent {
    val repository: NetworkRepository by inject()
}

@OptIn(ExperimentalUuidApi::class)
val AlohomoraInspector = createClientPlugin("AlohomoraInspector") {
    val callStartKey = AttributeKey<Long>("AlohomoraCallStart")
    val entity = ApiRequest(id = Uuid.random().toString())
    onRequest { request, content ->
        request.attributes.put(callStartKey, kotlin.time.Clock.System.now().toEpochMilliseconds())
        entity.request = request.body.toString()
    }

    onResponse { response ->
        val startTime = response.call.request.attributes.getOrNull(callStartKey) ?: 0L
        val endTime = kotlin.time.Clock.System.now().toEpochMilliseconds()
        val duration = endTime - startTime

        entity.apply {
            time = startTime
            method = response.call.request.method.value
            url = response.call.request.url.toString()
//            headers = response.call.request.headers.entries().toString()
            status = response.status.value
            path = response.headers.entries().toString()
            this.response = response.bodyAsText()
            this.duration = duration
        }

        // Fire and forget
        GlobalScope.launch {
            try {
                NetworkInjector.repository.addCall(entity)
            } catch (e: Exception) {
                // Ignore failures in logging
            }
        }
    }
}

