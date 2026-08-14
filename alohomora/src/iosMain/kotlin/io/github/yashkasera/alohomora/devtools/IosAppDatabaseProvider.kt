package io.github.yashkasera.alohomora.devtools

import io.github.yashkasera.alohomora.common.AppDatabaseInfo
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSLibraryDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

/**
 * Discovers SQLite databases inside the iOS app sandbox.
 *
 * Android gets this for free from `Context.databaseList()`; iOS has no such registry, so the
 * sandbox directories an app realistically stores a database in are walked instead. Covers the
 * Documents, Library and Library/Application Support directories, which is where Room,
 * Core Data and hand-rolled SQLite all end up.
 *
 * Applies the same exclusion rules as the Android provider: Alohomora's own database is hidden,
 * and WAL/SHM/journal sidecars are filtered out — listing those made the desktop client default
 * to a non-database file and show zero tables.
 */
@OptIn(ExperimentalForeignApi::class)
internal class IosAppDatabaseProvider : DevToolsAppDatabaseProvider {

    override fun listDatabases(): List<AppDatabaseInfo> {
        val overrides = DevToolsDatabaseOverrides.snapshot()
        val includeNames = overrides.includes.keys
        val excluded = (DEFAULT_EXCLUDES + overrides.excludes) - includeNames

        val discovered = LinkedHashMap<String, AppDatabaseInfo>()
        searchRoots().forEach { root ->
            filesIn(root).forEach { path ->
                val name = path.substringAfterLast('/')
                if (name.isBlank()) return@forEach
                if (!name.looksLikeDatabase()) return@forEach
                if (name in excluded || name.databaseBaseName() in excluded) return@forEach
                discovered.getOrPut(name) { AppDatabaseInfo(name = name, path = path) }
            }
        }

        overrides.includes.forEach { (name, path) ->
            val resolved = path ?: discovered[name]?.path ?: return@forEach
            discovered[name] = AppDatabaseInfo(name = name, path = resolved)
        }

        return discovered.values.toList()
    }

    override fun resolvePath(name: String): String? {
        DevToolsDatabaseOverrides.snapshot().includes[name]?.let { return it }
        // Resolved by re-discovery rather than by string concatenation, so a wire-supplied name
        // can never be turned into an arbitrary filesystem path.
        return listDatabases().firstOrNull { it.name == name }?.path
    }

    private fun searchRoots(): List<String> =
        listOf(NSDocumentDirectory, NSLibraryDirectory, NSApplicationSupportDirectory)
            .flatMap { directory ->
                @Suppress("UNCHECKED_CAST")
                NSSearchPathForDirectoriesInDomains(directory, NSUserDomainMask, true)
                    as? List<String> ?: emptyList()
            }
            .distinct()

    private fun filesIn(root: String): List<String> {
        val manager = NSFileManager.defaultManager

        @Suppress("UNCHECKED_CAST")
        val names =
            manager.contentsOfDirectoryAtPath(root, null) as? List<String> ?: return emptyList()
        return names.map { "$root/$it" }
    }

    private fun String.looksLikeDatabase(): Boolean {
        if (AUXILIARY_SUFFIXES.any { contains(it, ignoreCase = true) }) return false
        return DATABASE_SUFFIXES.any { endsWith(it, ignoreCase = true) }
    }

    private fun String.databaseBaseName(): String {
        var base = this
        var changed = true
        while (changed) {
            changed = false
            for (suffix in AUXILIARY_SUFFIXES) {
                if (base.endsWith(suffix, ignoreCase = true)) {
                    base = base.dropLast(suffix.length)
                    changed = true
                }
            }
        }
        return base
    }

    private companion object {
        val DEFAULT_EXCLUDES = setOf("alohomora.db")
        val DATABASE_SUFFIXES = listOf(".db", ".sqlite", ".sqlite3")
        val AUXILIARY_SUFFIXES = listOf("-wal", "-shm", "-journal", ".corrupt")
    }
}
