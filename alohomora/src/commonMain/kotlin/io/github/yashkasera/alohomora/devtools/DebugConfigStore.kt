package io.github.yashkasera.alohomora.devtools

internal interface DebugConfigStore {
    fun get(key: String): String?
    fun set(key: String, value: String)
    fun remove(key: String)
}
