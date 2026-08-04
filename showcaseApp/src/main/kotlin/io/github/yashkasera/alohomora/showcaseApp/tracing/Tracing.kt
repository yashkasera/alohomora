package io.github.yashkasera.alohomora.showcaseApp.tracing

import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import java.util.concurrent.TimeUnit

/** Reported to Alohomora as the span's scope name, so the console shows where instrumentation came from. */
const val SHOWCASE_TRACER_NAME: String = "io.github.yashkasera.alohomora.showcaseApp"

fun showcaseTracerProvider(): SdkTracerProvider =
    SdkTracerProvider.builder()
        .addSpanProcessor(
            // Batched rather than SimpleSpanProcessor: export then runs on the processor's worker
            // instead of the thread being traced, which is how a real app is configured, and it is
            // the path AlohomoraSpanExporter's return-success-immediately contract exists for.
            BatchSpanProcessor.builder(AlohomoraSpanExporter())
                // The millis/TimeUnit overload, not setScheduleDelay(Duration): java.time.Duration
                // is API 26 and this app's minSdk is 24. One second because the 5s default is long
                // enough that a refresh looks like it produced no trace at all.
                .setScheduleDelay(1, TimeUnit.SECONDS)
                .build(),
        )
        .build()

/**
 * Runs [block] inside a span and ends it whichever way [block] leaves.
 *
 * The parent is passed explicitly instead of using `Span.makeCurrent()`, and that is not style:
 * OpenTelemetry's implicit current context is a `ThreadLocal`, and a `suspend` function can resume
 * on a different thread than it suspended on. A child created from the implicit context after a
 * suspension point would silently become a root, flattening the trace into unrelated spans.
 *
 * [block] receives the context to parent grandchildren from, so nesting is visible at the call site.
 */
internal inline fun <T> Tracer.traced(
    name: String,
    parent: Context,
    kind: SpanKind = SpanKind.INTERNAL,
    block: (span: Span, context: Context) -> T,
): T {
    val span = spanBuilder(name).setParent(parent).setSpanKind(kind).startSpan()
    return try {
        block(span, parent.with(span))
    } catch (e: Throwable) {
        // Recorded before rethrowing: an unmarked span that simply stops is indistinguishable from
        // one that succeeded, which is the opposite of what the trace is being read for.
        span.setStatus(StatusCode.ERROR, e.message ?: e::class.java.simpleName)
        throw e
    } finally {
        span.end()
    }
}
