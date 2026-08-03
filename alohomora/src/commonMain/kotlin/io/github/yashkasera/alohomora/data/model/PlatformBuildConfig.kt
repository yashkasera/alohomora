package io.github.yashkasera.alohomora.data.model

/**
 * Build metadata for the running app, discovered without the host having to hand it over.
 *
 * Each platform answers this differently, and one of them declines entirely:
 * - **Android** returns null. `AlohomoraInitializer` already resolves the Gradle plugin's generated
 *   config through `ServiceLoader` before any manual `init()` could run, and that path has the
 *   `Context` (and therefore the right `ClassLoader`) this signature does not.
 * - **iOS** reads app identity from `Bundle.main` and git metadata from the manifest described in
 *   [BundledBuildInfo].
 */
internal expect fun discoverPlatformBuildConfig(): AlohomoraConfig?
