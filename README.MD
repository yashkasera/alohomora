[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Release](https://img.shields.io/github/v/release/yashkasera/Alohomora)](https://github.com/yashkasera/Alohomora/releases/latest)
[![Build](https://github.com/yashkasera/Alohomora/actions/workflows/release.yml/badge.svg)](https://github.com/yashkasera/Alohomora/actions/workflows/release.yml)


# Alohomora

Real-time observability for debug builds of Android and iOS apps. Traffic, traces, events,
database state, errors, cache, and feature flags — captured automatically, browsable in an in-app
console and a Compose Desktop companion. Full observability in debug. Zero overhead in release.

<p>
  <a href="https://yashkasera.github.io/alohomora/">Documentation</a> &middot;
  <a href="https://yashkasera.github.io/alohomora/setup.html">Setup Guide</a> &middot;
  <a href="https://github.com/yashkasera/alohomora/releases">Releases</a>
</p>

## Quick Start

```kotlin
dependencies {
    debugImplementation("io.github.yashkasera:alohomora:1.0.0")
    releaseImplementation("io.github.yashkasera:alohomora-noop:1.0.0")
}
```

Android auto-initializes via AndroidX Startup. Shake the device to open the console.

[Full setup guide →](https://yashkasera.github.io/alohomora/setup.html) (Gradle plugin, iOS,
traffic capture, desktop app)

## Features

| | Feature | What it does |
|---|---|---|
| 📡 | **Traffic** | Every request captured with headers, bodies, timing. OkHttp + Ktor auto-capture |
| 🔁 | **Traffic Replay** | Edit and re-send through your own HTTP client, signatures regenerated |
| 📊 | **Traces** | Waterfall view of distributed spans. Adapters for OpenTelemetry, Sentry |
| 📋 | **Events** | Searchable timeline with properties |
| 🗄️ | **Database** | Browse tables, run SQL, view schema |
| 🚨 | **Errors** | Uncaught exceptions auto-captured, chains to Crashlytics/Sentry |
| 💾 | **Cache** | SharedPreferences / NSUserDefaults inspection |
| 🏴 | **Feature Flags** | Record and inspect from any provider |
| 🎭 | **Mock Rules** | Intercept requests, return custom responses with template generators |
| 🐢 | **Throttling** | Edge to Wi-Fi presets, device-wide via local VPN |
| 🔗 | **Deep Links** | Compose, fire, and replay deep links from the desktop |
| 🤖 | **MCP Server** | Expose captured data to AI coding assistants |
| ⚙️ | **Config & Git** | Branch, SHA, dirty flag, last N commits baked into the build |
| ⌨️ | **Command Palette** | Cmd+K for everything |
| 🖥️ | **Desktop Companion** | Full-screen debugging on macOS, Windows, Linux |

[Full feature docs →](https://yashkasera.github.io/alohomora/features.html)

## Architecture

Two artifacts, same API. `debugImplementation` gets full observability — Room DB, TCP server,
Compose UI. `releaseImplementation` gets empty method bodies that R8 optimises away (<10 KB).
The desktop companion connects via ADB port forwarding and renders all captured data.

## Documentation

- [Setup Guide](https://yashkasera.github.io/alohomora/setup.html) — Android, iOS, Gradle plugin, desktop companion
- [Features](https://yashkasera.github.io/alohomora/features.html) — Traffic, replay, mocks, database, errors, and more
- [Traces & Adapters](https://yashkasera.github.io/alohomora/traces.html) — OpenTelemetry, Sentry, Datadog, Firebase
- [MCP Server](https://yashkasera.github.io/alohomora/mcp.html) — Expose debug data to AI agents
- [API Reference](https://yashkasera.github.io/alohomora/api.html) — Every method on the Alohomora singleton

## What This Is Not

A metrics dashboard, a logging service, or a monitoring tool that alerts after damage is done.
It is a thinking aid for developers who debug systems, not just code.

## License

[Apache 2.0](LICENSE)
