package io.github.yashkasera.alohomora.firebase

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.common.FeatureFlag

private const val SOURCE = "Firebase Remote Config"

/**
 * Reads all activated values from [remoteConfig] and pushes them as feature flags.
 *
 * Call this after `fetchAndActivate()` completes:
 *
 * ```kotlin
 * remoteConfig.fetchAndActivate().addOnSuccessListener {
 *     Alohomora.syncFirebaseRemoteConfig(remoteConfig)
 * }
 * ```
 *
 * Uses per-source replacement, so flags from other providers are preserved.
 *
 * @param remoteConfig the app's `FirebaseRemoteConfig` instance
 * @param type optional type label for all synced flags (default: `"remote_config"`)
 */
@Suppress("unused")
fun Alohomora.syncFirebaseRemoteConfig(
    remoteConfig: FirebaseRemoteConfig,
    type: String = "remote_config",
) {
    val flags = remoteConfig.all.map { (key, value) ->
        FeatureFlag(
            key = key,
            value = value.asString(),
            source = SOURCE,
            type = type,
            metadata = buildMap {
                put("source_ordinal", value.source.toSourceLabel())
            },
        )
    }
    Alohomora.setFeatureFlags(flags, source = SOURCE)
}

private fun Int.toSourceLabel(): String = when (this) {
    FirebaseRemoteConfig.VALUE_SOURCE_STATIC -> "static"
    FirebaseRemoteConfig.VALUE_SOURCE_DEFAULT -> "default"
    FirebaseRemoteConfig.VALUE_SOURCE_REMOTE -> "remote"
    else -> "unknown"
}
