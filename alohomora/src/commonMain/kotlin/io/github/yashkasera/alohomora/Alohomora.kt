package io.github.yashkasera.alohomora

import co.touchlab.kermit.Logger
import io.github.yashkasera.alohomora.Alohomora.initLock
import io.github.yashkasera.alohomora.Alohomora.isReplaySupported
import io.github.yashkasera.alohomora.common.Event
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.data.model.AlohomoraConfig
import io.github.yashkasera.alohomora.devtools.DevToolsDatabaseOverrides
import io.github.yashkasera.alohomora.devtools.DevToolsDefaults
import io.github.yashkasera.alohomora.devtools.DevToolsRuntime
import io.github.yashkasera.alohomora.di.initKoin
import io.github.yashkasera.alohomora.domain.repository.EventsRepository
import io.github.yashkasera.alohomora.domain.repository.TrafficRepository
import io.github.yashkasera.alohomora.plugin.CustomScreenPlugin
import io.github.yashkasera.alohomora.plugin.PluginRegistry
import io.github.yashkasera.alohomora.replay.TrafficReplayHandler
import io.github.yashkasera.alohomora.replay.TrafficReplayRegistry
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
     */
    fun init() {
        try {
            AlohomoraInternal.init(config = null)
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
    internal fun initInternal(config: AlohomoraConfig? = null, appDeclaration: KoinAppDeclaration = {}) {
        initLock.withLock {
            if (koinApplication != null) return
            this.config = config
            koinApplication = initKoin(appDeclaration)
        }
    }

    // ============================================================================
    // Logging and Event Tracking
    // ============================================================================


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

    // ============================================================================
    // DevTools TCP Server
    // ============================================================================

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

    // ============================================================================
    // App Database Overrides
    // ============================================================================

    fun registerAppDatabase(name: String, path: String? = null) {
        DevToolsDatabaseOverrides.include(name, path)
    }

    fun excludeAppDatabase(name: String) {
        DevToolsDatabaseOverrides.exclude(name)
    }

    fun clearAppDatabaseOverrides() {
        DevToolsDatabaseOverrides.clear()
    }

    // ============================================================================
    // Trace Replay
    // ============================================================================

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

    // ============================================================================
    // Plugin System - Custom Screens
    // ============================================================================

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
