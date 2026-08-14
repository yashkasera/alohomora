package io.github.yashkasera.alohomora.desktop.mcp

import io.github.yashkasera.alohomora.common.AttentionItem
import io.github.yashkasera.alohomora.common.Error
import io.github.yashkasera.alohomora.common.Event
import io.github.yashkasera.alohomora.common.FeatureFlag
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.common.durationNanos
import io.github.yashkasera.alohomora.common.exceptionTypeName
import io.github.yashkasera.alohomora.common.mergeAttentionItems
import io.github.yashkasera.alohomora.common.startEpochMillis
import io.github.yashkasera.alohomora.common.trace.TraceRow
import io.github.yashkasera.alohomora.common.trace.TraceSummary
import io.github.yashkasera.alohomora.common.trace.toTraceRows
import io.github.yashkasera.alohomora.common.trace.toTraceSummaries
import io.github.yashkasera.alohomora.desktop.domain.model.DatabaseSnapshot
import io.github.yashkasera.alohomora.desktop.domain.model.DevToolsConnection
import io.github.yashkasera.alohomora.desktop.domain.repository.DevToolsRepository
import io.github.yashkasera.alohomora.replay.replayBlockedReason
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * The read-only MCP tool surface over one connected device window's captured data.
 *
 * The data projections live in [AlohomoraMcpToolData] as pure functions over a [DevToolsRepository],
 * so they are unit-testable with a fake and there is one code path per tool. [registerAlohomoraTools]
 * is a thin adapter: parse arguments, resolve the device, call the projection, wrap the result.
 *
 * Every tool is a pure in-memory read except `query_database_table` and `get_cache_value`, which
 * re-use the existing device request methods and await the matching `StateFlow` update with a bounded
 * timeout — observational, never mutating device state.
 */

// Shared with AlohomoraMcpWriteTools (same package): the tool-plumbing helpers below are internal.
internal val json = Json {
    encodeDefaults = true
    explicitNulls = false
}

/** Long enough for an adb round trip, short enough that a wedged device fails the tool, not hangs it. */
internal const val ROUND_TRIP_TIMEOUT_MILLIS = 5_000L

/** Caps how many rows a list_* tool returns when the caller gives no limit. */
internal const val DEFAULT_LIST_LIMIT = 100

fun registerAlohomoraTools(
    server: Server,
    registry: DeviceSessionRegistry,
    serverVersion: String,
) {
    server.addTool(
        "list_devices",
        "List every device window currently open in Alohomora, with its connection state and a build " +
            "summary. Use this first when more than one device is connected; other tools take an optional " +
            "deviceId that must match one of these.",
        emptySchema(),
    ) { _ ->
        result(AlohomoraMcpToolData.listDevices(registry.sessions.value))
    }

    server.addTool(
        "get_attention",
        "What is wrong right now: unviewed errors plus failed (non-2xx) traffic, newest first. The best " +
            "entry point when asked why something is broken.",
        deviceOnlySchema(),
    ) { request ->
        withRepo(registry, request) { repo -> result(AlohomoraMcpToolData.attention(repo)) }
    }

    server.addTool(
        "list_traffic",
        "List captured network requests (newest first) as compact summaries. Filters: status (exact code), " +
            "method, host, failedOnly (non-2xx only), limit. Use get_traffic for full bodies and headers.",
        buildSchema {
            put("deviceId", stringProp("Target device; optional when only one is connected."))
            put("status", intProp("Only entries with this exact HTTP status code."))
            put("method", stringProp("Only entries with this HTTP method (case-insensitive)."))
            put(
                "host",
                stringProp("Only entries whose host contains this string (case-insensitive)."),
            )
            put("failedOnly", boolProp("When true, only non-2xx entries."))
            put("limit", intProp("Max entries to return (default $DEFAULT_LIST_LIMIT)."))
        },
    ) { request ->
        withRepo(registry, request) { repo ->
            result(
                AlohomoraMcpToolData.listTraffic(
                    repo = repo,
                    status = request.int("status"),
                    method = request.str("method"),
                    host = request.str("host"),
                    failedOnly = request.bool("failedOnly") ?: false,
                    limit = request.int("limit") ?: DEFAULT_LIST_LIMIT,
                ),
            )
        }
    }

    server.addTool(
        "get_traffic",
        "Full detail for one captured request by id: headers, bodies, timing, a ready-to-run curl command, " +
            "and truncation flags. A truncated body is incomplete — the flags say so.",
        buildSchema {
            put("deviceId", stringProp("Target device; optional when only one is connected."))
            put("id", stringProp("The traffic entry id, from list_traffic."))
        },
    ) { request ->
        withRepo(registry, request) { repo ->
            val id =
                request.str("id") ?: return@withRepo errorResult("Missing required argument: id")
            AlohomoraMcpToolData.getTraffic(repo, id)?.let { result(it) }
                ?: errorResult("No traffic entry with id $id")
        }
    }

    server.addTool(
        "list_errors",
        "List captured errors and crashes (newest first) as titles. Use get_error for the full stack trace.",
        buildSchema {
            put("deviceId", stringProp("Target device; optional when only one is connected."))
            put("limit", intProp("Max entries to return (default $DEFAULT_LIST_LIMIT)."))
        },
    ) { request ->
        withRepo(registry, request) { repo ->
            result(
                AlohomoraMcpToolData.listErrors(
                    repo,
                    request.int("limit") ?: DEFAULT_LIST_LIMIT,
                ),
            )
        }
    }

    server.addTool(
        "get_error",
        "Full detail for one captured error by id, including the complete stack trace.",
        buildSchema {
            put("deviceId", stringProp("Target device; optional when only one is connected."))
            put("id", intProp("The error id, from list_errors."))
        },
    ) { request ->
        withRepo(registry, request) { repo ->
            val id =
                request.long("id") ?: return@withRepo errorResult("Missing required argument: id")
            AlohomoraMcpToolData.getError(repo, id)?.let { result(it) }
                ?: errorResult("No error with id $id")
        }
    }

    server.addTool(
        "list_traces",
        "List distributed traces (groups of spans sharing a traceId), newest first. Filters: errorsOnly, " +
            "limit. Covers the latest spans the console holds. Use get_trace for the span tree.",
        buildSchema {
            put("deviceId", stringProp("Target device; optional when only one is connected."))
            put("errorsOnly", boolProp("When true, only traces containing an error span."))
            put("limit", intProp("Max traces to return (default $DEFAULT_LIST_LIMIT)."))
        },
    ) { request ->
        withRepo(registry, request) { repo ->
            result(
                AlohomoraMcpToolData.listTraces(
                    repo = repo,
                    errorsOnly = request.bool("errorsOnly") ?: false,
                    limit = request.int("limit") ?: DEFAULT_LIST_LIMIT,
                ),
            )
        }
    }

    server.addTool(
        "get_trace",
        "The span tree for one traceId, flattened in display order with depth, duration, and the skew/orphan " +
            "flags the waterfall shows. A trace with no root span yet is still in flight (incomplete).",
        buildSchema {
            put("deviceId", stringProp("Target device; optional when only one is connected."))
            put("traceId", stringProp("The traceId, from list_traces."))
        },
    ) { request ->
        withRepo(registry, request) { repo ->
            val traceId = request.str("traceId")
                ?: return@withRepo errorResult("Missing required argument: traceId")
            AlohomoraMcpToolData.getTrace(repo, traceId)?.let { result(it) }
                ?: errorResult("No spans for traceId $traceId")
        }
    }

    server.addTool(
        "list_events",
        "List captured analytics/system events (newest first) with their properties. Filter by name.",
        buildSchema {
            put("deviceId", stringProp("Target device; optional when only one is connected."))
            put(
                "name",
                stringProp("Only events whose name contains this string (case-insensitive)."),
            )
            put("limit", intProp("Max events to return (default $DEFAULT_LIST_LIMIT)."))
        },
    ) { request ->
        withRepo(registry, request) { repo ->
            result(
                AlohomoraMcpToolData.listEvents(
                    repo = repo,
                    name = request.str("name"),
                    limit = request.int("limit") ?: DEFAULT_LIST_LIMIT,
                ),
            )
        }
    }

    server.addTool(
        "get_database_schema",
        "The current database snapshot the console holds: databases, tables, and column schemas. No device " +
            "round trip. Use query_database_table to read rows.",
        deviceOnlySchema(),
    ) { request ->
        withRepo(registry, request) { repo -> result(AlohomoraMcpToolData.databaseSchema(repo)) }
    }

    server.addTool(
        "query_database_table",
        "Read rows from one table. Sends a request to the device and waits for the snapshot to update " +
            "(bounded timeout). Purely observational — it never writes.",
        buildSchema {
            put("deviceId", stringProp("Target device; optional when only one is connected."))
            put("databaseName", stringProp("The database name, from get_database_schema."))
            put("tableName", stringProp("The table name, from get_database_schema."))
            put("limit", intProp("Max rows to request (default 200)."))
        },
    ) { request ->
        withRepo(registry, request) { repo ->
            val db = request.str("databaseName")
                ?: return@withRepo errorResult("Missing required argument: databaseName")
            val tableName = request.str("tableName")
                ?: return@withRepo errorResult("Missing required argument: tableName")
            AlohomoraMcpToolData.queryDatabaseTable(
                repo,
                db,
                tableName,
                request.int("limit") ?: 200,
            )
                ?.let { result(it) }
                ?: errorResult("Timed out waiting for table $tableName from the device")
        }
    }

    server.addTool(
        "list_cache_keys",
        "List the keys held in the app's cache (SharedPreferences / UserDefaults). Use get_cache_value to " +
            "read one.",
        deviceOnlySchema(),
    ) { request ->
        withRepo(registry, request) { repo -> result(AlohomoraMcpToolData.listCacheKeys(repo)) }
    }

    server.addTool(
        "get_cache_value",
        "Read one cache value by key. Sends a request to the device and waits for it (bounded timeout). " +
            "Observational — it never writes.",
        buildSchema {
            put("deviceId", stringProp("Target device; optional when only one is connected."))
            put("key", stringProp("The cache key, from list_cache_keys."))
        },
    ) { request ->
        withRepo(registry, request) { repo ->
            val key =
                request.str("key") ?: return@withRepo errorResult("Missing required argument: key")
            AlohomoraMcpToolData.getCacheValue(repo, key)?.let { result(it) }
                ?: errorResult("Timed out waiting for cache value $key from the device")
        }
    }

    server.addTool(
        "list_feature_flags",
        "List the app's feature flags with their current values and sources.",
        deviceOnlySchema(),
    ) { request ->
        withRepo(registry, request) { repo -> result(AlohomoraMcpToolData.listFeatureFlags(repo)) }
    }

    server.addTool(
        "get_build_metadata",
        "The debug build's metadata: app/package, version, variant, and git branch/commit. Grounding for " +
            "which build the captured data came from.",
        deviceOnlySchema(),
    ) { request ->
        withRepo(registry, request) { repo ->
            AlohomoraMcpToolData.buildMetadata(repo)?.let { result(it) }
                ?: errorResult("No build metadata reported yet")
        }
    }

    server.addTool(
        "get_git_history",
        "Recent git commits embedded in the debug build, newest first.",
        buildSchema {
            put("deviceId", stringProp("Target device; optional when only one is connected."))
            put("limit", intProp("Max commits to return (default $DEFAULT_LIST_LIMIT)."))
        },
    ) { request ->
        withRepo(registry, request) { repo ->
            result(
                AlohomoraMcpToolData.gitHistory(
                    repo,
                    request.int("limit") ?: DEFAULT_LIST_LIMIT,
                ),
            )
        }
    }

    server.addTool(
        "search_traffic",
        "Full-text search across captured traffic: URLs, headers, request and response bodies. " +
            "Returns the same compact summaries as list_traffic. Use this instead of iterating " +
            "list_traffic + get_traffic when looking for a specific payload, error message, or header value.",
        buildSchema {
            put("deviceId", stringProp("Target device; optional when only one is connected."))
            put(
                "query",
                stringProp("The search string (case-insensitive). Matched against URL, host, path, query, method, request/response bodies, and header names and values."),
            )
            put("limit", intProp("Max entries to return (default $DEFAULT_LIST_LIMIT)."))
        },
    ) { request ->
        withRepo(registry, request) { repo ->
            val query = request.str("query")
                ?: return@withRepo errorResult("Missing required argument: query")
            result(
                AlohomoraMcpToolData.searchTraffic(
                    repo = repo,
                    query = query,
                    limit = request.int("limit") ?: DEFAULT_LIST_LIMIT,
                ),
            )
        }
    }

    server.addTool(
        "get_timeline",
        "Interleaved chronological view of traffic, events, errors, and trace spans within an " +
            "optional time window. Each entry is tagged with its kind. The best tool for correlating " +
            "what happened around a specific moment — replaces manual stitching across list_* tools.",
        buildSchema {
            put("deviceId", stringProp("Target device; optional when only one is connected."))
            put("since", longProp("Only entries at or after this epoch-millis timestamp."))
            put("until", longProp("Only entries at or before this epoch-millis timestamp."))
            put(
                "kinds",
                stringProp("Comma-separated subset of: traffic, event, error, span. Default: all four."),
            )
            put("limit", intProp("Max entries to return (default $DEFAULT_LIST_LIMIT)."))
        },
    ) { request ->
        withRepo(registry, request) { repo ->
            result(
                AlohomoraMcpToolData.getTimeline(
                    repo = repo,
                    since = request.long("since"),
                    until = request.long("until"),
                    kinds = request.str("kinds"),
                    limit = request.int("limit") ?: DEFAULT_LIST_LIMIT,
                ),
            )
        }
    }
}

/**
 * The pure data projections behind the tools.
 *
 * Each reads `repo.<flow>.value`, filters/maps, and returns a [JsonElement]. `alohomora-common`
 * models are `@Serializable` and embedded directly; the desktop-side models (build/git/cache/database,
 * none `@Serializable`) are projected field by field — which is also where the one secret,
 * `BuildInfo.slackWebhookUrl`, is dropped. Nullable returns mean "not found"; the tool adapter maps
 * that to a legible error.
 */
internal object AlohomoraMcpToolData {

    fun listDevices(handles: List<DeviceSessionHandle>): JsonElement = buildJsonArray {
        handles.forEach { handle ->
            add(
                buildJsonObject {
                    put("deviceId", handle.deviceId)
                    put("model", handle.model)
                    put("platform", handle.platform)
                    put("connection", handle.devToolsRepository.connectionState.value.label())
                    handle.devToolsRepository.buildInfo.value?.let { build ->
                        put("app", build.projectName)
                        put("versionName", build.versionName)
                        put("branch", build.branch)
                        put("commitSha", build.commitSha)
                    }
                },
            )
        }
    }

    fun attention(repo: DevToolsRepository): JsonElement {
        val items = mergeAttentionItems(repo.errors.value, repo.traffic.value)
        return buildJsonArray {
            items.forEach { item ->
                when (item) {
                    is AttentionItem.UnviewedError -> add(
                        buildJsonObject {
                            put("kind", "error")
                            put("id", item.error.id)
                            put("title", item.error.exceptionTypeName())
                            put("place", item.error.place)
                            put("time", item.error.time)
                        },
                    )

                    is AttentionItem.FailedTraffic -> add(
                        buildJsonObject {
                            put("kind", "traffic")
                            put("id", item.entry.id)
                            put("title", item.entry.summary())
                            put("host", item.entry.host)
                            put("time", item.entry.time)
                        },
                    )
                }
            }
        }
    }

    fun listTraffic(
        repo: DevToolsRepository,
        status: Int?,
        method: String?,
        host: String?,
        failedOnly: Boolean,
        limit: Int,
    ): JsonElement {
        val entries = repo.traffic.value
            .asReversed()
            .filter { status == null || it.status == status }
            .filter { method == null || it.method.equals(method, ignoreCase = true) }
            .filter { host == null || it.host?.contains(host, ignoreCase = true) == true }
            .filter { !failedOnly || !it.isSuccessful() }
            .take(limit.coerceAtLeast(0))
        return buildJsonArray { entries.forEach { add(it.toSummaryJson()) } }
    }

    fun getTraffic(repo: DevToolsRepository, id: String): JsonElement? {
        val entry = repo.traffic.value.firstOrNull { it.id == id } ?: return null
        return buildJsonObject {
            put("entry", json.encodeToJsonElement(TrafficEntry.serializer(), entry))
            put("curl", entry.curlCommand())
            put("requestBodyTruncated", entry.requestBodyTruncated)
            put("responseBodyTruncated", entry.responseBodyTruncated)
            entry.replayBlockedReason()?.let { put("replayBlockedReason", it.name) }
        }
    }

    fun listErrors(repo: DevToolsRepository, limit: Int): JsonElement {
        val errors = repo.errors.value.asReversed().take(limit.coerceAtLeast(0))
        return buildJsonArray {
            errors.forEach { error ->
                add(
                    buildJsonObject {
                        put("id", error.id)
                        put("title", error.exceptionTypeName())
                        put("place", error.place)
                        put("time", error.time)
                        put("viewed", error.isViewed)
                    },
                )
            }
        }
    }

    fun getError(repo: DevToolsRepository, id: Long): JsonElement? {
        val error = repo.errors.value.firstOrNull { it.id == id } ?: return null
        return buildJsonObject {
            put("title", error.exceptionTypeName())
            put("error", json.encodeToJsonElement(Error.serializer(), error))
        }
    }

    fun listTraces(repo: DevToolsRepository, errorsOnly: Boolean, limit: Int): JsonElement {
        val summaries = repo.spans.value.toTraceSummaries()
            .filter { !errorsOnly || it.hasError }
            .take(limit.coerceAtLeast(0))
        return buildJsonArray {
            summaries.forEach { add(json.encodeToJsonElement(TraceSummary.serializer(), it)) }
        }
    }

    fun getTrace(repo: DevToolsRepository, traceId: String): JsonElement? {
        val traceSpans = repo.spans.value.filter { it.traceId == traceId }
        if (traceSpans.isEmpty()) return null
        val rows = traceSpans.toTraceRows()
        return buildJsonObject {
            put("traceId", traceId)
            put("spanCount", traceSpans.size)
            put("isComplete", traceSpans.any { it.parentSpanId == null })
            put("spans", buildJsonArray { rows.forEach { add(it.toJson()) } })
        }
    }

    fun listEvents(repo: DevToolsRepository, name: String?, limit: Int): JsonElement {
        val events = repo.events.value
            .asReversed()
            .filter { name == null || it.name.contains(name, ignoreCase = true) }
            .take(limit.coerceAtLeast(0))
        return buildJsonArray { events.forEach { add(it.toJson()) } }
    }

    fun databaseSchema(repo: DevToolsRepository): JsonElement =
        repo.databaseSnapshot.value.toSchemaJson()

    suspend fun queryDatabaseTable(
        repo: DevToolsRepository,
        databaseName: String,
        tableName: String,
        limit: Int,
    ): JsonElement? {
        val table = withTimeoutOrNull(ROUND_TRIP_TIMEOUT_MILLIS) {
            repo.requestDatabaseTable(databaseName, tableName, limit)
            repo.databaseSnapshot
                .map { it.table }
                .first { it != null && it.name.equals(tableName, ignoreCase = true) }
        } ?: return null
        return buildJsonObject {
            put("databaseName", table.databaseName ?: databaseName)
            put("name", table.name)
            put("columns", buildJsonArray { table.columns.forEach { add(it) } })
            put("rows", buildJsonArray { table.rows.forEach { row -> add(row.toJson()) } })
        }
    }

    fun listCacheKeys(repo: DevToolsRepository): JsonElement =
        buildJsonArray { repo.cacheState.value.keys.forEach { add(it) } }

    suspend fun getCacheValue(repo: DevToolsRepository, key: String): JsonElement? {
        val values = withTimeoutOrNull(ROUND_TRIP_TIMEOUT_MILLIS) {
            repo.requestCacheValue(key)
            repo.cacheState.map { it.values }.first { it.containsKey(key) }
        } ?: return null
        return buildJsonObject {
            put("key", key)
            put("value", values[key])
        }
    }

    fun listFeatureFlags(repo: DevToolsRepository): JsonElement = buildJsonArray {
        repo.featureFlags.value.forEach {
            add(
                json.encodeToJsonElement(
                    FeatureFlag.serializer(),
                    it,
                ),
            )
        }
    }

    fun buildMetadata(repo: DevToolsRepository): JsonElement? {
        val build = repo.buildInfo.value ?: return null
        // slackWebhookUrl is deliberately not serialized — it is the one secret on this model.
        return buildJsonObject {
            put("projectName", build.projectName)
            put("packageName", build.packageName)
            put("versionName", build.versionName)
            put("versionCode", build.versionCode)
            put("variantName", build.variantName)
            put("flavorName", build.flavorName)
            put("buildType", build.buildType)
            put("branch", build.branch)
            put("commitSha", build.commitSha)
            put("isDirty", build.isDirty)
            put("buildTimestampUtc", build.buildTimestampUtc)
        }
    }

    fun gitHistory(repo: DevToolsRepository, limit: Int): JsonElement {
        val commits = repo.gitHistory.value.take(limit.coerceAtLeast(0))
        return buildJsonArray {
            commits.forEach { commit ->
                add(
                    buildJsonObject {
                        put("sha", commit.sha)
                        put("author", commit.author)
                        put("message", commit.message)
                        put("timestamp", commit.timestamp)
                    },
                )
            }
        }
    }

    fun searchTraffic(
        repo: DevToolsRepository,
        query: String,
        limit: Int,
    ): JsonElement {
        val q = query.lowercase()
        val entries = repo.traffic.value
            .asReversed()
            .filter { it.matchesQuery(q) }
            .take(limit.coerceAtLeast(0))
        return buildJsonArray { entries.forEach { add(it.toSummaryJson()) } }
    }

    fun getTimeline(
        repo: DevToolsRepository,
        since: Long?,
        until: Long?,
        kinds: String?,
        limit: Int,
    ): JsonElement {
        val allowed = kinds?.lowercase()?.split(",")?.map { it.trim() }?.toSet()
            ?: setOf("traffic", "event", "error", "span")

        val items = mutableListOf<TimelineItem>()

        if ("traffic" in allowed) {
            repo.traffic.value.mapTo(items) { entry ->
                TimelineItem(
                    kind = "traffic",
                    time = entry.time ?: 0L,
                    json = entry.toSummaryJson(),
                )
            }
        }
        if ("event" in allowed) {
            repo.events.value.mapTo(items) { event ->
                TimelineItem(
                    kind = "event",
                    time = event.time,
                    json = event.toJson(),
                )
            }
        }
        if ("error" in allowed) {
            repo.errors.value.mapTo(items) { error ->
                TimelineItem(
                    kind = "error",
                    time = error.time,
                    json = buildJsonObject {
                        put("id", error.id)
                        put("title", error.exceptionTypeName())
                        put("place", error.place)
                        put("time", error.time)
                    },
                )
            }
        }
        if ("span" in allowed) {
            repo.spans.value.mapTo(items) { span ->
                TimelineItem(
                    kind = "span",
                    time = span.startEpochMillis(),
                    json = buildJsonObject {
                        put("traceId", span.traceId)
                        put("spanId", span.spanId)
                        put("name", span.name)
                        put("kind", span.kind)
                        put("statusCode", span.statusCode)
                        put("startEpochNanos", span.startEpochNanos)
                        put("durationNanos", span.durationNanos())
                    },
                )
            }
        }

        items.sortByDescending { it.time }

        val filtered = items
            .filter { since == null || it.time >= since }
            .filter { until == null || it.time <= until }
            .take(limit.coerceAtLeast(0))

        return buildJsonArray {
            filtered.forEach { item ->
                add(
                    buildJsonObject {
                        put("kind", item.kind)
                        put("time", item.time)
                        put("data", item.json)
                    },
                )
            }
        }
    }
}

private class TimelineItem(val kind: String, val time: Long, val json: JsonObject)

private fun TrafficEntry.matchesQuery(q: String): Boolean {
    if (url?.contains(q, ignoreCase = true) == true) return true
    if (host?.contains(q, ignoreCase = true) == true) return true
    if (path?.contains(q, ignoreCase = true) == true) return true
    if (query?.contains(q, ignoreCase = true) == true) return true
    if (method?.contains(q, ignoreCase = true) == true) return true
    if (message?.contains(q, ignoreCase = true) == true) return true
    if (requestBody?.contains(q, ignoreCase = true) == true) return true
    if (responseBody?.contains(q, ignoreCase = true) == true) return true
    if (headersContain(requestHeaders, q)) return true
    if (headersContain(responseHeaders, q)) return true
    return false
}

private fun headersContain(headers: Map<String, List<String>>?, q: String): Boolean {
    if (headers == null) return false
    return headers.any { (name, values) ->
        name.contains(q, ignoreCase = true) || values.any { it.contains(q, ignoreCase = true) }
    }
}

// --- device resolution -------------------------------------------------------------------------

/**
 * Resolves the request's optional `deviceId` and runs [block] against that window's repository.
 *
 * Returns a legible error result (listing the connected devices) when nothing resolves, so an agent
 * that omitted `deviceId` against several open windows is told what to do rather than handed an empty
 * read.
 */
private inline fun withRepo(
    registry: DeviceSessionRegistry,
    request: CallToolRequest,
    block: (DevToolsRepository) -> CallToolResult,
): CallToolResult {
    val handle = registry.resolve(request.str("deviceId"))
        ?: return errorResult(noDeviceMessage(registry))
    return block(handle.devToolsRepository)
}

internal fun noDeviceMessage(registry: DeviceSessionRegistry): String {
    val ids = registry.sessions.value.map { it.deviceId }
    return when {
        ids.isEmpty() -> "No device is connected. Open a device window in Alohomora first."
        else -> "Multiple devices are connected. Pass deviceId as one of: ${ids.joinToString(", ")}"
    }
}

// --- schema builders ---------------------------------------------------------------------------

internal inline fun buildSchema(properties: JsonObjectBuilder.() -> Unit): ToolSchema =
    ToolSchema(properties = buildJsonObject(properties), required = emptyList())

internal fun emptySchema(): ToolSchema =
    ToolSchema(properties = buildJsonObject { }, required = emptyList())

internal fun deviceOnlySchema(): ToolSchema = buildSchema {
    put("deviceId", stringProp("Target device; optional when only one is connected."))
}

internal fun stringProp(description: String): JsonObject = buildJsonObject {
    put("type", "string")
    put("description", description)
}

internal fun intProp(description: String): JsonObject = buildJsonObject {
    put("type", "integer")
    put("description", description)
}

internal fun longProp(description: String): JsonObject = buildJsonObject {
    put("type", "integer")
    put("description", description)
}

internal fun boolProp(description: String): JsonObject = buildJsonObject {
    put("type", "boolean")
    put("description", description)
}

// --- argument readers --------------------------------------------------------------------------

internal fun CallToolRequest.primitive(key: String): JsonPrimitive? =
    arguments?.get(key) as? JsonPrimitive

internal fun CallToolRequest.str(key: String): String? =
    primitive(key)?.let { if (it.isString) it.content else it.content.takeIf { c -> c != "null" } }

internal fun CallToolRequest.int(key: String): Int? = primitive(key)?.content?.toIntOrNull()
internal fun CallToolRequest.long(key: String): Long? = primitive(key)?.content?.toLongOrNull()
internal fun CallToolRequest.bool(key: String): Boolean? = primitive(key)?.booleanOrNull

// --- result helpers ----------------------------------------------------------------------------

internal fun result(element: JsonElement): CallToolResult =
    CallToolResult(
        content = listOf(
            TextContent(
                json.encodeToString(
                    JsonElement.serializer(),
                    element,
                ),
            ),
        ),
    )

internal fun errorResult(message: String): CallToolResult =
    CallToolResult(content = listOf(TextContent(message)), isError = true)

// --- projections ------------------------------------------------------------------------------

private fun TrafficEntry.toSummaryJson(): JsonObject = buildJsonObject {
    put("id", id)
    put("status", status)
    put("method", method)
    put("host", host)
    put("path", pathWithQuery())
    put("summary", summary())
    put("durationMillis", duration)
    put("mocked", isMocked())
    put("replayOf", replayOf)
    put("viewed", isViewed)
}

private fun Event.toJson(): JsonObject = buildJsonObject {
    put("id", id)
    put("name", name)
    put("time", time)
    put("viewed", isViewed)
    put("properties", properties ?: JsonNull)
}

private fun TraceRow.toJson(): JsonObject = buildJsonObject {
    put("spanId", span.spanId)
    put("parentSpanId", span.parentSpanId)
    put("name", span.name)
    put("kind", span.kind)
    put("statusCode", span.statusCode)
    put("depth", depth)
    put("startEpochNanos", span.startEpochNanos)
    put("durationNanos", span.durationNanos())
    put("hasChildren", hasChildren)
    put("descendantCount", descendantCount)
    put("isOrphan", isOrphan)
    put("hasSkew", hasSkew)
    put("scopeName", span.scopeName)
}

private fun Map<String, String?>.toJson(): JsonObject = buildJsonObject {
    forEach { (key, value) -> put(key, value) }
}

private fun DatabaseSnapshot.toSchemaJson(): JsonObject = buildJsonObject {
    put(
        "databases",
        buildJsonArray {
            databases.forEach { db ->
                add(
                    buildJsonObject {
                        put("name", db.name)
                        put("path", db.path)
                    },
                )
            }
        },
    )
    selectedDatabase?.let { put("selectedDatabase", it.name) }
    schema?.let { schema ->
        put(
            "schema",
            buildJsonObject {
                put("databaseName", schema.databaseName)
                put("tables", buildJsonArray { schema.tables.forEach { add(it) } })
                put(
                    "columns",
                    buildJsonArray {
                        schema.schemas.forEach { table ->
                            add(
                                buildJsonObject {
                                    put("table", table.name)
                                    put("primaryKey", table.primaryKey)
                                    put(
                                        "columns",
                                        buildJsonArray {
                                            table.columns.forEach { column ->
                                                add(
                                                    buildJsonObject {
                                                        put("name", column.name)
                                                        put("type", column.type)
                                                        put("notNull", column.notNull)
                                                        put("primaryKey", column.primaryKey)
                                                    },
                                                )
                                            }
                                        },
                                    )
                                },
                            )
                        }
                    },
                )
            },
        )
    }
}

private fun DevToolsConnection.label(): String = when (this) {
    DevToolsConnection.Disconnected -> "disconnected"
    is DevToolsConnection.Connecting -> "connecting"
    is DevToolsConnection.AwaitingAuth -> "awaiting_auth"
    is DevToolsConnection.Connected -> "connected"
    is DevToolsConnection.Reconnecting -> "reconnecting"
    is DevToolsConnection.Failed -> "failed"
}
