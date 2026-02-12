package io.github.yashkasera.alohomora.desktop.domain.model

data class ApiLog(
    val id: String,
    val status: Int?,
    val url: String?,
    val message: String?,
    val method: String?,
    val scheme: String?,
    val host: String?,
    val path: String?,
    val query: String?,
    val request: String?,
    val response: String?,
    val time: Long?,
    val duration: Long?,
    val requestHeaders: Map<String, List<String>>?,
    val responseHeaders: Map<String, List<String>>?,
    val curl: String?,
    val size: Long?,
    val isViewed: Boolean,
)
