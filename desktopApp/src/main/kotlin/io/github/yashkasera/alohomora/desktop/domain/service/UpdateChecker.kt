package io.github.yashkasera.alohomora.desktop.domain.service

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class UpdateInfo(
    val latestVersion: String,
    val currentVersion: String,
    val htmlUrl: String,
    val releaseNotes: String,
) {
    val isUpdateAvailable: Boolean
        get() = compareVersions(latestVersion, currentVersion) > 0
}

object UpdateChecker {

    private const val RELEASES_URL =
        "https://api.github.com/repos/yashkasera/alohomora/releases/latest"

    private val json = Json { ignoreUnknownKeys = true }

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) { json(json) }
    }

    suspend fun check(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val response = httpClient.get(RELEASES_URL) {
                header("Accept", "application/vnd.github+json")
            }
            if (response.status.value != 200) return@withContext null

            val release = json.decodeFromString<GitHubRelease>(response.bodyAsText())
            val tag = release.tagName.removePrefix("v")
            UpdateInfo(
                latestVersion = tag,
                currentVersion = currentVersion,
                htmlUrl = release.htmlUrl,
                releaseNotes = release.body.orEmpty(),
            )
        } catch (_: Exception) {
            null
        }
    }
}

private fun compareVersions(a: String, b: String): Int {
    val aParts = a.split(".").map { it.toIntOrNull() ?: 0 }
    val bParts = b.split(".").map { it.toIntOrNull() ?: 0 }
    val len = maxOf(aParts.size, bParts.size)
    for (i in 0 until len) {
        val ap = aParts.getOrElse(i) { 0 }
        val bp = bParts.getOrElse(i) { 0 }
        if (ap != bp) return ap.compareTo(bp)
    }
    return 0
}

@Serializable
private data class GitHubRelease(
    @SerialName("tag_name") val tagName: String = "",
    @SerialName("html_url") val htmlUrl: String = "",
    val body: String? = null,
)
