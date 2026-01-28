package io.github.yashkasera.alohomora.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import io.ktor.utils.io.charsets.Charsets
import kotlinx.serialization.json.Json

@Entity
data class ApiRequest(
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
    var request: String? = null,
    var response: String? = null,
    var time: Long? = null,
    var duration: Long? = null,
    var requestHeaders: Map<String, List<String>>? = null,
    var responseHeaders: Map<String, List<String>>? = null,
    var curl: String? = null,
    var size: Long? = null,
    var isViewed: Boolean = false,
) {

    val isShareable: Boolean
        get() =
            request != UNABLE_PARSE_MESSAGE

    val isSuccessful: Boolean
        get() = status in 200..299

    val pathWithQuery: String
        get() = "$path${if (query.isNullOrEmpty()) "" else "?$query"}"

    val summary: String
        get() = "$status $method $pathWithQuery"

    val schemeHostPath: String
        get() = url?.split("?")?.get(0) ?: ""

    private fun getCurlCommand(): String {
        var command = buildString {
            append("curl -X $method")
            requestHeaders?.let {
                append(" -H \"$it\"")
            }

        }

        /*StringBuffer("curl -X ${this.method}")

        this.headers.names().forEach { element ->
            command.append(" -H \"$element: ${this.header(element)}\"")
        }

        val body = this.body
        if (body != null) {
            if (body.contentType() != null) {
                command.append(" -H \"Content-Type: ${body.contentType()}\"")
            }
            if (body.contentLength() != (-1).toLong()) {
                command.append(" -H \"Content-Length: ${body.contentLength()}\"")
            }
            command.append(" --data $'${body.toJsonString().replace("\n", "\\n")}'")
        }
        command =
            command.append(" ${URLDecoder.decode(this.url.toString(), Charsets.UTF_8.name())}")
*/
        return command.toString()
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
