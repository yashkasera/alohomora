package io.github.yashkasera.alohomora

import co.touchlab.kermit.Logger
import io.github.yashkasera.alohomora.Alohomora.initLock
import io.github.yashkasera.alohomora.Alohomora.isReplaySupported
import io.github.yashkasera.alohomora.Alohomora.persistError
import io.github.yashkasera.alohomora.Alohomora.persistSpan
import io.github.yashkasera.alohomora.common.Error
import io.github.yashkasera.alohomora.common.Event
import io.github.yashkasera.alohomora.common.FeatureFlag
import io.github.yashkasera.alohomora.common.NANOS_PER_SECOND
import io.github.yashkasera.alohomora.common.Span
import io.github.yashkasera.alohomora.common.SpanEvent
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.common.normalizeSpanId
import io.github.yashkasera.alohomora.common.spanAttributesToJson
import io.github.yashkasera.alohomora.data.datasource.local.TrafficDao
import io.github.yashkasera.alohomora.data.model.AlohomoraConfig
import io.github.yashkasera.alohomora.data.model.discoverPlatformBuildConfig
import io.github.yashkasera.alohomora.devtools.DevToolsDatabaseOverrides
import io.github.yashkasera.alohomora.devtools.DevToolsDefaults
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
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
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

/**
 * Main entry point for the Alohomora debugging library.
 *
 * This object provides APIs for:
 * - Initializing the library
 * - Logging and event tracking
 * - Registering custom screens via plugins
 * - Storing and retrieving custom values
 * - Connecting to remote debugging tools
 *
 * Example usage:
 * ```kotlin
 * // Initialize the library
 * Alohomora.init()
 *
 * // Register a custom screen
 * Alohomora.registerPlugin(MyCustomScreen())
 *
 * // Store custom values
 * Alohomora.putValue("api_key", "sk_test_123")
 *
 * // Track events
 * Alohomora.trackEvent("user_login", mapOf("user_id" to "123"))
 * ```
 */
@Suppress("unused")
object Alohomora {
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

    /**
     * Latest commits baked into this build.
     *
     * - Debug / staging (if enabled via plugin): populated
     * - Release / unsupported platforms: empty
     */

    internal var config: AlohomoraConfig? = null
        private set

    internal val json by lazy {
        Json {
            prettyPrint = true
            prettyPrintIndent = "  "
            ignoreUnknownKeys = true
        }
    }

    internal val identifier by lazy {
        config?.let {
            "${it.projectName}-${it.variantName}-${it.versionName}-${it.commitSha}"
        }
    }

    /**
     * Initialize the library.
     *
     * **On Android this is not needed** — `AlohomoraInitializer` (AndroidX Startup) runs it
     * automatically at process start with the application `Context` already wired in. Call
     * this only on platforms without an auto-initializer, such as iOS.
     *
     * Takes no Koin declaration: Alohomora's container is isolated and internal, so there
     * is nothing for a consumer to contribute to it.
     *
     * Build metadata is discovered, not passed in — see [discoverPlatformBuildConfig]. Accepting an
     * `AlohomoraConfig` parameter here would have to be mirrored in `alohomora-noop`, and would put
     * a Kotlin interface in front of Swift callers just to relay values the platform already knows.
     */
    fun init() {
        try {
            AlohomoraInternal.init(config = discoverPlatformBuildConfig())
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
    @JvmStatic
    fun recordTraffic(
        id: String = Uuid.random().toString(),
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
    ) {
        // Resolution happens inside the coroutine on purpose: building TrafficRepository
        // transitively opens AlohomoraDb, which runs a synchronous SQLite open plus
        // `PRAGMA quick_check`. Doing that eagerly would charge it to whichever thread
        // recorded the first trace — typically an OkHttp dispatcher thread, or main.
        scope.launch {
            val repo = koin?.get<TrafficRepository>() ?: return@launch
            val trace = TrafficEntry(
                id = id,
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

    @JvmStatic
    @JvmOverloads
    fun recordEvent(name: String, properties: Map<String, String>? = null) {
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

    /**
     * Records a caught, non-fatal [throwable] in the Errors console.
     *
     * Uncaught exceptions are captured automatically — see `installCrashHandler`. Use this for the
     * ones you handled but still want to see, the `catch` blocks that would otherwise swallow a
     * problem silently.
     *
     * @param place where the failure happened. Defaults to the top stack frame, which is usually
     *   what you want; pass something meaningful ("SyncWorker", "checkout") when the frame is a
     *   generic helper and tells you nothing.
     */
    @JvmStatic
    @JvmOverloads
    fun recordError(throwable: Throwable, place: String? = null) {
        val error = ErrorCapture.toError(throwable, place)
        scope.launch {
            try {
                persistError(error)
            } catch (e: Exception) {
                Logger.d { "[Alohomora] Failed to record error: ${e.message}" }
            }
        }
    }

    /**
     * Records an error from values you already have, for callers with no Kotlin [Throwable].
     *
     * This exists for Swift. A Swift `Error` or `NSError` is not a `KotlinThrowable`, so the
     * overload above is unreachable from iOS host code — without this, an iOS app could not report
     * its own caught failures at all.
     *
     * @param reason formatted `Type: message`. The console splits on the first `:` for the row
     *   title, so `"DecodingError: keyNotFound(\"id\")"` reads correctly and a bare message does not.
     */
    @JvmStatic
    @JvmOverloads
    fun recordError(reason: String, stackTrace: String? = null, place: String? = null) {
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
     * Both, not either: the Errors screen owns the stack trace, while the event is what puts the
     * failure in sequence next to the traffic and events around it — and what carries it to the
     * desktop app, whose protocol has no error message type.
     *
     * `suspend` rather than fire-and-forget because the crash handlers must be able to await it on
     * a thread that is about to be torn down.
     */
    internal suspend fun persistError(error: Error) {
        koin?.get<ErrorRepository>()?.save(error)
        koin?.get<EventsRepository>()?.save(
            Event(
                time = error.time,
                name = ErrorCapture.CRASH_EVENT_NAME,
                properties = Json.encodeToJsonElement(
                    buildMap {
                        error.reason?.let { put("reason", it) }
                        error.place?.let { put("place", it) }
                    },
                ),
            ),
        )
    }

    /**
     * Records a completed span, so its trace shows up in the Traces console.
     *
     * Alohomora depends on **no tracing SDK**. Bridge whatever tracer the app already runs —
     * OpenTelemetry, Sentry, Datadog, hand-rolled timing — with a short adapter in your own code; the
     * README carries ready-made ones. Every tracer's export hook is vendor-specific, so shipping an
     * adapter would serve one vendor while dragging its SDK into both published artifacts, the same
     * reasoning as the deliberately absent `retrofitReplayHandler`.
     *
     * Pass the ids your tracer produced: sharing a [traceId] is what stitches spans into one trace.
     * Both are lowercased on write, and an all-zero [parentSpanId] is treated as absent (how
     * OpenTelemetry reports a root).
     *
     * **Timestamps are epoch nanoseconds, and the adapter converts.** OpenTelemetry emits nanos
     * already; Sentry emits fractional seconds as a `Double`, so multiply by 1e9 once, at the adapter.
     * Get it wrong and every span renders as a 1970 date. See [Span.startEpochNanos] for why this is
     * the one domain that does not use milliseconds.
     *
     * [kind] and [statusCode] take your tracer's own vocabulary; unrecognised values are stored as
     * given rather than rejected. Use `"ERROR"` for [statusCode] to get the failure styling.
     *
     * Fire-and-forget: returns immediately and persists on a background scope, so it is safe to call
     * from a tracer's export thread.
     */
    @JvmStatic
    @JvmOverloads
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

    /**
     * Records a standalone span with generated ids, for timing one block with no surrounding trace.
     *
     * Renders as a single-span trace. Use the full overload whenever the work *is* part of a trace —
     * a span recorded this way cannot be parented, so it will not appear inside the waterfall of the
     * operation that caused it.
     */
    @OptIn(ExperimentalUuidApi::class)
    @JvmStatic
    @JvmOverloads
    fun recordSpan(name: String, durationNanos: Long, attributes: Map<String, String>? = null) {
        val now = Clock.System.now()
        val endEpochNanos = now.epochSeconds * NANOS_PER_SECOND + now.nanosecondsOfSecond
        recordSpan(
            // Hex halves of a random UUID, so the ids are the right shape and width for anything that
            // later correlates them with a real tracer's output.
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
     *
     * Worth having separately from [persistSpan]: a batch of 50 spans through the single-span path is
     * 50 SQLite transactions, on a thread the host's export pipeline is waiting on.
     */
    internal suspend fun persistSpans(spans: List<Span>) {
        koin?.get<SpanRepository>()?.saveAll(spans)
    }

    /**
     * Enables or disables shake-to-open, the gesture that surfaces the console on a physical shake.
     *
     * On by default. Turn it off when it competes with a gesture the host already owns (a dev menu,
     * a custom shake action) or gets in the way of tests that move the device. The accelerometer
     * listener is installed at init regardless; this only gates whether a shake opens the console,
     * so it is safe to call at any time — before or after `init()`.
     */
    @JvmStatic
    fun setShakeToOpenEnabled(enabled: Boolean) {
        ShakeToOpenState.enabled = enabled
    }

    fun startDevToolsServer(port: Int = DevToolsDefaults.DEFAULT_PORT): Boolean {
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

    fun registerAppDatabase(name: String, path: String? = null) {
        DevToolsDatabaseOverrides.include(name, path)
    }

    fun excludeAppDatabase(name: String) {
        DevToolsDatabaseOverrides.exclude(name)
    }

    fun clearAppDatabaseOverrides() {
        DevToolsDatabaseOverrides.clear()
    }

    /**
     * Registers the client Alohomora should use to re-send a captured request.
     *
     * Alohomora never sends replays itself, and this is not an implementation shortcut. Everything
     * a request needs in order to be accepted — a payload signature, a fresh bearer token,
     * certificate pinning — is derived by the host app's own interceptor chain. A request that
     * Alohomora assembled and sent would carry the signature captured from the *original* body,
     * so the moment a payload is edited the server would reject it. Re-entering the app's chain is
     * what makes an edited payload arrive correctly signed.
     *
     * On Android with OkHttp, pass the ready-made handler:
     * ```kotlin
     * Alohomora.registerReplayHandler(okHttpReplayHandler(myOkHttpClient))
     * ```
     *
     * Anywhere else, or when signing happens above the client, supply your own:
     * ```kotlin
     * Alohomora.registerReplayHandler { request ->
     *     val response = myClient.send(request.method, request.url, request.headers, request.body)
     *     ReplayOutcome.Sent()
     * }
     * ```
     *
     * Until a handler is registered, [isReplaySupported] is false and both consoles hide the replay
     * action instead of offering one that cannot work.
     */
    fun registerReplayHandler(handler: TrafficReplayHandler) {
        TrafficReplayRegistry.register(handler)
    }

    /** Removes the registered replay handler, disabling replay in both consoles. */
    fun clearReplayHandler() {
        TrafficReplayRegistry.clear()
    }

    /** True when a replay handler is registered and captured requests can be re-sent. */
    val isReplaySupported: Boolean get() = TrafficReplayRegistry.isSupported

    @JvmStatic
    @JvmOverloads
    fun recordFeatureFlag(
        key: String,
        value: String,
        source: String? = null,
        type: String? = null,
        metadata: Map<String, String>? = null,
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

    @JvmStatic
    @JvmOverloads
    fun setFeatureFlags(
        flags: List<FeatureFlag>,
        source: String? = null,
    ) {
        scope.launch {
            val store = koin?.get<FeatureFlagStore>() ?: return@launch
            try {
                store.putAll(flags, source)
            } catch (e: Exception) {
                Logger.d { "[Alohomora] Failed to set feature flags: ${e.message}" }
            }
        }
    }

    @JvmStatic
    fun clearFeatureFlags() {
        scope.launch {
            val store = koin?.get<FeatureFlagStore>() ?: return@launch
            store.clear()
        }
    }

    /**
     * Register a custom screen plugin.
     *
     * Example:
     * ```kotlin
     * Alohomora.registerPlugin(MyFeatureFlagsScreen())
     * ```
     *
     * @param plugin The custom screen plugin to register
     * @throws IllegalArgumentException if a plugin with the same id is already registered
     */
    fun registerPlugin(plugin: CustomScreenPlugin) {
        PluginRegistry.register(plugin)
    }

    /**
     * Unregister a custom screen plugin by id.
     *
     * @param pluginId The id of the plugin to unregister
     * @return true if the plugin was removed, false if it wasn't found
     */
    fun unregisterPlugin(pluginId: String): Boolean {
        return PluginRegistry.unregister(pluginId)
    }

    /**
     * Get all registered custom screen plugins.
     *
     * @return List of all registered plugins
     */
    fun getPlugins(): List<CustomScreenPlugin> {
        return PluginRegistry.getAllPlugins()
    }
}
