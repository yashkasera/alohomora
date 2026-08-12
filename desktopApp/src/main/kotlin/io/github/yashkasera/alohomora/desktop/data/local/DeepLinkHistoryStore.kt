package io.github.yashkasera.alohomora.desktop.data.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class DeepLinkEntry(
    val url: String,
    val timestamp: Long,
    val label: String? = null,
)

class DeepLinkHistoryStore {

    private val json = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
        ignoreUnknownKeys = true
    }

    private val file: File by lazy {
        val home = System.getProperty("user.home")
        File(home, ".alohomora/deeplink-history.json")
    }

    suspend fun load(): List<DeepLinkEntry> = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext emptyList()
        runCatching {
            json.decodeFromString<List<DeepLinkEntry>>(file.readText())
        }.getOrDefault(emptyList())
    }

    suspend fun add(url: String, label: String? = null): Unit = withContext(Dispatchers.IO) {
        val entries = load().toMutableList()
        entries.removeAll { it.url == url }
        entries.add(0, DeepLinkEntry(url, System.currentTimeMillis(), label))
        val trimmed = entries.take(MAX_ENTRIES)
        write(trimmed)
    }

    suspend fun remove(url: String): Unit = withContext(Dispatchers.IO) {
        val entries = load().filter { it.url != url }
        write(entries)
    }

    suspend fun clear(): Unit = withContext(Dispatchers.IO) {
        if (file.exists()) file.delete()
    }

    private fun write(entries: List<DeepLinkEntry>) {
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(entries))
    }

    companion object {
        private const val MAX_ENTRIES = 50
    }
}
