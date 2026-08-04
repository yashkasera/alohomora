package io.github.yashkasera.alohomora.trace

import kotlin.concurrent.Volatile

/**
 * Records whether this app has span capture wired up at all.
 *
 * Alohomora depends on no tracing SDK — the host app bridges its own tracer to
 * `Alohomora.recordSpan` — so there is nothing to inspect to find out whether spans will ever
 * arrive. Without this flag the desktop cannot tell an app with no tracer from one whose first trace
 * has not happened yet, and would offer `REQUEST_TRACE_SPANS` against a device that will never
 * answer. Reported to the desktop as `InitialStatePayload.spanCaptureSupported`.
 *
 * An object rather than a Koin binding for the same reason as `TrafficReplayRegistry`: both consoles
 * read it, neither owns it, and the first `recordSpan` can land before Koin is up.
 *
 * One-way by design. It is never reset, because a tracer that produced a span once will produce more,
 * and a flag that flickered back to false would make the desktop hide a panel that has data in it.
 */
internal object SpanCaptureRegistry {

    @Volatile
    private var active = false

    /** True once anything has recorded a span, so the Traces panel can be offered at all. */
    val isSupported: Boolean get() = active

    fun markActive() {
        active = true
    }
}
