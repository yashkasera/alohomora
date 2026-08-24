package io.github.yashkasera.alohomora

import io.github.yashkasera.alohomora.common.ActionParameter
import io.github.yashkasera.alohomora.common.FeatureFlag
import io.github.yashkasera.alohomora.common.SpanEvent
import io.github.yashkasera.alohomora.devtools.DevToolsActionHandler
import io.github.yashkasera.alohomora.plugin.CustomScreenPlugin
import io.github.yashkasera.alohomora.replay.TrafficReplayHandler

/**
 * No-op mirror of `:alohomora`'s `Alohomora` object.
 * Every member must match exactly; `:consumerParity` enforces this.
 */
@Suppress("unused")
object Alohomora {

    fun init() = Unit

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
    ) = Unit

    fun recordEvent(name: String, properties: Map<String, String>? = null) = Unit

    fun recordError(throwable: Throwable, place: String? = null) = Unit

    fun recordError(reason: String, stackTrace: String? = null, place: String? = null) = Unit

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
    ) = Unit

    fun recordSpan(
        name: String,
        durationNanos: Long,
        attributes: Map<String, String>? = null,
    ) = Unit

    fun setShakeToOpenEnabled(enabled: Boolean) = Unit

    fun startDevToolsServer(port: Int = 53999): Boolean = false

    fun stopDevToolsServer() = Unit

    fun registerAppDatabase(name: String, path: String? = null) = Unit

    fun excludeAppDatabase(name: String) = Unit

    fun clearAppDatabaseOverrides() = Unit

    fun registerSharedPreferences(name: String, reader: () -> Map<String, Any?>) = Unit

    fun unregisterSharedPreferences(name: String) = Unit

    fun clearSharedPreferencesOverrides() = Unit

    fun registerReplayHandler(handler: TrafficReplayHandler) = Unit

    fun clearReplayHandler() = Unit

    val isReplaySupported: Boolean get() = false

    fun redactHeaders(vararg headerNames: String) = Unit

    fun clearRedactedHeaders() = Unit

    fun recordFeatureFlag(
        key: String,
        value: String,
        source: String? = null,
        type: String? = null,
        metadata: Map<String, String>? = null,
    ) = Unit

    fun setFeatureFlags(flags: List<FeatureFlag>, source: String? = null) = Unit

    fun clearFeatureFlags() = Unit

    fun registerPlugin(plugin: CustomScreenPlugin) = Unit

    fun unregisterPlugin(pluginId: String): Boolean = false

    fun getPlugins(): List<CustomScreenPlugin> = emptyList()

    fun registerAction(
        id: String,
        label: String,
        description: String? = null,
        parameters: List<ActionParameter> = emptyList(),
        handler: DevToolsActionHandler,
    ) = Unit

    fun unregisterAction(id: String): Boolean = false

    fun publishPluginData(pluginId: String) = Unit

    fun setDebugConfig(key: String, value: String) = Unit

    fun getDebugConfig(key: String): String? = null

    fun removeDebugConfig(key: String) = Unit
}
