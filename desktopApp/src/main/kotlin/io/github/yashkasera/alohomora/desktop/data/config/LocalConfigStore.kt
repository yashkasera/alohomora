package io.github.yashkasera.alohomora.desktop.data.config

import io.github.yashkasera.alohomora.desktop.domain.config.ConfigKind
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Zero-setup personal store: one JSON file per artifact under `~/.alohomora/local/{dir}/{slug}.json`,
 * no git. The same `Json` config `MockSessionStore` uses, so re-saving an unchanged artifact is
 * byte-identical — no store change here, but the git-backed store depends on it, and identical output
 * keeps the two backends interchangeable.
 *
 * Identity is the artifact's id, never the path: [findFileById] scans the small kind dir and matches on
 * the decoded id, so a rename that leaves the filename put still resolves.
 */
class LocalConfigStore(
    private val baseDir: File = defaultBaseDir(),
) {
    private val json = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
        ignoreUnknownKeys = true
    }

    private fun kindDir(kind: ConfigKind<*>) = File(baseDir, kind.dir)

    /** All `.json` files under the kind dir, including one grouping sub-directory level (deeplinks). */
    private fun kindFiles(kind: ConfigKind<*>): List<File> {
        val root = kindDir(kind)
        if (!root.isDirectory) return emptyList()
        return root.walkTopDown()
            .filter { it.isFile && it.extension == "json" }
            .sortedBy { it.path }
            .toList()
    }

    suspend fun <T> list(kind: ConfigKind<T>): List<T> = withContext(Dispatchers.IO) {
        kindFiles(kind).mapNotNull { file ->
            runCatching { json.decodeFromString(kind.serializer, file.readText()) }.getOrNull()
        }
    }

    suspend fun <T> save(kind: ConfigKind<T>, item: T): Unit = withContext(Dispatchers.IO) {
        val group = kind.subDirOf(item)?.let { slug(it) }
        val dir = (if (group != null) File(kindDir(kind), group) else kindDir(kind)).apply { mkdirs() }
        val id = kind.idOf(item)
        // Overwrite the artifact's existing file (preserving its original slug) rather than creating a
        // second file on rename — the id is authority, the filename is cosmetic.
        val target = findFileById(kind, id) ?: uniqueFile(dir, slug(kind.nameOf(item)))
        target.writeText(json.encodeToString(kind.serializer, item))
    }

    suspend fun <T> delete(kind: ConfigKind<T>, id: String): Unit = withContext(Dispatchers.IO) {
        findFileById(kind, id)?.delete()
    }

    /** The artifact file's last-modified time, or 0 if absent — the source of "when" for LOCAL. */
    suspend fun <T> lastModified(kind: ConfigKind<T>, id: String): Long = withContext(Dispatchers.IO) {
        findFileById(kind, id)?.lastModified() ?: 0L
    }

    private fun <T> findFileById(kind: ConfigKind<T>, id: String): File? =
        kindFiles(kind).firstOrNull { file ->
            runCatching { kind.idOf(json.decodeFromString(kind.serializer, file.readText())) }.getOrNull() == id
        }

    /** `stem.json`, then `stem-2.json`, `stem-3.json`… until an unused path. Deterministic. */
    private fun uniqueFile(dir: File, stem: String): File {
        val first = File(dir, "$stem.json")
        if (!first.exists()) return first
        var suffix = 2
        while (true) {
            val candidate = File(dir, "$stem-$suffix.json")
            if (!candidate.exists()) return candidate
            suffix++
        }
    }

    companion object {
        fun defaultBaseDir(): File = File(System.getProperty("user.home"), ".alohomora/local")
    }
}
