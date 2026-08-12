package io.github.yashkasera.alohomora.desktop.data.local

import io.github.yashkasera.alohomora.common.MockRule
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class HarRoot(val log: HarLog)

@Serializable
private data class HarLog(val entries: List<HarEntry> = emptyList())

@Serializable
private data class HarEntry(
    val request: HarRequest,
    val response: HarResponse,
)

@Serializable
private data class HarRequest(
    val method: String,
    val url: String,
)

@Serializable
private data class HarResponse(
    val status: Int,
    val content: HarContent = HarContent(),
)

@Serializable
private data class HarContent(
    val text: String? = null,
    val mimeType: String? = null,
)

private val harJson = Json { ignoreUnknownKeys = true }

fun importHar(json: String): List<MockRule> {
    val har = harJson.decodeFromString<HarRoot>(json)
    return har.log.entries
        .filter { it.response.status in 200..299 }
        .filter { !it.response.content.text.isNullOrBlank() }
        .mapIndexed { index, entry ->
            val path = runCatching {
                val url = entry.request.url
                val afterScheme = url.substringAfter("://")
                val pathStart = afterScheme.indexOf('/')
                if (pathStart >= 0) afterScheme.substring(pathStart).substringBefore('?')
                else "/"
            }.getOrDefault("/")
            MockRule(
                id = "",
                enabled = true,
                urlPattern = path,
                isRegex = false,
                method = entry.request.method.uppercase(),
                statusCode = entry.response.status,
                responseBody = entry.response.content.text ?: "",
                contentType = entry.response.content.mimeType
                    ?.substringBefore(';')
                    ?.trim()
                    ?: "application/json",
            )
        }
}
