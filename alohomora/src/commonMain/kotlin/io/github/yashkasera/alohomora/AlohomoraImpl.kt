package io.github.yashkasera.alohomora

import co.touchlab.kermit.Logger
import io.github.yashkasera.alohomora.AlohomoraImpl.initLock
import io.github.yashkasera.alohomora.AlohomoraImpl.persistError
import io.github.yashkasera.alohomora.cache.SharedPreferencesOverrides
import io.github.yashkasera.alohomora.common.ActionParameter
import io.github.yashkasera.alohomora.common.CRASH_EVENT_NAME
import io.github.yashkasera.alohomora.common.Error
import io.github.yashkasera.alohomora.common.Event
import io.github.yashkasera.alohomora.common.FeatureFlag
import io.github.yashkasera.alohomora.common.HeaderRedaction
import io.github.yashkasera.alohomora.common.NANOS_PER_SECOND
import io.github.yashkasera.alohomora.common.Span
import io.github.yashkasera.alohomora.common.SpanEvent
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.common.normalizeSpanId
import io.github.yashkasera.alohomora.common.spanAttributesToJson
import io.github.yashkasera.alohomora.data.datasource.local.TrafficDao
import io.github.yashkasera.alohomora.data.model.AlohomoraConfig
import io.github.yashkasera.alohomora.data.model.discoverPlatformBuildConfig
import io.github.yashkasera.alohomora.devtools.DebugConfigStore
import io.github.yashkasera.alohomora.devtools.DevToolsActionHandler
import io.github.yashkasera.alohomora.devtools.DevToolsActionRegistry
import io.github.yashkasera.alohomora.devtools.DevToolsDatabaseOverrides
import io.github.yashkasera.alohomora.devtools.DevToolsPluginDataRegistry
import io.github.yashkasera.alohomora.devtools.DevToolsRuntime
import io.github.yashkasera.alohomora.devtools.FeatureFlagStore
import io.github.yashkasera.alohomora.di.initKoin
import io.github.yashkasera.alohomora.domain.repository.ErrorRepository
import io.github.yashkasera.alohomora.domain.repository.EventsRepository
import io.github.yashkasera.alohomora.domain.repository.SpanRepository
import io.github.yashkasera.alohomora.domain.repository.TrafficRepository
import io.github.yashkasera.alohomora.error.ErrorCapture
import io.github.yashkasera.alohomora.error.installCrashHandler
import io.github.yashkasera.alohomora.plugin.CustomScreenPlugin
import io.github.yashkasera.alohomora.plugin.PluginRegistry
import io.github.yashkasera.alohomora.replay.TrafficReplayHandler
import io.github.yashkasera.alohomora.replay.TrafficReplayRegistry
import io.github.yashkasera.alohomora.trace.SpanCaptureRegistry
import io.github.yashkasera.alohomora.traffic.TrafficNotificationCallback
import kotlin.concurrent.Volatile
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.atomicfu.locks.ReentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.koin.core.KoinApplication
import org.koin.dsl.KoinAppDeclaration

internal object AlohomoraImpl {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val initLock = ReentrantLock()

    /**
     * Alohomora's isolated Koin container. Never registered in Koin's `GlobalContext`, so
     * a host app is free to run its own `startKoin`. See [initKoin].
     */
    @Volatile
    internal var koinApplication: KoinApplication? = null
        private set

    private val koin get() = koinApplication?.koin

    internal var config: AlohomoraConfig? = null
        private set

    internal val identifier by lazy {
        config?.let {
            "${it.appName}-${it.variantName}-${it.versionName}-${it.commitSha}"
        }
    }

    fun init() {
        try {
            initInternal(config = discoverPlatformBuildConfig())
        } catch (e: Throwable) {
            println("[Alohomora] init failed: ${e.message}")
        }
    }

    /**
     * Once-only initialization, guarded by [initLock].
     *
     * [config] is assigned only on the initializing call: a later `init()` with a null
     * config must not wipe the build metadata discovered by `AlohomoraInitializer`.
     */
    internal fun initInternal(
        config: AlohomoraConfig? = null,
        appDeclaration: KoinAppDeclaration = {},
    ) {
        initLock.withLock {
            if (koinApplication != null) return
            this.config = config
            koinApplication = initKoin(appDeclaration)
            installCrashHandler()
            installShakeToOpen()
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun recordTraffic(
        id: String,
        status: Int?,
        url: String?,
        message: String?,
        method: String?,
        scheme: String?,
        host: String?,
        path: String?,
        query: String?,
        requestBody: String?,
        responseBody: String?,
        time: Long?,
        duration: Long?,
        requestHeaders: Map<String, List<String>>?,
        requestContentType: String?,
        responseContentType: String?,
        responseHeaders: Map<String, List<String>>?,
        requestSize: Long?,
        responseSize: Long?,
        mockedBy: String?,
    ) {
        // Resolution happens inside the coroutine on purpose: building TrafficRepository
        // transitively opens AlohomoraDb, which runs a synchronous SQLite open plus
        // `PRAGMA quick_check`. Doing that eagerly would charge it to whichever thread
        // recorded the first trace — typically an OkHttp dispatcher thread, or main.
        scope.launch {
            val repo = koin?.get<TrafficRepository>() ?: return@launch
            val trace = TrafficEntry(
                id = id.ifEmpty { Uuid.random().toString() },
                status = status,
                url = url,
                message = message,
                method = method,
                scheme = scheme,
                host = host,
                path = path,
                query = query,
                requestBody = requestBody,
                responseBody = responseBody,
                time = time,
                duration = duration,
                requestHeaders = requestHeaders,
                requestContentType = requestContentType,
                responseContentType = responseContentType,
                responseHeaders = responseHeaders,
                requestSize = requestSize,
                responseSize = responseSize,
                mockedBy = mockedBy,
            )
            try {
                repo.save(trace)
            } catch (e: Exception) {
                Logger.d {
                    "[Alohomora] Failed to log API request: ${e.message}"
                }
            }
        }
    }

    internal fun persistTrafficEntry(entry: TrafficEntry) {
        scope.launch {
            val repo = koin?.get<TrafficRepository>() ?: return@launch
            try {
                repo.save(entry)
                koin?.getOrNull<TrafficNotificationCallback>()?.let { callback ->
                    val dao = koin?.get<TrafficDao>() ?: return@let
                    val latest = dao.getLatest(5)
                    if (latest.isNotEmpty()) callback.onTrafficUpdated(latest)
                }
            } catch (e: Exception) {
                Logger.d { "[Alohomora] Failed to log API request: ${e.message}" }
            }
        }
    }

    fun recordEvent(name: String, properties: Map<String, String>?) {
        scope.launch {
            val repo = koin?.get<EventsRepository>() ?: return@launch
            try {
                repo.save(
                    Event(
                        time = Clock.System.now().toEpochMilliseconds(),
                        name = name,
                        properties = Json.encodeToJsonElement(properties),
                    ),
                )
            } catch (e: Exception) {
                Logger.d { "[Alohomora] Failed to record telemetry: ${e.message}" }
            }
        }
    }

    fun recordError(throwable: Throwable, place: String?) {
        val error = ErrorCapture.toError(throwable, place)
        scope.launch {
            try {
                persistError(error)
            } catch (e: Exception) {
                Logger.d { "[Alohomora] Failed to record error: ${e.message}" }
            }
        }
    }

    fun recordError(reason: String, stackTrace: String?, place: String?) {
        val error = Error(
            place = place,
            reason = reason,
            stackTrace = stackTrace,
            time = Clock.System.now().toEpochMilliseconds(),
        )
        scope.launch {
            try {
                persistError(error)
            } catch (e: Exception) {
                Logger.d { "[Alohomora] Failed to record error: ${e.message}" }
            }
        }
    }

    /**
     * Writes [error] to the Errors table and mirrors it into the Events timeline.
     *
     * `suspend` rather than fire-and-forget because the crash handlers must be able to await it on
     * a thread that is about to be torn down.
     */
    internal suspend fun persistError(error: Error) {
        koin?.get<ErrorRepository>()?.save(error)
        koin?.get<EventsRepository>()?.save(
            Event(
                time = error.time,
                name = CRASH_EVENT_NAME,
                properties = Json.encodeToJsonElement(
                    buildMap {
                        error.reason?.let { put("reason", it) }
                        error.place?.let { put("place", it) }
                    },
                ),
            ),
        )
    }

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
    ) {
        val normalizedTraceId = normalizeSpanId(traceId)
        val normalizedSpanId = normalizeSpanId(spanId)
        if (normalizedTraceId == null || normalizedSpanId == null) {
            // Dropped rather than stored: a span with no usable id cannot be grouped into a trace or
            // parented, so it would sit in the table as a row no console can render.
            Logger.d { "[Alohomora] Ignoring span '$name' with a blank trace or span id" }
            return
        }
        SpanCaptureRegistry.markActive()
        val span = Span(
            traceId = normalizedTraceId,
            spanId = normalizedSpanId,
            parentSpanId = normalizeSpanId(parentSpanId),
            name = name,
            kind = kind,
            startEpochNanos = startEpochNanos,
            endEpochNanos = endEpochNanos,
            statusCode = statusCode,
            statusDescription = statusDescription?.takeIf { it.isNotBlank() },
            attributes = spanAttributesToJson(attributes),
            events = events,
            scopeName = scopeName,
        )
        scope.launch {
            try {
                persistSpan(span)
            } catch (e: Exception) {
                Logger.d { "[Alohomora] Failed to record span: ${e.message}" }
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun recordSpan(name: String, durationNanos: Long, attributes: Map<String, String>?) {
        val now = Clock.System.now()
        val endEpochNanos = now.epochSeconds * NANOS_PER_SECOND + now.nanosecondsOfSecond
        recordSpan(
            traceId = Uuid.random().toHexString(),
            spanId = Uuid.random().toHexString().take(Span.SPAN_ID_HEX_LENGTH),
            name = name,
            startEpochNanos = endEpochNanos - durationNanos,
            endEpochNanos = endEpochNanos,
            attributes = attributes,
        )
    }

    /** Writes [span] to the Span table. Split out to mirror [persistError]'s shape. */
    internal suspend fun persistSpan(span: Span) {
        koin?.get<SpanRepository>()?.save(span)
    }

    /**
     * Writes a whole batch in one transaction, for a tracer that exports in batches.
     */
    internal suspend fun persistSpans(spans: List<Span>) {
        koin?.get<SpanRepository>()?.saveAll(spans)
    }

    fun setShakeToOpenEnabled(enabled: Boolean) {
        ShakeToOpenState.enabled = enabled
    }

    fun startDevToolsServer(port: Int): Boolean {
        return try {
            val runtime = koin?.get<DevToolsRuntime>() ?: return false
            runtime.start(port)
        } catch (e: Throwable) {
            println("[Alohomora] startDevToolsServer failed: ${e.message}")
            false
        }
    }

    fun stopDevToolsServer() {
        val runtime = koin?.get<DevToolsRuntime>() ?: return
        runtime.stop()
    }

    fun registerAppDatabase(name: String, path: String?) {
        DevToolsDatabaseOverrides.include(name, path)
    }

    fun excludeAppDatabase(name: String) {
        DevToolsDatabaseOverrides.exclude(name)
    }

    fun clearAppDatabaseOverrides() {
        DevToolsDatabaseOverrides.clear()
    }

    fun registerSharedPreferences(name: String, reader: () -> Map<String, Any?>) {
        SharedPreferencesOverrides.register(name, reader)
    }

    fun unregisterSharedPreferences(name: String) {
        SharedPreferencesOverrides.unregister(name)
    }

    fun clearSharedPreferencesOverrides() {
        SharedPreferencesOverrides.clear()
    }

    fun registerReplayHandler(handler: TrafficReplayHandler) {
        TrafficReplayRegistry.register(handler)
    }

    fun clearReplayHandler() {
        TrafficReplayRegistry.clear()
    }

    val isReplaySupported: Boolean get() = TrafficReplayRegistry.isSupported

    fun redactHeaders(vararg headerNames: String) {
        HeaderRedaction.setHeaders(headerNames.toSet())
    }

    fun clearRedactedHeaders() {
        HeaderRedaction.clearHeaders()
    }

    fun recordFeatureFlag(
        key: String,
        value: String,
        source: String?,
        type: String?,
        metadata: Map<String, String>?,
    ) {
        scope.launch {
            val store = koin?.get<FeatureFlagStore>() ?: return@launch
            try {
                store.put(FeatureFlag(key, value, source, type, metadata))
            } catch (e: Exception) {
                Logger.d { "[Alohomora] Failed to record feature flag: ${e.message}" }
            }
        }
    }

    fun setFeatureFlags(flags: List<FeatureFlag>, source: String?) {
        scope.launch {
            val store = koin?.get<FeatureFlagStore>() ?: return@launch
            try {
                store.putAll(flags, source)
            } catch (e: Exception) {
                Logger.d { "[Alohomora] Failed to set feature flags: ${e.message}" }
            }
        }
    }

    fun clearFeatureFlags() {
        scope.launch {
            val store = koin?.get<FeatureFlagStore>() ?: return@launch
            store.clear()
        }
    }

    fun registerPlugin(plugin: CustomScreenPlugin) {
        if (PluginRegistry.register(plugin)) {
            plugin.actions.forEach { action ->
                DevToolsActionRegistry.register(
                    action.id, action.label, action.description, action.parameters, action.handler,
                )
            }
            if (plugin.dataFields.isNotEmpty()) {
                DevToolsPluginDataRegistry.register(plugin.id, plugin.dataFields)
            }
        }
    }

    fun unregisterPlugin(pluginId: String): Boolean {
        val plugin = PluginRegistry.getPlugin(pluginId)
        val removed = PluginRegistry.unregister(pluginId)
        if (removed && plugin != null) {
            plugin.actions.forEach { DevToolsActionRegistry.unregister(it.id) }
            DevToolsPluginDataRegistry.unregister(pluginId)
        }
        return removed
    }

    fun getPlugins(): List<CustomScreenPlugin> {
        return PluginRegistry.getAllPlugins()
    }

    fun registerAction(
        id: String,
        label: String,
        description: String?,
        parameters: List<ActionParameter>,
        handler: DevToolsActionHandler,
    ) {
        DevToolsActionRegistry.register(id, label, description, parameters, handler)
    }

    fun unregisterAction(id: String): Boolean {
        return DevToolsActionRegistry.unregister(id)
    }

    fun publishPluginData(pluginId: String) {
        DevToolsPluginDataRegistry.notifyChanged(pluginId)
    }

    fun setDebugConfig(key: String, value: String) {
        koin?.get<DebugConfigStore>()?.set(key, value)
    }

    fun getDebugConfig(key: String): String? {
        return koin?.get<DebugConfigStore>()?.get(key)
    }

    fun removeDebugConfig(key: String) {
        koin?.get<DebugConfigStore>()?.remove(key)
    }
}
