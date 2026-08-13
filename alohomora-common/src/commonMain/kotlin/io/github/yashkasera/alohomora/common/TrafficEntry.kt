package io.github.yashkasera.alohomora.common

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Entity
@Serializable
data class TrafficEntry(
    @PrimaryKey
    val id: String,
    var status: Int? = null,
    var url: String? = null,
    var message: String? = null,
    var method: String? = null,
    var scheme: String? = null,
    var host: String? = null,
    var path: String? = null,
    var query: String? = null,
    var requestBody: String? = null,
    var responseBody: String? = null,
    var time: Long? = null,
    var duration: Long? = null,
    var requestHeaders: Map<String, List<String>>? = null,
    var requestContentType: String? = null,
    var responseContentType: String? = null,
    var responseHeaders: Map<String, List<String>>? = null,
    var requestSize: Long? = null,
    var responseSize: Long? = null,
    var isViewed: Boolean = false,
    /**
     * True when [requestBody] holds only the leading part of a body that exceeded the capture cap.
     *
     * Tracked explicitly rather than sniffed from the `…truncated` suffix the capture appends: a
     * body may legitimately contain that text, and replay has to be able to refuse a partial body
     * with certainty. Sending one would put silently corrupted data on the wire.
     */
    var requestBodyTruncated: Boolean = false,
    /** As [requestBodyTruncated], for the response. Display-only; nothing replays a response. */
    var responseBodyTruncated: Boolean = false,
    /**
     * The id of the traffic entry this one was replayed from, or null for an organically captured request.
     *
     * Lets the console show a replay next to its original instead of as an unexplained duplicate.
     */
    var replayOf: String? = null,
    var mockedBy: String? = null,
) {

    fun isShareable(): Boolean =
        requestBody != UNABLE_PARSE_MESSAGE

    fun isSuccessful(): Boolean = status in 200..299

    fun isMocked(): Boolean = mockedBy != null

    fun pathWithQuery(): String = "$path${if (query.isNullOrEmpty()) "" else "?$query"}"

    fun summary(): String = "$status $method ${pathWithQuery()}"

    fun schemeHostPath(): String = url?.split("?")?.get(0) ?: ""

    fun curlCommand(): String = generateCurlCommand()

    private fun generateCurlCommand(): String {
        return buildString {

            val contentType = requestContentType

            val defaultMethod =
                when {
                    requestBody != null -> "POST"
                    else -> "GET"
                }

            append("curl ${url.toString().shellEscape()}")

            if (method != defaultMethod) {
                append(" \\\n  -X ${method?.shellEscape()}")
            }

            // Iterate the values. Interpolating the List directly emitted
            // `Accept: [application/json]` — Kotlin's List.toString(), brackets included —
            // producing a curl command that does not reproduce the request.
            requestHeaders?.forEach { (name, values) ->
                if (contentType == null || !name.equals("Content-Type", ignoreCase = true)) {
                    values.forEach { value ->
                        append(" \\\n  -H ${"$name: $value".shellEscape()}")
                    }
                }
            }

            if (contentType != null) {
                append(" \\\n  -H ${"Content-Type: $contentType".shellEscape()}")
            }

            // Add request body if present
            requestBody?.takeIf { it.isNotBlank() && it != UNABLE_PARSE_MESSAGE }?.let { body ->
                // Escape single quotes in the body
                append(" --data '${body.shellEscape()}'")
            }

            // Add URL
            url?.let {
                append(" \"$it\"")
            }
        }
    }

    private fun String.shellEscape(): String = "'${replace("'", "'\\''")}'"


    fun statusMessage(): String {
        return when (status) {
            200 -> "OK"
            201 -> "Created"
            204 -> "No Content"
            400 -> "Bad Request"
            401 -> "Unauthorized"
            403 -> "Forbidden"
            404 -> "Not Found"
            500 -> "Internal Server Error"
            502 -> "Bad Gateway"
            503 -> "Service Unavailable"
            else -> ""
        }
    }

    fun generateTransactionText(): String {
        return buildString {
            appendLine("URL: ${url ?: "N/A"}")
            appendLine("Method: ${method ?: "N/A"}")
            appendLine("Protocol: ${scheme ?: "https"}")
            appendLine("Status: ${status ?: "N/A"}")
            appendLine("SSL: Yes")
            appendLine()

            time?.let {
                appendLine("Request time: $it")
            }
            appendLine("Response time: ${time?.plus(duration ?: 0)}")
            appendLine("Duration: ${duration ?: 0} ms")
            appendLine()

            val requestSize = requestSize ?: 0
            val responseSize = responseSize ?: 0
            val totalSize = requestSize + responseSize

            appendLine("Request size: ${formatBytes(requestSize)}")
            appendLine("Response size: ${formatBytes(responseSize)}")
            appendLine("Total size: ${formatBytes(totalSize)}")
            appendLine()

            // Request section
            appendLine("---------- Request ----------")
            appendLine()

            requestHeaders?.forEach { (key, values) ->
                values.forEach { value ->
                    appendLine("$key: $value")
                }
            }
            appendLine()

            requestBody?.takeIf { it.isNotBlank() }?.let {
                appendLine(it)
                appendLine()
            }

            // Response section
            appendLine("---------- Response ----------")
            appendLine()

            responseHeaders?.forEach { (key, values) ->
                values.forEach { value ->
                    appendLine("$key: $value")
                }
            }
            appendLine()

            responseBody?.takeIf { it.isNotBlank() }?.let {
                appendLine(it)
                appendLine()
            }
        }
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }

    companion object {
        const val UNABLE_PARSE_MESSAGE = "Cannot parse body"
    }
}

class HeadersConverter {
    @TypeConverter
    fun convertTo(data: Map<String, List<String>>): String = Json.encodeToString(data)

    @TypeConverter
    fun convertFrom(string: String): Map<String, List<String>> = Json.decodeFromString(string)
}
