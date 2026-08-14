# Alohomora No-Op

Zero-overhead implementation of Alohomora for release builds.

## What this module does

`alohomora-noop` keeps the same **public API surface** as `alohomora`, but all functions are no-ops
at runtime. This lets you keep the same source calls across debug and release variants.

## Dependency setup

```kotlin
dependencies {
    debugImplementation("io.github.yashkasera:alohomora:1.0.0")
    releaseImplementation("io.github.yashkasera:alohomora-noop:1.0.0")
}
```

## Supported public APIs

Every member of `alohomora`'s `Alohomora` object is mirrored here, so a single source call compiles
against both artifacts. Parity is enforced by `./gradlew consumerParity`.

Lifecycle:

- `Alohomora.init()`

Capture:

- `Alohomora.recordTraffic(...)`
- `Alohomora.recordEvent(name, properties?)`
- `Alohomora.recordError(throwable, place?)`
- `Alohomora.recordError(reason, stackTrace?, place?)` — for Swift and other non-`Throwable` callers
- `Alohomora.recordSpan(traceId, spanId, name, startEpochNanos, endEpochNanos, ...)`
- `Alohomora.recordSpan(name, durationNanos, attributes?)`

Feature flags:

- `Alohomora.recordFeatureFlag(...)`
- `Alohomora.setFeatureFlags(flags, source?)`
- `Alohomora.clearFeatureFlags()`

Console & server:

- `Alohomora.setShakeToOpenEnabled(enabled)`
- `Alohomora.startDevToolsServer(port?)` — returns `false` in the no-op
- `Alohomora.stopDevToolsServer()`

Database inspection:

- `Alohomora.registerAppDatabase(name, path?)`
- `Alohomora.excludeAppDatabase(name)`
- `Alohomora.clearAppDatabaseOverrides()`

Traffic replay:

- `Alohomora.registerReplayHandler(handler)`
- `Alohomora.clearReplayHandler()`
- `Alohomora.isReplaySupported` — always `false` in the no-op

Custom screen plugins:

- `Alohomora.registerPlugin(plugin)`
- `Alohomora.unregisterPlugin(pluginId)` — returns `false` in the no-op
- `Alohomora.getPlugins()` — returns an empty list in the no-op

## Notes

- Internal-only APIs are intentionally not exposed here.
- Use `Alohomora.registerPlugin(...)`/`unregisterPlugin(...)`/`getPlugins()` instead of internal
  registries.
- No-op methods do not perform logging, persistence, network tracing, or server startup.

## GitHub Packages publishing/consumption

Use the GitHub Maven registry:

- URL: `https://maven.pkg.github.com/<owner>/<repo>`
- Auth via Gradle properties or environment variables:
  - `gpr.user` / `GITHUB_ACTOR`
  - `gpr.key` / `GH_PACKAGES_TOKEN` / `GITHUB_TOKEN`
