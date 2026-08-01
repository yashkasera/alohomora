package io.github.yashkasera.alohomora.devtools

import android.content.Context
import io.github.yashkasera.alohomora.common.AppDatabaseInfo

internal class AndroidAppDatabaseProvider(
    private val context: Context,
) : DevToolsAppDatabaseProvider {
    override fun listDatabases(): List<AppDatabaseInfo> {
        val overrides = DevToolsDatabaseOverrides.snapshot()
        val includeNames = overrides.includes.keys
        val excluded = (DEFAULT_EXCLUDES + overrides.excludes) - includeNames

        // context.databaseList() returns every file in databases/, not just openable
        // databases: WAL/SHM/journal sidecars, plus `.corrupt` leftovers from past resets.
        // Listing those made the client default to e.g. "alohomora.db-wal" — not a database,
        // so the Vault showed zero tables. Exclusion must also be evaluated against the
        // *base* name, or "alohomora.db-wal" slips past the "alohomora.db" exclude.
        val baseNames = context.databaseList()
            .filter { name -> name.isNotBlank() && !name.isAuxiliaryDatabaseFile() }
            .filter { name -> name !in excluded && name.databaseBaseName() !in excluded }

        val databases = LinkedHashMap<String, AppDatabaseInfo>()
        baseNames.forEach { name ->
            val path = context.getDatabasePath(name).absolutePath
            databases[name] = AppDatabaseInfo(name = name, path = path)
        }

        overrides.includes.forEach { (name, path) ->
            val resolvedPath = path ?: context.getDatabasePath(name).absolutePath
            databases[name] = AppDatabaseInfo(name = name, path = resolvedPath)
        }

        return databases.values.toList()
    }

    override fun resolvePath(name: String): String? {
        val overrides = DevToolsDatabaseOverrides.snapshot()
        val includePath = overrides.includes[name]
        if (includePath != null) return includePath

        val includeNames = overrides.includes.keys
        val excluded = (DEFAULT_EXCLUDES + overrides.excludes) - includeNames
        if (name in excluded) return null
        if (name.isAuxiliaryDatabaseFile() || name.databaseBaseName() in excluded) return null

        val existsInBase = context.databaseList().contains(name)
        if (!existsInBase) return null
        return context.getDatabasePath(name).absolutePath
    }

    companion object {
        private val DEFAULT_EXCLUDES = setOf("alohomora.db")

        /** SQLite sidecars, plus `.corrupt` files left behind by `Context.deleteDatabase`. */
        private val AUXILIARY_SUFFIXES = listOf("-wal", "-shm", "-journal", ".corrupt")

        private fun String.isAuxiliaryDatabaseFile(): Boolean =
            AUXILIARY_SUFFIXES.any { contains(it, ignoreCase = true) }

        /** `alohomora.db-wal` -> `alohomora.db`, so excludes match the logical database. */
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
    }
}
