package io.github.yashkasera.alohomora.devtools

interface DevToolsAppDatabaseProvider {
    fun listDatabases(): List<AppDatabaseInfo>
    fun resolvePath(name: String): String?
}

internal object EmptyAppDatabaseProvider : DevToolsAppDatabaseProvider {
    override fun listDatabases(): List<AppDatabaseInfo> = emptyList()

    override fun resolvePath(name: String): String? = null
}
