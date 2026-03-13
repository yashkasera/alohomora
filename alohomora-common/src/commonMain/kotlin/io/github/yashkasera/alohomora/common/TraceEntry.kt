package io.github.yashkasera.alohomora.common

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Entity
@Serializable
data class TraceEntry(
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
) {

    val isShareable: Boolean
        get() =
            requestBody != UNABLE_PARSE_MESSAGE

    val isSuccessful: Boolean
        get() = status in 200..299

    val pathWithQuery: String
        get() = "$path${if (query.isNullOrEmpty()) "" else "?$query"}"

    val summary: String
        get() = "$status $method $pathWithQuery"

    val schemeHostPath: String
        get() = url?.split("?")?.get(0) ?: ""

    val curlCommand: String
        get() = generateCurlCommand()

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

            requestHeaders?.forEach { (name, value) ->
                if (contentType == null || !name.equals("Content-Type", ignoreCase = true))
                    append(" \\\n  -H ${"$name: $value".shellEscape()}")
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


    val statusMessage: String
        get() {
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

    /*val overview
        get() = buildString {
            bold { append("Method: ") }
            append("${method}\n\n")

            bold { append("URL: ") }
            append("${url}\n\n")

            bold { append("Status: ") }
            append(": ${status}\n\n")

            if (message.isNullOrEmpty().not()) {
                bold { append("Message: ") }
                append("${message}\n\n")
            }

            time?.let {
                bold { append("Time: ") }
                append("${DateUtils().toDate(it, DateUtils.Format._27)}\n\n")
            }

            bold { append("Duration: ") }
            append("${duration}\n\n")

            size?.let {
                bold { append("Size: ") }
                append("${it.toReadableSize}\n\n")
            }

            if (query.isNullOrEmpty().not()) {
                bold {
                    append("Query: \n")
                }
                italic {
                    query?.split("&")?.forEach { query ->
                        query.split("=").let { strings ->
                            append("${strings[0]}: ${strings[1]}\n")
                        }
                    }
                }
                append("\n")
            }

            bold { append("Headers: \n") }
            italic { append("${headers}") }

        }*/

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
