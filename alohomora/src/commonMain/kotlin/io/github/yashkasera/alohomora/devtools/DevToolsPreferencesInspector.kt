package io.github.yashkasera.alohomora.devtools

internal interface DevToolsPreferencesInspector {
    suspend fun getAllKeys(): List<String>
    suspend fun getValue(key: String): String?
}
