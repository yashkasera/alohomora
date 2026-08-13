package io.github.yashkasera.alohomora

import io.github.yashkasera.alohomora.common.FeatureFlag
import io.github.yashkasera.alohomora.common.SpanEvent
import io.github.yashkasera.alohomora.plugin.CustomScreenPlugin
import io.github.yashkasera.alohomora.replay.TrafficReplayHandler
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

/**
 * No-op mirror of `:alohomora`'s `Alohomora` object.
 * Every member must match exactly; `:consumerParity` enforces this.
 */
object Alohomora {

    fun init() {}

    @Suppress("UNUSED_PARAMETER")
    @JvmStatic
    fun recordTraffic(
        id: String = "",
        status: Int? = null,
        url: String? = null,
        message: String? = null,
        method: String? = null,
        scheme: String? = null,
        host: String? = null,
        path: String? = null,
        query: String? = null,
        requestBody: String? = null,
        responseBody: String? = null,
        time: Long? = null,
        duration: Long? = null,
        requestHeaders: Map<String, List<String>>? = null,
        requestContentType: String? = null,
        responseContentType: String? = null,
        responseHeaders: Map<String, List<String>>? = null,
        requestSize: Long? = null,
        responseSize: Long? = null,
        mockedBy: String? = null,
    ) {}

    @Suppress("UNUSED_PARAMETER")
    @JvmStatic
    @JvmOverloads
    fun recordEvent(name: String, properties: Map<String, String>? = null) {}

    @Suppress("UNUSED_PARAMETER")
    @JvmStatic
    @JvmOverloads
    fun recordError(throwable: Throwable, place: String? = null) {}

    @Suppress("UNUSED_PARAMETER")
    @JvmStatic
    @JvmOverloads
    fun recordError(reason: String, stackTrace: String? = null, place: String? = null) {}

    /** No-op. Defaults are literals to avoid depending on `:alohomora-common`. */
    @Suppress("UNUSED_PARAMETER")
    @JvmStatic
    @JvmOverloads
    fun recordSpan(
        traceId: String,
        spanId: String,
        name: String,
        startEpochNanos: Long,
        endEpochNanos: Long,
        parentSpanId: String? = null,
        kind: String = "INTERNAL",
        statusCode: String = "UNSET",
        statusDescription: String? = null,
        attributes: Map<String, String>? = null,
        events: List<SpanEvent> = emptyList(),
        scopeName: String? = null,
    ) {}

    @Suppress("UNUSED_PARAMETER")
    @JvmStatic
    @JvmOverloads
    fun recordSpan(name: String, durationNanos: Long, attributes: Map<String, String>? = null) {}

    @Suppress("UNUSED_PARAMETER")
    @JvmStatic
    fun setShakeToOpenEnabled(enabled: Boolean) {}

    @Suppress("UNUSED_PARAMETER")
    fun startDevToolsServer(port: Int = 53999): Boolean {
        return false
    }

    fun stopDevToolsServer() {}

    @Suppress("UNUSED_PARAMETER")
    fun registerAppDatabase(name: String, path: String? = null) {}

    @Suppress("UNUSED_PARAMETER")
    fun excludeAppDatabase(name: String) {}

    fun clearAppDatabaseOverrides() {}

    @Suppress("UNUSED_PARAMETER")
    fun registerReplayHandler(handler: TrafficReplayHandler) {}

    fun clearReplayHandler() {}

    val isReplaySupported: Boolean get() = false

    @Suppress("UNUSED_PARAMETER")
    @JvmStatic
    @JvmOverloads
    fun recordFeatureFlag(
        key: String,
        value: String,
        source: String? = null,
        type: String? = null,
        metadata: Map<String, String>? = null,
    ) {}

    @Suppress("UNUSED_PARAMETER")
    @JvmStatic
    @JvmOverloads
    fun setFeatureFlags(
        flags: List<FeatureFlag>,
        source: String? = null,
    ) {}

    @JvmStatic
    fun clearFeatureFlags() {}

    @Suppress("UNUSED_PARAMETER")
    fun registerPlugin(plugin: CustomScreenPlugin) {}

    @Suppress("UNUSED_PARAMETER")
    fun unregisterPlugin(pluginId: String): Boolean {
        return false
    }

    fun getPlugins(): List<CustomScreenPlugin> {
        return emptyList()
    }
}
