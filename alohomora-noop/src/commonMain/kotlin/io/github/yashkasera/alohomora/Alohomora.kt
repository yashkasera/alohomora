package io.github.yashkasera.alohomora

import io.github.yashkasera.alohomora.common.FeatureFlag
import io.github.yashkasera.alohomora.common.SpanEvent
import io.github.yashkasera.alohomora.plugin.CustomScreenPlugin
import io.github.yashkasera.alohomora.replay.TrafficReplayHandler
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

/**
 * No-op implementation of Alohomora for release builds.
 *
 * This version has zero runtime overhead - all methods are empty and will be
 * optimized away by the compiler/ProGuard/R8.
 *
 * Use this in your release builds:
 * ```
 * dependencies {
 *     debugImplementation("io.github.yashkasera:alohomora:1.0.0")
 *     releaseImplementation("io.github.yashkasera:alohomora-noop:1.0.0")
 * }
 * ```
 *
 * **Every member here must mirror `:alohomora`'s `Alohomora` object exactly** — same names,
 * same parameter lists and order, same defaults, same JVM annotations. A consumer compiles
 * one call site against both artifacts, so any divergence is a release-only build failure.
 * `:consumerParity` in the root build enforces this; do not hand-edit one side alone.
 */
object Alohomora {

    fun init() {
        /* no-op */
    }

    // ============================================================================
    // Logging and Event Tracking - No-op
    // ============================================================================

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
    ) {
        /* no-op */
    }

    @Suppress("UNUSED_PARAMETER")
    @JvmStatic
    @JvmOverloads
    fun recordEvent(name: String, properties: Map<String, String>? = null) {
        /* no-op */
    }

    // ============================================================================
    // Error Recording - No-op
    // ============================================================================

    @Suppress("UNUSED_PARAMETER")
    @JvmStatic
    @JvmOverloads
    fun recordError(throwable: Throwable, place: String? = null) {
        /* no-op */
    }

    @Suppress("UNUSED_PARAMETER")
    @JvmStatic
    @JvmOverloads
    fun recordError(reason: String, stackTrace: String? = null, place: String? = null) {
        /* no-op */
    }

    // ============================================================================
    // Trace Recording - No-op
    // ============================================================================

    /**
     * No-op mirror of `:alohomora`'s `recordSpan`.
     *
     * This being a no-op is what lets a host app register its tracer adapter from `src/main` rather
     * than a debug-only source set: the adapter compiles and runs in release, and every span it hands
     * over lands here and is discarded. R8 removes the call and the argument construction with it.
     *
     * The defaults are literals rather than `Span.KIND_INTERNAL` / `Span.STATUS_UNSET`, because
     * `Span` lives in `:alohomora-common`, which this module deliberately does not depend on. They
     * must match the real declaration's constants.
     */
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
    ) {
        /* no-op */
    }

    @Suppress("UNUSED_PARAMETER")
    @JvmStatic
    @JvmOverloads
    fun recordSpan(name: String, durationNanos: Long, attributes: Map<String, String>? = null) {
        /* no-op */
    }

    // ============================================================================
    // DevTools TCP Server - No-op
    // ============================================================================

    @Suppress("UNUSED_PARAMETER")
    fun startDevToolsServer(port: Int = 53999): Boolean {
        return false
    }

    fun stopDevToolsServer() {
        /* no-op */
    }

    // ============================================================================
    // App Database Overrides - No-op
    // ============================================================================

    @Suppress("UNUSED_PARAMETER")
    fun registerAppDatabase(name: String, path: String? = null) {
        /* no-op */
    }

    @Suppress("UNUSED_PARAMETER")
    fun excludeAppDatabase(name: String) {
        /* no-op */
    }

    fun clearAppDatabaseOverrides() {
        /* no-op */
    }

    // ============================================================================
    // Traffic Replay - No-op
    // ============================================================================

    @Suppress("UNUSED_PARAMETER")
    fun registerReplayHandler(handler: TrafficReplayHandler) {
        /* no-op */
    }

    fun clearReplayHandler() {
        /* no-op */
    }

    val isReplaySupported: Boolean get() = false

    // ============================================================================
    // Feature Flags - No-op
    // ============================================================================

    @Suppress("UNUSED_PARAMETER")
    @JvmStatic
    @JvmOverloads
    fun recordFeatureFlag(
        key: String,
        value: String,
        source: String? = null,
        type: String? = null,
        metadata: Map<String, String>? = null,
    ) {
        /* no-op */
    }

    @Suppress("UNUSED_PARAMETER")
    @JvmStatic
    @JvmOverloads
    fun setFeatureFlags(
        flags: List<FeatureFlag>,
        source: String? = null,
    ) {
        /* no-op */
    }

    @JvmStatic
    fun clearFeatureFlags() {
        /* no-op */
    }

    // ============================================================================
    // Plugin System - No-op
    // ============================================================================

    @Suppress("UNUSED_PARAMETER")
    fun registerPlugin(plugin: CustomScreenPlugin) {
        /* no-op */
    }

    @Suppress("UNUSED_PARAMETER")
    fun unregisterPlugin(pluginId: String): Boolean {
        return false
    }

    fun getPlugins(): List<CustomScreenPlugin> {
        return emptyList()
    }
}
