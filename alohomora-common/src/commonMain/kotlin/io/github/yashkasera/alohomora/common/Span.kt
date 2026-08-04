package io.github.yashkasera.alohomora.common

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * One completed span, from whatever tracer the host app already runs.
 *
 * Alohomora depends on no tracing SDK — the app hands spans over through `Alohomora.recordSpan`,
 * so this shape has to serve OpenTelemetry, Sentry, Datadog and hand-written instrumentation
 * alike. That is why [kind] and [statusCode] are `String` rather than enums: an unrecognised
 * value from a vendor or a newer SDK round-trips instead of failing to decode.
 *
 * The id fields follow W3C Trace Context, which every one of those tracers already speaks.
 */
@Entity(
    indices = [
        // traceId: every read groups by it. spanId: unique so a tracer that exports the same span
        // twice (a retried flush, a duplicated adapter registration) cannot draw two waterfall bars
        // for one operation.
        Index("traceId"),
        Index(value = ["spanId"], unique = true),
    ],
)
@Serializable
data class Span(
    /**
     * Surrogate rowid, deliberately not [spanId]-as-primary-key.
     *
     * `DevToolsStreamAdapter` keys on `Long?` and drops anything not greater than the last key it
     * saw, so the key has to be monotonic. Nothing else here is: a parent span ends *after* its
     * children, a later-started sibling can end before an earlier one, and two spans can share an
     * end timestamp — an end-ordered key both scrambles the sequence and silently drops spans.
     * Insertion order is the only monotonic thing available. Same reasoning as `ErrorDao`.
     */
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** 32 hex chars. Lowercased on write: grouping is string equality, so an uppercase id would render as its own trace. */
    val traceId: String,
    /** 16 hex chars. Lowercased on write, for the same reason as [traceId]. */
    val spanId: String,
    /**
     * Null for a root.
     *
     * OpenTelemetry reports an absent parent as 16 zeros rather than null, so this is normalised
     * through [normalizeSpanId] on write. Skip that and every span looks like a child of a span
     * that does not exist, which makes the whole trace one flat list of orphans.
     */
    val parentSpanId: String? = null,
    val name: String,
    /** The tracer's own vocabulary: an OTel `SpanKind` name, Sentry's `op`, anything. Stored verbatim. */
    val kind: String = KIND_INTERNAL,
    /**
     * Epoch **nanoseconds**, not milliseconds — the one place in this project that diverges, and
     * the unit is in the name for that reason.
     *
     * Milliseconds would be wrong twice over. A sub-millisecond span is a zero-width waterfall bar,
     * and worse, ordering *within* a millisecond collapses: five sequential 200 µs calls render as
     * five simultaneous ones. That is a wrong picture, not a coarse one, and most spans an
     * instrumented mobile app emits are sub-millisecond.
     *
     * Producers disagree on units, so the conversion belongs at the adapter: OTel emits nanos
     * directly, Sentry emits fractional seconds as a `Double`. Use [startEpochMillis] to cross
     * into the millisecond world the rest of this module uses.
     */
    val startEpochNanos: Long,
    /** Epoch nanoseconds; see [startEpochNanos]. May precede it under clock skew — see [Span.hasSkew]. */
    val endEpochNanos: Long,
    /** `UNSET` / `OK` / `ERROR`, or a vendor status passed through unrecognised. */
    val statusCode: String = STATUS_UNSET,
    val statusDescription: String? = null,
    /**
     * Reuses `PropertiesConverter`, exactly like `Event.properties`, so there is no second JSON
     * converter to keep in step. Values are truncated on write — see
     * [SPAN_ATTRIBUTE_VALUE_MAX_CHARS].
     */
    val attributes: JsonElement? = null,
    /**
     * Stored as one JSON array column rather than a child table.
     *
     * `PropertiesConverter` and `HeadersConverter` already establish that structured child data
     * lives in a JSON string column here. The decisive reason is the protocol: spans stream one
     * per frame with no per-span follow-up request, so a child table would need a join in every
     * DAO query to serve a read nobody makes independently — nothing ever asks for events without
     * their span.
     *
     * Empty for tracers with no per-span event concept, which includes Sentry.
     */
    val events: List<SpanEvent> = emptyList(),
    /** Instrumentation scope / library name, when the tracer reports one. */
    val scopeName: String? = null,
    /**
     * Set once the user opens the span's trace, so the list can dim it. Mirrors `TrafficEntry.isViewed`.
     *
     * A per-span column flipped per-trace: viewing is a trace-level act, so opening a trace marks
     * every span in it, and a trace counts as viewed only when all of its spans are.
     */
    val isViewed: Boolean = false,
) {
    companion object {
        const val KIND_INTERNAL: String = "INTERNAL"
        const val STATUS_UNSET: String = "UNSET"
        const val STATUS_ERROR: String = "ERROR"

        /** Hex length of a W3C Trace Context trace id. */
        const val TRACE_ID_HEX_LENGTH: Int = 32

        /** Hex length of a W3C Trace Context span id. */
        const val SPAN_ID_HEX_LENGTH: Int = 16
    }
}

/**
 * A timestamped marker inside a span — a retry, a cache miss, a lock acquisition.
 *
 * Public API: callers pass these to `Alohomora.recordSpan`. Precedent for a re-exported
 * `alohomora-common` type in the public surface is `ReplayRequest` and `CustomScreenPlugin`.
 *
 * Attributes are `Map<String, String>?` rather than `JsonElement` because this crosses the public
 * API, where a Swift caller cannot construct a `JsonElement`. The span's own attributes are
 * `JsonElement` because they only ever cross the wire.
 */
@Serializable
data class SpanEvent(
    val name: String,
    /** Epoch nanoseconds, matching [Span.startEpochNanos]. */
    val epochNanos: Long,
    val attributes: Map<String, String>? = null,
)

/** Mirrors `PropertiesConverter`; see [Span.events] for why events are a column and not a table. */
class SpanEventsConverter {
    @TypeConverter
    fun convertTo(data: List<SpanEvent>): String = Json.encodeToString(data)

    @TypeConverter
    fun convertFrom(string: String): List<SpanEvent> = Json.decodeFromString(string)
}

/** Negative under clock skew or an unset end timestamp; callers render those as instantaneous. */
fun Span.durationNanos(): Long = endEpochNanos - startEpochNanos

/**
 * True when the reported end precedes the start.
 *
 * Surfaced rather than corrected: a debugging tool that quietly fixes up impossible timestamps
 * hides the bug someone opened it to find.
 */
fun Span.hasSkew(): Boolean = endEpochNanos < startEpochNanos

fun Span.isError(): Boolean = statusCode == Span.STATUS_ERROR

/**
 * The single conversion point into the millisecond world every other timestamp in this project
 * uses, so nothing divides by 1e6 by hand at a call site.
 */
fun Span.startEpochMillis(): Long = startEpochNanos / NANOS_PER_MILLI

// Public rather than internal: `internal` is module-scoped, and :alohomora needs these to convert a
// tracer's timestamps at the recording boundary. Named constants because a bare 1_000_000_000 in
// call-site arithmetic is exactly how a unit bug gets written.
const val NANOS_PER_MILLI: Long = 1_000_000L
const val NANOS_PER_SECOND: Long = 1_000_000_000L
