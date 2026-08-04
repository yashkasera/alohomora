package io.github.yashkasera.alohomora.showcaseApp.tracing

import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.common.SpanEvent
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SpanExporter

/**
 * Hands OpenTelemetry's completed spans to Alohomora.
 *
 * This class living in the app rather than in the library is the point of the whole design.
 * Alohomora depends on no tracing SDK, so `showcaseApp/build.gradle.kts` is the only file in this
 * repository that declares an OpenTelemetry coordinate — if a tracer type had leaked into
 * `:alohomora`, that would not be possible. Everything crossing the boundary below is a `String`,
 * a `Long` or a `Map`.
 *
 * It is in `src/main`, not a debug-only source set, on purpose: `alohomora-noop`'s `recordSpan` is a
 * no-op, so this adapter compiles and runs in release and every span it forwards is discarded there.
 * A debug-only adapter would mean a second `SdkTracerProvider` wiring for release to fall back to.
 */
class AlohomoraSpanExporter : SpanExporter {

    override fun export(spans: Collection<SpanData>): CompletableResultCode {
        spans.forEach { span ->
            Alohomora.recordSpan(
                traceId = span.traceId,
                spanId = span.spanId,
                name = span.name,
                // No unit conversion: OpenTelemetry reports epoch nanoseconds, which is exactly what
                // recordSpan wants. This is where a Sentry adapter would multiply its fractional
                // seconds by 1e9 — get that wrong for any tracer and every span dates to 1970.
                startEpochNanos = span.startEpochNanos,
                endEpochNanos = span.endEpochNanos,
                // OpenTelemetry reports an absent parent as 16 zeros, never null. Forwarded as-is
                // because recordSpan normalises it; treating it as a real id would make every root
                // look like the child of a span that does not exist.
                parentSpanId = span.parentSpanId,
                // OpenTelemetry's own vocabulary, passed through. recordSpan stores kind and status
                // verbatim rather than mapping them onto an enum, so there is no translation table
                // here to fall behind a newer SDK — and "ERROR" is already the value the consoles
                // style as a failure.
                kind = span.kind.name,
                statusCode = span.status.statusCode.name,
                statusDescription = span.status.description,
                attributes = span.attributes.toStringMap(),
                events = span.events.map { event ->
                    SpanEvent(
                        name = event.name,
                        epochNanos = event.epochNanos,
                        attributes = event.attributes.toStringMap(),
                    )
                },
                scopeName = span.instrumentationScopeInfo.name,
            )
        }
        // Complete and successful immediately, rather than a result finished later, because
        // recordSpan is fire-and-forget — it returns before the write happens, so there is nothing
        // here to await. Returning an *uncompleted* CompletableResultCode is the trap:
        // BatchSpanProcessor's worker waits on whatever export returns, so it would stall for the
        // full 30-second export timeout on every batch and the queue would back up behind it.
        return CompletableResultCode.ofSuccess()
    }

    override fun flush(): CompletableResultCode = CompletableResultCode.ofSuccess()

    override fun shutdown(): CompletableResultCode = CompletableResultCode.ofSuccess()
}

/**
 * OpenTelemetry attribute values are also `Long`, `Double`, `Boolean` and `List`; the Alohomora API
 * takes strings, so the rendering decision is made here rather than by the console.
 */
private fun Attributes.toStringMap(): Map<String, String> =
    asMap().entries.associate { (key, value) -> key.key to value.toString() }
