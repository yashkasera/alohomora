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

/**
 * Whether the host app is a debug build, for the manual `init()` path. The fallback DevTools
 * activation uses when the build made no explicit decision (see `DevToolsActivation`).
 *
 * - **Android** returns false and is never the real answer: `AlohomoraInitializer` runs first, reads
 *   `FLAG_DEBUGGABLE` from the `Context`, and passes it to `initInternal` itself — after which a
 *   manual `init()` is a no-op.
 * - **iOS** reports a development-signed build: a simulator, or an embedded provisioning profile
 *   that grants `get-task-allow`. App Store builds carry no profile and TestFlight/ad-hoc profiles
 *   deny it, so both read as release.
 */
internal expect fun discoverHostDebuggable(): Boolean
