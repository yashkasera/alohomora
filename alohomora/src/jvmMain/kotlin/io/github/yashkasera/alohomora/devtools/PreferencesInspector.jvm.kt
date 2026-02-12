package io.github.yashkasera.alohomora.devtools

internal class JvmPreferencesInspector : DevToolsPreferencesInspector {
    override suspend fun getAllKeys(): List<String> = emptyList()

    override suspend fun getValue(key: String): String? = null
}
