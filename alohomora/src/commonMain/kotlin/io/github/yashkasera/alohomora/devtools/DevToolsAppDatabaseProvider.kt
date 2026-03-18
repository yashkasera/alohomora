package io.github.yashkasera.alohomora.devtools

import io.github.yashkasera.alohomora.common.AppDatabaseInfo

internal interface DevToolsAppDatabaseProvider {
    fun listDatabases(): List<AppDatabaseInfo>
    fun resolvePath(name: String): String?
}

internal object EmptyAppDatabaseProvider : DevToolsAppDatabaseProvider {
    override fun listDatabases(): List<AppDatabaseInfo> = emptyList()

    override fun resolvePath(name: String): String? = null
}
