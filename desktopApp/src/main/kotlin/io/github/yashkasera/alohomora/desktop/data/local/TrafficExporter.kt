package io.github.yashkasera.alohomora.desktop.data.local

import io.github.yashkasera.alohomora.common.TrafficEntry
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

enum class TrafficExportFormat(
    val label: String,
    val extension: String,
    val dialogTitle: String,
) {
    JSON("JSON", ".json", "Export traffic as JSON"),
    HAR("HAR 1.2", ".har", "Export traffic as HAR"),
    CURL("cURL Script", ".sh", "Export traffic as cURL script"),
}

fun List<TrafficEntry>.toExportString(
    format: TrafficExportFormat,
    appVersion: String,
): String = when (format) {
    TrafficExportFormat.JSON -> toJsonExport()
    TrafficExportFormat.HAR -> toHarExport(appVersion)
    TrafficExportFormat.CURL -> toCurlExport()
}

// ── Clean projection ────────────────────────────────────────────────────────────

@Serializable
data class ExportableTrafficEntry(
    val id: String,
    val status: Int? = null,
    val url: String? = null,
    val message: String? = null,
    val method: String? = null,
    val scheme: String? = null,
    val host: String? = null,
    val path: String? = null,
    val query: String? = null,
    val requestBody: String? = null,
    val responseBody: String? = null,
    val time: Long? = null,
    val duration: Long? = null,
    val requestHeaders: Map<String, List<String>>? = null,
    val requestContentType: String? = null,
    val responseContentType: String? = null,
    val responseHeaders: Map<String, List<String>>? = null,
    val requestSize: Long? = null,
    val responseSize: Long? = null,
)

internal fun TrafficEntry.toExportable(): ExportableTrafficEntry = ExportableTrafficEntry(
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

// ── JSON ────────────────────────────────────────────────────────────────────────

@Serializable
data class TrafficExportEnvelope(
    val version: Int = 1,
    val exportedAt: Long,
    val entryCount: Int,
    val entries: List<ExportableTrafficEntry>,
)

private fun List<TrafficEntry>.toJsonExport(): String {
    val envelope = TrafficExportEnvelope(
        exportedAt = System.currentTimeMillis(),
        entryCount = size,
        entries = map(TrafficEntry::toExportable),
    )
    return exportJson.encodeToString(TrafficExportEnvelope.serializer(), envelope)
}

// ── HAR 1.2 ─────────────────────────────────────────────────────────────────────

@Serializable
internal data class HarExportRoot(val log: HarExportLog)

@Serializable
internal data class HarExportLog(
    val version: String = "1.2",
    val creator: HarExportCreator,
    val entries: List<HarExportEntry>,
)

@Serializable
internal data class HarExportCreator(
    val name: String = "Alohomora",
    val version: String,
)

@Serializable
internal data class HarExportEntry(
    val startedDateTime: String,
    val time: Long,
    val request: HarExportRequest,
    val response: HarExportResponse,
    val cache: Map<String, String> = emptyMap(),
    val timings: HarExportTimings,
)

@Serializable
internal data class HarExportRequest(
    val method: String,
    val url: String,
    val httpVersion: String = "HTTP/1.1",
    val headers: List<HarNameValue>,
    val queryString: List<HarNameValue>,
    val headersSize: Long = -1,
    val bodySize: Long,
    val postData: HarExportPostData? = null,
)

@Serializable
internal data class HarExportResponse(
    val status: Int,
    val statusText: String,
    val httpVersion: String = "HTTP/1.1",
    val headers: List<HarNameValue>,
    val content: HarExportContent,
    val headersSize: Long = -1,
    val bodySize: Long,
    val redirectURL: String = "",
)

@Serializable
internal data class HarExportContent(
    val size: Long,
    val mimeType: String,
    val text: String? = null,
)

@Serializable
internal data class HarExportPostData(
    val mimeType: String,
    val text: String,
)

@Serializable
internal data class HarExportTimings(
    val send: Long = 0,
    val wait: Long,
    val receive: Long = 0,
)

@Serializable
internal data class HarNameValue(
    val name: String,
    @SerialName("value") val v: String,
)

private val isoFormatter: DateTimeFormatter =
    DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC)

internal fun TrafficEntry.toHarEntry(): HarExportEntry {
    val startedDateTime = time?.let { isoFormatter.format(Instant.ofEpochMilli(it)) }
        ?: "1970-01-01T00:00:00.000Z"

    return HarExportEntry(
        startedDateTime = startedDateTime,
        time = duration ?: 0,
        request = HarExportRequest(
            method = method?.uppercase() ?: "GET",
            url = url ?: "",
            headers = requestHeaders.toHarHeaders(),
            queryString = parseQueryString(query),
            bodySize = requestSize ?: requestBody?.length?.toLong() ?: 0,
            postData = requestBody?.takeIf { it.isNotBlank() }?.let { body ->
                HarExportPostData(
                    mimeType = requestContentType ?: "application/octet-stream",
                    text = body,
                )
            },
        ),
        response = HarExportResponse(
            status = status ?: 0,
            statusText = statusMessage(),
            headers = responseHeaders.toHarHeaders(),
            content = HarExportContent(
                size = responseSize ?: responseBody?.length?.toLong() ?: 0,
                mimeType = responseContentType ?: "application/octet-stream",
                text = responseBody,
            ),
            bodySize = responseSize ?: responseBody?.length?.toLong() ?: 0,
        ),
        timings = HarExportTimings(wait = duration ?: 0),
    )
}

private fun Map<String, List<String>>?.toHarHeaders(): List<HarNameValue> =
    this?.flatMap { (name, values) -> values.map { HarNameValue(name, it) } } ?: emptyList()

private fun parseQueryString(query: String?): List<HarNameValue> {
    if (query.isNullOrBlank()) return emptyList()
    return query.split('&').mapNotNull { param ->
        val eq = param.indexOf('=')
        if (eq < 0) HarNameValue(param, "")
        else HarNameValue(param.substring(0, eq), param.substring(eq + 1))
    }
}

private fun List<TrafficEntry>.toHarExport(appVersion: String): String {
    val root = HarExportRoot(
        log = HarExportLog(
            creator = HarExportCreator(version = appVersion),
            entries = map { it.toHarEntry() },
        ),
    )
    return exportJson.encodeToString(HarExportRoot.serializer(), root)
}

// ── cURL ────────────────────────────────────────────────────────────────────────

private fun List<TrafficEntry>.toCurlExport(): String = buildString {
    appendLine("#!/usr/bin/env bash")
    appendLine("# Alohomora traffic export")
    appendLine("# Exported at: ${isoFormatter.format(Instant.now())}")
    appendLine("# Entries: $size")
    for (entry in this@toCurlExport) {
        appendLine()
        appendLine(entry.curlCommand())
    }
}
