package io.github.yashkasera.alohomora.device

import io.github.yashkasera.alohomora.common.Error
import io.github.yashkasera.alohomora.common.Event
import io.github.yashkasera.alohomora.common.FeatureFlag
import io.github.yashkasera.alohomora.common.Span
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.devtools.FeatureFlagStore
import io.github.yashkasera.alohomora.domain.repository.ErrorRepository
import io.github.yashkasera.alohomora.domain.repository.EventsRepository
import io.github.yashkasera.alohomora.domain.repository.SpanRepository
import io.github.yashkasera.alohomora.domain.repository.TrafficRepository
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Fixture builders that write straight through the internal repositories.
 *
 * Deliberately **not** `Alohomora.recordTraffic` / `recordEvent` / `recordError` / `recordSpan`.
 * Those are fire-and-forget: they launch onto the library's own `Dispatchers.Default` scope, which
 * the Compose test clock knows nothing about, so a `waitForIdle()` right after one returns before
 * the row exists and the assertion races. Writing through the repository inside `runBlocking`
 * means the row is committed before the test touches the UI.
 *
 * The public ingestion path is not left untested — `RecordApiTest` covers it, and polls.
 *
 * Timestamps here are fixed rather than `now()`. Lists are newest-first, so a deterministic
 * ordering is what lets a test assert *which* row is on top.
 */
object Seed {

    /** Arbitrary but fixed: 2026-01-01T00:00:00Z, in milliseconds. */
    const val BASE_MILLIS: Long = 1_767_225_600_000L

    const val BASE_NANOS: Long = BASE_MILLIS * 1_000_000L

    fun ConsoleTestRule.seedTraffic(vararg entries: TrafficEntry) = runBlocking {
        val repo = koin.get<TrafficRepository>()
        entries.forEach { repo.save(it) }
    }

    fun ConsoleTestRule.seedEvents(vararg events: Event) = runBlocking {
        val repo = koin.get<EventsRepository>()
        events.forEach { repo.save(it) }
    }

    fun ConsoleTestRule.seedErrors(vararg errors: Error) = runBlocking {
        val repo = koin.get<ErrorRepository>()
        errors.forEach { repo.save(it) }
    }

    fun ConsoleTestRule.seedSpans(vararg spans: Span) = runBlocking {
        koin.get<SpanRepository>().saveAll(spans.toList())
    }

    fun ConsoleTestRule.seedFeatureFlags(vararg flags: FeatureFlag) {
        val store = koin.get<FeatureFlagStore>()
        flags.forEach { store.put(it) }
    }

    fun traffic(
        id: String,
        method: String = "GET",
        status: Int = 200,
        host: String = "api.example.com",
        path: String = "/v1/posts",
        index: Int = 0,
        requestBody: String? = null,
        responseBody: String? = """{"ok":true}""",
        requestBodyTruncated: Boolean = false,
        replayOf: String? = null,
        mockedBy: String? = null,
    ) = TrafficEntry(
        id = id,
        status = status,
        url = "https://$host$path",
        method = method,
        scheme = "https",
        host = host,
        path = path,
        time = BASE_MILLIS + index * 1_000L,
        duration = 42L,
        requestBody = requestBody,
        responseBody = responseBody,
        requestHeaders = mapOf("Accept" to listOf("application/json")),
        responseHeaders = mapOf("Content-Type" to listOf("application/json")),
        requestContentType = "application/json",
        responseContentType = "application/json",
        requestSize = requestBody?.length?.toLong() ?: 0L,
        responseSize = responseBody?.length?.toLong() ?: 0L,
        requestBodyTruncated = requestBodyTruncated,
        replayOf = replayOf,
        mockedBy = mockedBy,
    )

    fun event(
        name: String,
        index: Int = 0,
        properties: Map<String, String>? = null,
    ) = Event(
        name = name,
        properties = properties?.let { props ->
            buildJsonObject { props.forEach { (k, v) -> put(k, JsonPrimitive(v)) } }
        },
        time = BASE_MILLIS + index * 1_000L,
    )

    fun error(
        reason: String,
        place: String = "com.example.Thing.doWork(Thing.kt:12)",
        index: Int = 0,
        stackTrace: String = "java.lang.IllegalStateException: boom\n\tat com.example.Thing.doWork(Thing.kt:12)",
    ) = Error(
        reason = reason,
        place = place,
        stackTrace = stackTrace,
        time = BASE_MILLIS + index * 1_000L,
    )

    /**
     * One span. Offsets are in **nanoseconds** from [BASE_NANOS] — the only place in the project
     * that is not milliseconds, because a sub-millisecond span would otherwise be a zero-width bar.
     */
    fun span(
        traceId: String,
        spanId: String,
        name: String,
        parentSpanId: String? = null,
        startOffsetNanos: Long = 0L,
        durationNanos: Long = 5_000_000L,
        kind: String = "INTERNAL",
        statusCode: String = "OK",
    ) = Span(
        traceId = traceId,
        spanId = spanId,
        parentSpanId = parentSpanId,
        name = name,
        kind = kind,
        startEpochNanos = BASE_NANOS + startOffsetNanos,
        endEpochNanos = BASE_NANOS + startOffsetNanos + durationNanos,
        statusCode = statusCode,
    )

    fun flag(
        key: String,
        value: String = "true",
        source: String? = "Firebase Remote Config",
        type: String? = "feature_flag",
    ) = FeatureFlag(key = key, value = value, source = source, type = type)
}
