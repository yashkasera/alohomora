package io.github.yashkasera.alohomora.devtools

import platform.Foundation.NSUserDefaults

internal class IosDebugConfigStore(
    private val defaults: NSUserDefaults = NSUserDefaults(suiteName = "alohomora_debug_config"),
) : DebugConfigStore {

    override fun get(key: String): String? = defaults.stringForKey(key)

    override fun set(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }

    override fun remove(key: String) {
        defaults.removeObjectForKey(key)
    }
}
