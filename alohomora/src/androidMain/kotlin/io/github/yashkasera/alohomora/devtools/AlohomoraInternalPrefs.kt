package io.github.yashkasera.alohomora.devtools

/**
 * SharedPreferences files that hold Alohomora's own state rather than the host app's.
 *
 * The Cache inspector lists every file under `shared_prefs/` and lets a paired desktop edit them,
 * so without this exclusion it served desktop trust tokens to every paired desktop, and let a
 * one-off session write a token of its own into the trusted set for silent access later.
 */
internal object AlohomoraInternalPrefs {
    const val TRUST: String = "alohomora_devtools_trust"
    const val DEBUG_CONFIG: String = "alohomora_debug_config"

    val ALL: Set<String> = setOf(TRUST, DEBUG_CONFIG)
}
