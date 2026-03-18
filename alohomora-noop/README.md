# Alohomora No-Op

Zero-overhead implementation of Alohomora for release builds.

## What this module does

`alohomora-noop` keeps the same **public API surface** as `alohomora`, but all functions are no-ops at runtime. This lets you keep the same source calls across debug and release variants.

## Dependency setup

```kotlin
dependencies {
    debugImplementation("io.github.yashkasera:alohomora:1.0.0")
    releaseImplementation("io.github.yashkasera:alohomora-noop:1.0.0")
}
```

## Supported public APIs

- `Alohomora.init(...)`
- `Alohomora.log(...)`
- `Alohomora.recordTrace(...)`
- `Alohomora.recordTelemetry(...)`
- `Alohomora.startDevToolsServer(...)`
- `Alohomora.stopDevToolsServer()`
- `Alohomora.registerAppDatabase(...)`
- `Alohomora.excludeAppDatabase(...)`
- `Alohomora.clearAppDatabaseOverrides()`
- `Alohomora.registerPlugin(...)`
- `Alohomora.unregisterPlugin(...)`
- `Alohomora.getPlugins()`

## Notes

- Internal-only APIs are intentionally not exposed here.
- Use `Alohomora.registerPlugin(...)`/`unregisterPlugin(...)`/`getPlugins()` instead of internal registries.
- `CustomValueStore` is not part of the public API contract.
- No-op methods do not perform logging, persistence, network tracing, or server startup.

## GitHub Packages publishing/consumption

Use the GitHub Maven registry:

- URL: `https://maven.pkg.github.com/<owner>/<repo>`
- Auth via Gradle properties or environment variables:
  - `gpr.user` / `GITHUB_ACTOR`
  - `gpr.key` / `GH_PACKAGES_TOKEN` / `GITHUB_TOKEN`
