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

        val baseNames = context.databaseList()
            .filter { name -> name.isNotBlank() && name !in excluded }

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

        val existsInBase = context.databaseList().contains(name)
        if (!existsInBase) return null
        return context.getDatabasePath(name).absolutePath
    }

    companion object {
        private val DEFAULT_EXCLUDES = setOf("alohomora.db")
    }
}
