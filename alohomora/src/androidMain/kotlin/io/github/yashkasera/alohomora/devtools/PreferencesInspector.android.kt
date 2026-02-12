package io.github.yashkasera.alohomora.devtools

import android.content.Context

internal class AndroidPreferencesInspector(
    context: Context,
) : DevToolsPreferencesInspector {
    private val prefs = context.getSharedPreferences(
        "${context.packageName}_preferences",
        Context.MODE_PRIVATE
    )

    override suspend fun getAllKeys(): List<String> {
        return prefs.all.keys.sorted()
    }

    override suspend fun getValue(key: String): String? {
        val value = prefs.all[key] ?: return null
        return when (value) {
            is Set<*> -> value.joinToString(prefix = "[", postfix = "]")
            else -> value.toString()
        }
    }
}
