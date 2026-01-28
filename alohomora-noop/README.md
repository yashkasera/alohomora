# Alohomora No-Op

> **Zero-overhead implementation of Alohomora for release builds** 🚀

## What is this?

This is a **no-operation (no-op)** version of the Alohomora debugging library. It provides the same API as the full library but does absolutely nothing at runtime.

## Why use this?

- ✅ **Zero runtime overhead** - All methods are empty
- ✅ **Tiny size** - No heavy dependencies (Room, Ktor, Koin, etc.)
- ✅ **Same API** - Drop-in replacement for the full library
- ✅ **ProGuard/R8 friendly** - Dead code is automatically removed

## Quick Setup

```kotlin
// In your app's build.gradle.kts
dependencies {
    // Full library for debug builds
    debugImplementation("io.github.yashkasera:alohomora:1.0.0")
    
    // No-op library for release builds
    releaseImplementation("io.github.yashkasera:alohomora-noop:1.0.0")
}
```

That's it! Your code doesn't need to change at all.

## Example

```kotlin
// This code works with BOTH versions
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // In debug: initializes full library
        // In release: does nothing (optimized away)
        Alohomora.init()
        
        // In debug: stores value
        // In release: does nothing (optimized away)
        Alohomora.putValue("api_key", "sk_test_123")
        
        // In debug: returns stored value
        // In release: returns default value
        val key = Alohomora.getString("api_key", "default")
    }
}
```

## Included APIs

All public APIs from the full library are included:

- `Alohomora.init()`
- `Alohomora.log()`
- `Alohomora.trackEvent()`
- `Alohomora.registerPlugin()`
- `Alohomora.putValue()` / `getString()` / `getInt()` / etc.
- `CustomScreenPlugin` interface
- `CustomValueStore` object

## Dependencies

Minimal dependencies compared to the full library:

```kotlin
dependencies {
    implementation("org.jetbrains.compose.runtime:runtime")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
}
```

**No** Room, Ktor, Koin, Compose UI, Material3, or other heavy libraries!

## Important: Always Provide Defaults

Since this library returns defaults for all `get*()` methods, always provide sensible defaults:

```kotlin
// ✅ Good
val timeout = Alohomora.getInt("timeout", 30)

// ❌ Bad - will be null in release builds
val timeout = Alohomora.getInt("timeout")
```

## Documentation

See the main project documentation:

- [No-Op Implementation Guide](../NOOP_IMPLEMENTATION_GUIDE.md) - Complete guide
- [Plugin System Guide](../PLUGIN_SYSTEM_GUIDE.md) - How plugins work
- [Quick Start](../PLUGIN_QUICK_START.md) - Quick reference

## License

Same license as the main Alohomora library.
