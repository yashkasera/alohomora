package io.github.yashkasera.alohomora.data.model

/**
 * Always null: see `AlohomoraInitializer.discoverConfig`, which runs first and has the `Context`.
 *
 * Returning a bundled-manifest config here would be actively wrong — it would beat the Gradle
 * plugin's generated config on the manual `init()` path and report `"unknown"` git metadata for a
 * build that has the real thing compiled in.
 */
internal actual fun discoverPlatformBuildConfig(): AlohomoraConfig? = null
