package io.github.yashkasera.alohomora.devtools

import platform.Foundation.NSUserDefaults

internal class IosCacheInspector : DevToolsCacheInspector {
    private val defaults = NSUserDefaults.standardUserDefaults

    override suspend fun getAllKeys(): List<String> {
        val dict = defaults.dictionaryRepresentation()
        return dict.keys.map { it.toString() }.sorted()
    }

    override suspend fun getValue(key: String): String? {
        val value = defaults.objectForKey(key) ?: return null
        return value.toString()
    }
}
