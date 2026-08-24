package io.github.yashkasera.alohomora

import io.github.yashkasera.alohomora.Alohomora.isReplaySupported
import io.github.yashkasera.alohomora.common.ActionParameter
import io.github.yashkasera.alohomora.common.FeatureFlag
import io.github.yashkasera.alohomora.common.Span
import io.github.yashkasera.alohomora.common.SpanEvent
import io.github.yashkasera.alohomora.devtools.DevToolsActionHandler
import io.github.yashkasera.alohomora.devtools.DevToolsDefaults
import io.github.yashkasera.alohomora.plugin.CustomScreenPlugin
import io.github.yashkasera.alohomora.replay.TrafficReplayHandler

/**
 * Public entry point for the Alohomora debugging library.
 *
 * Every method delegates to [AlohomoraImpl], which is `internal` — consumers depend on
 * this object's API, not on the implementation behind it.
 */
@Suppress("unused")
object Alohomora {

    /**
     * Initialize the library.
     *
     * **On Android this is not needed** — `AlohomoraInitializer` (AndroidX Startup) runs it
     * automatically at process start. Call this only on platforms without an auto-initializer,
     * such as iOS.
     */
    fun init() = AlohomoraImpl.init()

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
    ) = AlohomoraImpl.recordTraffic(
        id, status, url, message, method, scheme, host, path, query,
        requestBody, responseBody, time, duration, requestHeaders,
        requestContentType, responseContentType, responseHeaders,
        requestSize, responseSize, mockedBy,
    )

    fun recordEvent(name: String, properties: Map<String, String>? = null) =
        AlohomoraImpl.recordEvent(name, properties)

    /**
     * Records a caught, non-fatal [throwable] in the Errors console.
     *
     * Uncaught exceptions are captured automatically — see `installCrashHandler`. Use this for
     * ones you handled but still want to see.
     */
    fun recordError(throwable: Throwable, place: String? = null) =
        AlohomoraImpl.recordError(throwable, place)

    /**
     * Records an error from values you already have, for callers with no Kotlin [Throwable].
     *
     * This exists for Swift. A Swift `Error` or `NSError` is not a `KotlinThrowable`, so the
     * overload above is unreachable from iOS host code.
     */
    fun recordError(reason: String, stackTrace: String? = null, place: String? = null) =
        AlohomoraImpl.recordError(reason, stackTrace, place)

    /**
     * Records a completed span so its trace shows up in the Traces console.
     *
     * **Timestamps are epoch nanoseconds.** Pass the ids your tracer produced — sharing a
     * [traceId] stitches spans into one trace. Both ids are lowercased on write, and an all-zero
     * [parentSpanId] is treated as absent.
     */
    fun recordSpan(
        traceId: String,
        spanId: String,
        name: String,
        startEpochNanos: Long,
        endEpochNanos: Long,
        parentSpanId: String? = null,
        kind: String = Span.KIND_INTERNAL,
        statusCode: String = Span.STATUS_UNSET,
        statusDescription: String? = null,
        attributes: Map<String, String>? = null,
        events: List<SpanEvent> = emptyList(),
        scopeName: String? = null,
    ) = AlohomoraImpl.recordSpan(
        traceId, spanId, name, startEpochNanos, endEpochNanos,
        parentSpanId, kind, statusCode, statusDescription, attributes, events, scopeName,
    )

    /**
     * Records a standalone span with generated ids, for timing one block with no surrounding trace.
     *
     * Renders as a single-span trace. Use the full overload whenever the work *is* part of a trace.
     */
    fun recordSpan(name: String, durationNanos: Long, attributes: Map<String, String>? = null) =
        AlohomoraImpl.recordSpan(name, durationNanos, attributes)

    fun setShakeToOpenEnabled(enabled: Boolean) = AlohomoraImpl.setShakeToOpenEnabled(enabled)

    fun startDevToolsServer(port: Int = DevToolsDefaults.DEFAULT_PORT): Boolean =
        AlohomoraImpl.startDevToolsServer(port)

    fun stopDevToolsServer() = AlohomoraImpl.stopDevToolsServer()

    fun registerAppDatabase(name: String, path: String? = null) =
        AlohomoraImpl.registerAppDatabase(name, path)

    fun excludeAppDatabase(name: String) = AlohomoraImpl.excludeAppDatabase(name)
    fun clearAppDatabaseOverrides() = AlohomoraImpl.clearAppDatabaseOverrides()

    fun registerSharedPreferences(name: String, reader: () -> Map<String, Any?>) =
        AlohomoraImpl.registerSharedPreferences(name, reader)

    fun unregisterSharedPreferences(name: String) = AlohomoraImpl.unregisterSharedPreferences(name)
    fun clearSharedPreferencesOverrides() = AlohomoraImpl.clearSharedPreferencesOverrides()

    /**
     * Registers the client Alohomora should use to re-send a captured request.
     *
     * Until a handler is registered, [isReplaySupported] is false and both consoles hide
     * the replay action.
     */
    fun registerReplayHandler(handler: TrafficReplayHandler) =
        AlohomoraImpl.registerReplayHandler(handler)

    fun clearReplayHandler() = AlohomoraImpl.clearReplayHandler()

    val isReplaySupported: Boolean get() = AlohomoraImpl.isReplaySupported

    /**
     * Specifies which HTTP header names should have their values replaced with `[REDACTED]`
     * at capture time. Matching is case-insensitive.
     */
    fun redactHeaders(vararg headerNames: String) = AlohomoraImpl.redactHeaders(*headerNames)

    fun clearRedactedHeaders() = AlohomoraImpl.clearRedactedHeaders()

    fun recordFeatureFlag(
        key: String,
        value: String,
        source: String? = null,
        type: String? = null,
        metadata: Map<String, String>? = null,
    ) = AlohomoraImpl.recordFeatureFlag(key, value, source, type, metadata)

    fun setFeatureFlags(flags: List<FeatureFlag>, source: String? = null) =
        AlohomoraImpl.setFeatureFlags(flags, source)

    fun clearFeatureFlags() = AlohomoraImpl.clearFeatureFlags()

    fun registerPlugin(plugin: CustomScreenPlugin) = AlohomoraImpl.registerPlugin(plugin)
    fun unregisterPlugin(pluginId: String): Boolean = AlohomoraImpl.unregisterPlugin(pluginId)
    fun getPlugins(): List<CustomScreenPlugin> = AlohomoraImpl.getPlugins()

    fun registerAction(
        id: String,
        label: String,
        description: String? = null,
        parameters: List<ActionParameter> = emptyList(),
        handler: DevToolsActionHandler,
    ) = AlohomoraImpl.registerAction(id, label, description, parameters, handler)

    fun unregisterAction(id: String): Boolean = AlohomoraImpl.unregisterAction(id)
    fun publishPluginData(pluginId: String) = AlohomoraImpl.publishPluginData(pluginId)

    fun setDebugConfig(key: String, value: String) = AlohomoraImpl.setDebugConfig(key, value)
    fun getDebugConfig(key: String): String? = AlohomoraImpl.getDebugConfig(key)
    fun removeDebugConfig(key: String) = AlohomoraImpl.removeDebugConfig(key)
}
