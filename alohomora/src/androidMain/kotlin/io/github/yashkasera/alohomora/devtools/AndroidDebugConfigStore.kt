package io.github.yashkasera.alohomora.devtools

import android.content.Context

internal class AndroidDebugConfigStore(context: Context) : DebugConfigStore {

    private val prefs = context.applicationContext
        .getSharedPreferences("alohomora_debug_config", Context.MODE_PRIVATE)

    override fun get(key: String): String? = prefs.getString(key, null)

    override fun set(key: String, value: String) {
        prefs.edit().putString(key, value).commit()
    }

    override fun remove(key: String) {
        prefs.edit().remove(key).commit()
    }
}
