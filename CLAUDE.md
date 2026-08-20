# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this
repository.

## What This Project Is

Alohomora is a developer observability and debugging toolkit for Android/iOS apps, delivered as a
Kotlin Multiplatform library. It captures traffic, distributed traces, events, database state,
cache, and errors from a running debug app and streams them in real time to a companion Compose
Desktop app via a custom binary TCP protocol over ADB port forwarding.

## Build Commands

```bash
./gradlew assemble                    # Build all modules
./gradlew test                        # Run all tests
./gradlew :alohomora:iosSimulatorArm64Test   # Library tests, iOS  (commonTest + iosTest)
./gradlew :alohomora:testAndroidHostTest     # Library tests, Android host (commonTest + androidHostTest)
./gradlew :desktopApp:run             # Run the desktop companion app

# Packaging
./gradlew :desktopApp:packageDmg      # macOS .dmg
./gradlew :desktopApp:packageMsi      # Windows .msi
./gradlew :desktopApp:packageDeb      # Linux .deb

# Showcase app
./gradlew :showcaseApp:assembleDebug
./gradlew :showcaseApp:installDebug

# Publishing
./gradlew publishMavenCentral         # Publish all five artifacts to Maven Central
./gradlew publishToMavenLocal         # Publish to Maven Local for verification

# API compatibility (run before any public API change)
./gradlew apiCheck                    # Verify binary API compatibility
./gradlew apiDump                     # Update .api golden files
```

**Env vars required for publishing:** `mavenCentralUsername`/`mavenCentralPassword` (Sonatype
Central Portal) and `signing.keyId`/`signing.password`/`signing.secretKeyRingFile` (GPG).

**Five artifacts ship, not four.** `publishMavenCentral` covers `:alohomora`, `:alohomora-noop`,
`:alohomora-common`, `:alohomora-ui` *and* `alohomora-gradle-plugin`. The plugin cannot go in the
root `publishedProjects` list because it is an included build, not a subproject — a
`:alohomora-gradle-plugin:` task path does not resolve — so the root reaches it through
`gradle.includedBuild(...)`. It went unpublished through v1.0.0 for exactly this reason while
`docs/setup.html` documented a `plugins { id(...) version "1.0.0" }` block that no external consumer
could resolve. Keep the plugin on the one publish entry point rather than a second CI invocation.

Because `signAllPublications()` is unconditional, **`publishToMavenLocal` fails without a signing
key** ("no configured signatory") — for every module, not just the plugin. To verify a publication
locally without credentials, pass `-x` for the `sign*Publication` tasks, or supply a throwaway key
via `-PsigningInMemoryKey`.

The plugin ships to **both** Maven Central and the Gradle Plugin Portal, so a consumer's default
`gradlePluginPortal()` resolves it with no settings change. CI needs four secrets:
`MAVEN_CENTRAL_USERNAME`/`MAVEN_CENTRAL_PASSWORD` + `GPG_*` for Central, and
`GRADLE_PUBLISH_KEY`/`GRADLE_PUBLISH_SECRET` for the Portal.

## Module Structure

| Module                    | Type                              | Purpose                                                                                                                                           |
|---------------------------|-----------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| `alohomora`               | KMP library (Android + iOS)       | Full debug-time library: public API, Room DB, DevTools TCP server, Koin DI, Compose UI                                                            |
| `alohomora-noop`          | KMP library (Android + iOS + JVM) | Production no-op mirror of the same public API. Zero runtime overhead.                                                                            |
| `alohomora-common`        | KMP library (Android + iOS + JVM) | Shared data models, Room entities, binary protocol types. Used by both `alohomora` and `desktopApp`.                                              |
| `alohomora-ui`            | KMP library (Android + iOS + JVM) | Shared Compose UI components (incl. the trace waterfall), icons, and theming. Depends on `alohomora-common` for the models the waterfall renders. |
| `alohomora-gradle-plugin` | Gradle plugin (included build)    | Code-generates `AlohomoraBuildGenerationInfo` from Git metadata at build time.                                                                    |
| `desktopApp`              | JVM Compose Desktop app           | Standalone Mac/Windows/Linux companion. Connects via ADB, renders all captured data.                                                              |
| `showcaseApp`             | Android application               | Sample integration. Uses `alohomora-noop` intentionally (plugin switches which lib is used per variant).                                          |

## Architecture

### Debug/Release split

Both `alohomora` and `alohomora-noop` expose the same `object Alohomora` public API. Consumers use
`debugImplementation("...alohomora")` and `releaseImplementation("...alohomora-noop")`. The noop is
R8-optimized away in release builds and only depends on `compose.runtime`.

**The noop module must mirror every public method in `alohomora`'s `Alohomora` object exactly.**

### Auto-initialization

`AlohomoraInitializer : Initializer<Unit>` (AndroidX Startup) auto-runs at app start — no manual
`init()` call needed on Android. It discovers `AlohomoraConfig` via `ServiceLoader`. The config
implementation is code-generated by the Gradle plugin and registered via a `META-INF/services` file.

### Gradle plugin: build-time config injection

`GenerateAlohomoraConfigTask` runs `git log`, `git rev-parse`, and `git status --porcelain` at build
time, embedding branch, commit SHA, dirty flag, recent commits, Slack webhook URL, and version
metadata into a generated `AlohomoraBuildGenerationInfo.kt`. Plugin extension:

```kotlin
alohomora {
    enabledVariants = setOf("debug")
    maxCommits = 50
    slackWebhookUrl = "..."
    versionName = project.version.toString()
    versionCode = 1
}
```

**Only AGP's public `com.android.build.api.*` surface may be used here.** This repo pins
`android.newDsl=false` (gradle.properties), so the legacy `com.android.build.gradle.BaseExtension`
resolves *inside* the repo and `showcaseApp` builds fine — while an AGP 9 consumer on defaults fails
configuration the instant the plugin is applied ("Extension of type 'BaseExtension' does not exist").
Nothing in the build catches this: `showcaseApp` is the only consumer `check` sees, and it inherits
the flag. `configureReleaseVerification` hit it and now takes aapt2 from
`androidComponents.sdkComponents.aapt2`, which is DSL-agnostic, lazy, and resolves AGP's own bundled
binary instead of a hand-built `build-tools/<version>/aapt2` path that also assumed
`buildToolsVersion` was set. **Verify plugin changes against a scratch consumer that does not set
`newDsl`, not just `showcaseApp`.**

### iOS build-metadata injection

The plugin has no iOS counterpart because an `.xcodeproj` build has no Gradle build to hook.
`scripts/alohomora-build-info.sh` runs from an Xcode run-script phase and writes the same git
metadata into the app bundle as `alohomora-build-info.json`; `discoverPlatformBuildConfig()` (
`PlatformBuildConfig.ios.kt`) reads it at `Alohomora.init()`. **The shell script
and `BundledBuildInfo` are one contract with no compiler between them** — `BundledBuildInfoTest`
parses a verbatim copy of the script's output and is the only thing that catches a field rename on
one side. Two rules the two producers share, both load-bearing:

- **Commit timestamps are milliseconds.** `git log --pretty=%ct` emits seconds; both producers
  multiply once on the way out, or every commit renders as a 1970 date.
- **The Slack webhook is Debug-only.** The script drops it unless `CONFIGURATION` is `Debug`,
  mirroring the Gradle task's `debuggable` gate — a plaintext resource in an `.ipa` is recoverable
  with `unzip`.

The Android `actual` returns null on purpose: `AlohomoraInitializer` runs first with the `Context`
and finds the plugin's generated config via `ServiceLoader`, and a bundled manifest must not beat it
on the manual `init()` path. `versionName`, `versionCode`, `packageName` and `appName` are read
from `Bundle.main` rather than injected, so there is no second source of truth to drift.

Note `apiDump` after adding an internal `@Serializable` class to `:alohomora`: the Compose
compiler's `$stableprop` symbols leak into the klib dump even for internal types, so `klibApiCheck`
fails on what is not an API change at all.

### Error capture

`Alohomora.recordError(...)` for caught failures, plus a crash handler installed once from
`initInternal` (`installCrashHandler`, `expect`/`actual`). Four constraints, none cosmetic:

- **Chain, never replace.** Each actual keeps the handler it displaced and calls it in a `finally`.
  Alohomora arrives via `debugImplementation`, so swallowing the exception would disable the host
  app's Crashlytics/Sentry *in debug builds only* — a symptom nobody traces back to a debugging
  library. `CrashHandlerTest` guards this.
- **Persist synchronously, but bounded.** The process is one stack unwind from death, so
  `scope.launch` would never be scheduled; the actuals use `runBlocking`.
  `ErrorCapture.FATAL_TIMEOUT_MILLIS` caps it, because a SQLite write can block on another thread's
  lock and an unbounded wait on the crashing thread turns a crash into an ANR — no stack trace *and*
  a hang.
- **`claimFatal()` is one-shot and never reset.** A crash raised by the crash handler would
  otherwise re-enter forever.
- **Write both an `Error` row and an `App.Exception` event.** The row owns the stack trace; the
  event puts the failure in sequence next to the traffic around it. `EventItem` renders events with
  that name using a left error-coloured accent bar and error-coloured text. Errors additionally
  reach the desktop on their own `STREAM_ERROR` message — the event mirror predates it and is kept
  because the timeline position is worth having.

`ErrorCapture.toError` formats `reason` as `SimpleName: message` — `simpleName`, not
`qualifiedName`, because the latter is not universally supported on Kotlin/Native and a reflection
failure *inside a crash handler* would replace the app's real crash with ours. Read it back with
`Error.exceptionTypeName()`, never by hand: the two error screens used to inline
`substringAfterLast(".").substringBefore(":")`, whose order made any message containing a period
render a blank title.

iOS covers **Kotlin exceptions only**. `NSException` and Swift `fatalError` need
`NSSetUncaughtExceptionHandler` / signal handlers, which Alohomora deliberately does not install —
competing for those is how a debug library breaks a host app's crash reporter. Swift callers use the
`recordError(reason:stackTrace:place:)` overload, which exists because a Swift `Error` is not a
`KotlinThrowable`.

### Trace capture

`Alohomora.recordSpan(...)` is the entire ingestion surface. **Alohomora depends on no tracing SDK,
and
must not gain one.** The host app writes a ~15-line adapter from whatever tracer it already runs;
the
README carries ready-made ones for OpenTelemetry and Sentry, and `showcaseApp` wires the OTel one.

This is the `registerReplayHandler` shape, not the `TrafficInterceptor` shape, and the distinction
is the
whole design. An interceptor exists because capture must happen *inside* the request pipeline —
there is
no other way to see a body. A tracer already holds the span data and only needs somewhere to hand
it.
Every candidate hook is vendor-specific (OTel `SpanExporter`, Sentry `beforeSendTransaction`,
Datadog's
own tracer, Firebase Performance with no export hook at all), so a library-side adapter would serve
one
vendor while dragging its SDK into two published modules — the same argument that keeps
`retrofitReplayHandler` out. Shipping the adapter also bought a `CompletableResultCode` lifecycle
(uncompleted results stall `BatchSpanProcessor` 30s per batch), a `compileOnly` dependency in both
`:alohomora` and `:alohomora-noop`, hand-maintained noop parity for a class `apiCheck` cannot see,
and two
code paths where iOS needed `recordSpan` anyway. All of it evaporates because `recordSpan` is
fire-and-forget, so an adapter's `export` can return `ofSuccess()` immediately.

- **Timestamps are epoch nanoseconds, and the adapter converts.** The one domain that does not
  follow the
  milliseconds rule. Milliseconds would be wrong twice: a sub-millisecond span is a zero-width
  waterfall
  bar, and ordering *within* a millisecond collapses, so five sequential 200 µs calls render as five
  simultaneous ones — a wrong picture, not a coarse one. There are already two producers that
  disagree
  (OTel emits nanos, Sentry fractional seconds as a `Double`), which is why every field carries
  `Nanos` in
  its name and why `TraceWindow`/`formatOffset` never see a bare `time`.
- **`kind` and `statusCode` are `String`, not enums.** They carry the source tracer's vocabulary, so
  an
  unrecognised value must round-trip rather than fail to decode. `spanBarColor` maps unknown kinds
  to the
  internal colour for the same reason.
- **Tree assembly is rebuilt, never mutated** (`buildTraceTree`). A parent span ends *after* its
  children
  and export order is not guaranteed, so a child routinely arrives before its parent: orphans are
  promoted
  to roots and flagged, and re-parent themselves for free on the next rebuild. Do not make this
  incremental. Cycles terminate via a visited set, because `recordSpan` is public and a hand-written
  adapter can produce one.
- **Skew is surfaced, not corrected.** A span whose end precedes its start renders as instantaneous
  with a
  `SKEW` chip; a child outside its parent's bounds is marked, never clamped. Clamping fabricates
  data and
  hides the bug someone opened the panel to find.
- **Bar geometry lives in `TraceWindow.barGeometry`, not in the draw scope.** The minimum-width
  clamp is
  what keeps instantaneous and sub-pixel spans visible — most instrumented calls are under one pixel
  of a
  1-second trace — and a draw scope is unreachable from a test.
- **`SpanStore` evicts whole traces, never individual spans.** Trimming a flat list mid-trace leaves
  survivors *permanently* parentless, since the parent was evicted rather than merely late. A
  partial
  trace that looks live is worse than no trace.
- **Trace grouping has exactly one implementation** (`List<Span>.toTraceSummaries()` in
  `alohomora-common`). Deliberately no SQL aggregate on the device: the desktop has no database and
  must
  group in Kotlin anyway, and two implementations of one definition is how the consoles came to
  disagree
  on an error row's title before `exceptionTypeName()` was shared. The cost is that the mobile list
  covers
  the latest `SPAN_SNAPSHOT_LIMIT` spans rather than all history, which is what the desktop already
  shows.
- **`SpanCaptureRegistry` is a one-way latch** reported as
  `InitialStatePayload.spanCaptureSupported`.
  Since no SDK can be inspected, it is the only way to tell "this app has no tracer" from "no traces
  yet",
  and it gates `REQUEST_TRACE_SPANS` so a newer desktop never waits on an older app.

### DevTools TCP protocol

Custom framed binary protocol defined in `alohomora-common`: 9-byte header (`magic=0x414C4F48`,
`version=1`, `payloadLength`) + JSON payload → `DevToolsEnvelope(type, sequence, payload)`. *
*`alohomora-common` is a shared protocol contract** — changes here affect both the in-app library
and the desktop app.

Message types: `STREAM_EVENT`, `STREAM_API_LOG`, `SNAPSHOT_DATABASE`, `SNAPSHOT_PREFS`,
`REQUEST_INITIAL_STATE`, `REQUEST_DATABASE_SCHEMA`, `REQUEST_DATABASE_TABLE`, `REQUEST_PREF_VALUE`,
`REQUEST_CLEAR`, `REQUEST_REPLAY_TRACE`, `REPLAY_RESULT`, `PING`, `PONG`, `STREAM_ERROR`,
`STREAM_SPAN`, `SNAPSHOT_TRACE_SPANS`, `REQUEST_TRACE_SPANS`.

**Liveness (`PING`/`PONG`):** the device pings an idle client every
`DevToolsHeartbeat.PING_INTERVAL_MILLIS` and reaps `activeConnection` once the client has sent
nothing for `SILENCE_TIMEOUT_MILLIS`. The desktop mirrors it, dropping the session when the device
stops pinging. Both use the shared `DevToolsLiveness`, and both count *any* inbound frame — the ping
exists only to manufacture one on an otherwise idle session.

This is not interchangeable with a socket read timeout, and one must not be added: after auth the
desktop legitimately sends nothing for minutes while it only receives streams, so a bare read
timeout kills healthy idle sessions (it was tried and reverted). TCP keepalive is no help either —
on an `adb forward` the device's socket is to the on-device adb daemon over loopback, and it stays
genuinely healthy after the host process at the far end of the USB transport is gone. Only an
end-to-end round trip separates "idle but alive" from "dead peer". Without one, the device held its
single connection slot forever and rejected every later client until the app was restarted.

The device pings only a client that sets `AuthResponseMessage.heartbeatSupported`, and the desktop
arms its watchdog only after the first `PING` arrives. Both gates default to the old behaviour,
because a peer predating the heartbeat ignores `PING` as an unknown type — indistinguishable from a
dead one. **Never enforce silence against an unarmed peer.**

`REPLAY_RESULT` is the only reply to a desktop→device command. Every other command either answers
with a snapshot or is unobservable, but a replay can fail before any traffic exists (no handler
registered, an unparseable hand-edited URL, a refused connection), and with no reply the desktop
would wait forever on a traffic entry that is never coming. Device-side capabilities that a client
must not assume — currently `InitialStatePayload.replaySupported` — default to `false`, so a newer
desktop hides the action against an older app rather than sending into a void.

### Adding a DevTools message type

`STREAM_ERROR` is the worked example. **Do not bump `DevToolsProtocol.VERSION`** — additive changes
do not need it, and bumping breaks interop with every existing build. What makes additive safe, and
what a new type must preserve:

- An unknown type deserializes to `UnknownMessage` and both sides ignore it (`else -> Unit`). Never
  disconnect on one.
- A new field on an existing payload needs a **default**, so a peer that omits it still decodes.
  `InitialStatePayload.errors` defaults to empty and `RequestClearMessage.errors` to false for
  exactly this reason.
- A new device capability defaults to *unsupported*, the way `replaySupported` does, so a newer
  desktop hides an action rather than sending into a void.

`ErrorStreamProtocolTest` covers all three directions: round-trip, a snapshot from an older app with
the field absent, and an unknown type degrading. That last one has to use a fictional type name —
the current build knows its own types, so it cannot impersonate an older peer any other way.

### Library internals

**Entry point:** `Alohomora` object singleton holds a Koin instance and a `CoroutineScope`.
`AlohomoraInternal` is the internal counterpart used by interceptors.

**DI:** Koin `appModule` provides repositories, use cases, ViewModels, `DevToolsRuntime`, and
`SlackShareService`. `platformModule` is `expect`/`actual` — Android provides Room database builder,
`AndroidCacheInspector`, `AndroidAppDatabaseProvider`, `DevToolsTcpServer`, and `ShareManager`.

**Persistence:** `AlohomoraDb : RoomDatabase` (currently `version = 4`) with five entities (
`TrafficEntry`, `Event`, `Error`, `Screen`, `Span`). Uses bundled SQLite. The database undergoes a
`PRAGMA quick_check` health validation at startup; corrupt databases are deleted and recreated.

**Layering:** `data/datasource/local/` (Room DAOs) → `data/repository/` (impls) →
`domain/repository/` (interfaces, base `Repository<T, ID>`) → `domain/usecase/` (one operation per
class) → `presentation/ui/screens/` (Compose screens + Koin ViewModels).

**Network interception:**

- OkHttp: `TrafficInterceptor : okhttp3.Interceptor` — attach to `OkHttpClient.Builder`
- Ktor: `AlohomoraInspector` — a Ktor `ClientPlugin` hooking `onRequest`/`onResponse`

**Traffic replay:** A captured traffic can be edited and re-sent, from the mobile console or the
desktop. **Alohomora never sends it** — the host app registers a `TrafficReplayHandler` and the
request goes back through the app's own client, so its interceptors regenerate whatever they derive
per-request. This is not an implementation shortcut: a payload signature computed at capture time is
invalid the moment the body is edited, and the redaction below means secret headers are not
recoverable from a traffic entry at all.

```kotlin
Alohomora.registerReplayHandler(okHttpReplayHandler(client))  // OkHttp, incl. Retrofit
Alohomora.registerReplayHandler(ktorReplayHandler(client))    // Android + iOS, Ktor
Alohomora.registerReplayHandler { request -> /* … */ }        // signing above the client
```

There is deliberately **no Retrofit handler and no built-in fallback.** Retrofit has no interceptors
of its own — it delegates to the `OkHttpClient` passed to `Retrofit.Builder().client(...)`, so
`okHttpReplayHandler` on that client *is* the Retrofit path, and adding a `retrofitReplayHandler`
would only pull a Retrofit dependency into `:alohomora` and `:alohomora-noop` to save one line. A
default fallback built on a client Alohomora constructs itself was considered and rejected: it would
carry none of the app's interceptors, so a signed payload would fail auth and read as a bug in the
feature rather than a fallback doing its best. `isReplaySupported` is false until the app registers
something, and both consoles hide the action rather than offering one that cannot work.

Rules that keep replay honest — none are cosmetic:

- **Never mark a replay with a wire header.** `okHttpReplayHandler` uses a `ReplayTag`,
  `ktorReplayHandler` an `AlohomoraReplayOfKey` attribute. `ReplayMarker.HEADER` exists for handlers
  with no out-of-band channel and has to be stripped on the way out, which only works if capture
  runs *before* the app's signing interceptor. Get that order wrong and the signature covers a
  header that is then removed.
- **Refuse what cannot be reproduced.** `TrafficEntry.replayBlockedReason()` gates the action: a
  truncated body (`requestBodyTruncated`) would send silently corrupted data, and
  `UNABLE_PARSE_MESSAGE` bodies (multipart, streaming, one-shot) are placeholders.
- **Drop `[REDACTED]` header values, never forward them.** `ReplayHeaders.sanitize` removes them
  along with `Content-Length`/`Host` and anything in `additionalStripList`, so the app's own
  interceptors resupply them.
- The replay's response is not returned to the caller. Capture records it like any other request,
  stamped with `replayOf`; the consoles find it from there.

**Plugin system:** `CustomScreenPlugin` interface allows embedding custom Compose screens into
Alohomora's navigation. Managed by `PluginRegistry`, routed via `Routes.Extension(extensionId)`.

**TCP server:** `DevToolsRuntime` starts a TCP server (default port 53999). One active connection at
a time. On connect: sends initial state snapshot, then streams new events and traffic via
`DevToolsStreamAdapter` (key-based deduplication). All guarded by `isDebugBuild` expect/actual.

### Mock rules and network throttling

`NetworkRuleEngine` (device-side, in `:alohomora`) evaluates mock rules before a request leaves the
app. `findMatch(url, method)` iterates `CompiledMockRule` wrappers (lazy-compiled regex, cached) and
returns the first match. If the matched body contains `{{`, `TemplateEngine.resolve()` replaces
placeholders with fresh values per-request. The engine lives in `DevToolsRuntime` and receives rules
from the desktop via `SET_MOCK_RULES`.

**`TemplateEngine`** (`alohomora-common/.../mock/TemplateEngine.kt`) is a single
`resolve(template): String`
function. Regex `\{\{(\w+)(?:\(([^)]*)\))?\}\}` dispatches to `MockGenerators`. Generators are pure
Kotlin — `kotlin.uuid.Uuid.random()`, `kotlin.random.Random`, `kotlinx.datetime` — no platform deps.
Unknown placeholders pass through as-is.

**`MockRule`** (`alohomora-common/.../NetworkRule.kt`) carries `id`, `name`, `enabled`,
`urlPattern`,
`isRegex`, `method`, `statusCode`, `responseBody`, `contentType`. Also in `NetworkRule.kt`:
`ThrottleProfile` with five presets (NONE, EDGE, SLOW_3G, FAST_3G, SLOW_WIFI).

**Desktop-side persistence** (`desktopApp/.../data/local/MockSessionStore.kt`): file-backed at
`~/.alohomora/mock-sessions/` with an `index.json` + per-session `{uuid}.json`. Auto-save with
500 ms debounce via cancel-and-relaunch Job. Last active session restored on init.

**Import/export:**

- `MockExportEnvelope` (`.alohomora-mocks.json`) — `version`, `name`, `exportedAt`, `rules`
- `HarImporter.importHar(json)` — parses HAR 1.2, keeps only 2xx entries with a body
- `TrafficEntry.toMockRule()` — one-click mock creation from captured traffic

**UI:** `MockRulesSideSheet` (master, 50% width) with session selector, save/import/export actions,
rule list. `EditMockRuleSideSheet` (detail, 40% width) with JSON editor and "Insert generator"
dropdown. `NetworkRulesActions` in the top bar shows throttle preset dropdown, mock-rules chip
(with session name and active count), and VPN state chip.

### Deep link builder

`DeepLinkBuilderSideSheet` (`desktopApp/.../panels/DeepLinkBuilderSideSheet.kt`) is a top-level side
sheet (40% width) with two tabs via `AlohomoraPrimaryTabRow`:

- **Builder** — scheme dropdown (https, http, deeplink, content, custom), host, port, path, query
  params (add/remove rows), fragment, live URL preview via `AlohomoraCodeBlock`, "Open on device"
  and
  "Reset" buttons. `parseUrl()` / `buildUrl()` handle decomposition and recomposition.
- **History** — scrollable list from `DeepLinkHistoryStore`. Click populates builder and switches
  tab.
  Play fires directly. Trash removes individual entries. "Clear all" at the top.

**Persistence:** `DeepLinkHistoryStore` (`desktopApp/.../data/local/DeepLinkHistoryStore.kt`) writes
to `~/.alohomora/deeplink-history.json`. `add()` deduplicates, prepends, caps at 50 entries.

**Entry points:** Dashboard toolbar button, Command Palette ("Deep Link Builder" in DEVICE
category).
State `showDeepLinkBuilder` lives in `DevToolsDesktopApp`.

### Command palette

`CommandPalette` (`desktopApp/.../components/CommandPalette.kt`) opens via `Cmd/Ctrl+K`. A modal
overlay with a search field, grouped action list (NAVIGATION, GENERAL, DEVICE, DATA categories),
keyboard navigation (arrow keys + Enter), and shortcut chips.

`buildCommandActions()` assembles the full action list from callbacks. Categories:

- **NAVIGATION** — one entry per visible `DesktopSection`, with `Cmd+1..9` shortcuts
- **GENERAL** — theme toggle, help, zoom in/out/reset
- **DEVICE** — screenshot, force stop, launch, clear data, reboot, Wi-Fi/data toggle, clear logcat,
  deep link builder. All gated on `deviceReady` (connected + device selected)
- **DATA** — clear traffic, clear traces, clear events. Gated on `isConnected`

### Desktop app architecture

Manual DI (no Koin): `DesktopAppComposition` constructs all stores (`MockSessionStore`,
`DeepLinkHistoryStore`), repositories, use cases, and ViewModels. `LauncherScreen` dialog lets users
select an ADB device and open per-device windows. Each window owns its own `DesktopAppComposition`.
ADB port forwarding sets up the TCP tunnel from host to device. Includes an embedded terminal
panel (`LocalTerminal`, `TerminalView`) via pty4j.

### MCP server (desktop-only)

`desktopApp/.../mcp/` exposes the captured data as MCP tools over **Streamable HTTP on loopback** —
the
**first and only inbound listener** on the desktop side (everything else is an outbound TCP client
of
the in-app DevTools server). Uses the official `io.modelcontextprotocol:kotlin-sdk` with a
`ktor-server-cio` engine we supply at the catalog's Ktor `3.5.1` (the SDK ships no engine).

- **App-scoped, not per-window.** `AlohomoraMcpServer` is one instance for the whole application,
  started/stopped from `Main.kt`'s `application {}` scope by a `LaunchedEffect` on the Settings
  toggle
  — so N open device windows produce exactly one listener. Data is per-device-window, so
  `DeviceSessionRegistry` (kept in step with `Main.kt`'s `sessions` via `snapshotFlow`) bridges the
  two,
  and every tool takes an optional `deviceId` (defaulting to the sole session, else requiring one).
- **Tool logic is a pure layer.** `AlohomoraMcpToolData` holds functions over a `DevToolsRepository`
  returning `JsonElement`; the `addTool` handlers in `AlohomoraMcpTools` are thin adapters (parse
  args →
  resolve device → project → wrap). One code path, unit-testable with `FakeDevToolsRepository`. It
  reuses
  `alohomora-common` `@Serializable` models directly and hand-projects the desktop-side models
  (build/git/cache/database), which is also where `BuildInfo.slackWebhookUrl` is dropped — the one
  secret
  that must never be served.
- **Loopback + Origin-checked.** Binds `127.0.0.1` with the SDK's DNS-rebinding protection on;
  `allowedOrigins` is passed **explicitly** (loopback hosts) because a null `allowedOrigins` skips
  Origin
  validation. Origin-less requests (a CLI agent) are allowed, a browser Origin outside loopback gets
  403.
- **Write tools are a second opt-in.** `buildServer()` registers read tools + prompts always, and
  `AlohomoraMcpWriteTools` only when `writeEnabled()` is true (read fresh at each session-create, so
  the
  toggle takes effect on the next connection). Write tools route through the **same code paths as
  the
  UI**: replay/clear via `DevToolsRepository`, but mocks/throttle via `NetworkRulesViewModel` (its
  `_mockRules`/`_throttleProfile` are the UI's source of truth — going straight to the repo would
  desync
  it), which is why `DeviceSessionHandle` carries the view model too. The one destructive tool,
  `clear_captured`, awaits `McpConfirmationBroker.confirm()` — an app-scoped Allow/Deny dialog
  window —
  before running; note the wire trap that `clearCaptured(traces=…)` clears *traffic*, `spans` clears
  trace spans.
- **Prompts + discovery.** `McpPrompts` registers `triage`/`debug_request`/`explain_screen` (canned
  read-tool flows). `McpClientConfig` builds the copy-paste connect snippet per client (Claude Code
  native http, Cursor url-only, Claude Desktop via the `mcp-remote` npx bridge) for the Settings
  picker.
- **Prefs:** `DesktopMcpPrefs` (`java.util.prefs`, node `.../desktop/mcp`) persists `enabled`,
  `writeEnabled`, and `port` (all default off / **53900**; DevTools itself uses 53999).
- **Tool logic stays a pure layer.** `AlohomoraMcpToolData` (reads) and `AlohomoraMcpWriteData` (
  writes)
  are functions over the repo/view-model returning `JsonElement`/`WriteResult`; the `addTool`
  handlers
  are thin adapters, unit-testable with `FakeDevToolsRepository`. `BuildInfo.slackWebhookUrl` is
  dropped
  in the build-metadata projection — the one secret that must never be served. The mock/throttle
  write
  path is view-model-backed (disk-coupled `MockSessionStore`), so it's covered by manual E2E, not
  units.

## Compose UI Architecture

Both mobile and desktop use Compose for the DevTools UI. **The mobile console runs inside the debug
app; the desktop app connects to the device via ADB and TCP.** This means the Compose UI code is
shared (`alohomora-ui`), but the hosting context differs sharply.

### Mobile (Android/iOS) UI

- `alohomora/src/commonMain/kotlin/.../presentation/ui/screens/` — shared Compose screens
- `alohomora/src/androidMain/` — Android-specific activity hosting, sheet presentation, keyboard
  handling
- `alohomora/src/iosMain/` — iOS-specific UIKit wrapper (uses `ComposeViewController` for bottom
  sheet, main navigation)
- Each screen is a `@Composable` that reads from a ViewModel in `presentation/viewmodel/`
- All UI components are in `alohomora-ui/src/commonMain/kotlin/.../ui/components/` and follow
  Material Design 3 tokens

### Desktop UI

- `desktopApp/src/main/kotlin/.../presentation/ui/` — desktop-specific panels and screens
- Manual DI (no Koin); each desktop window gets its own `DesktopAppComposition` instance
- Panels are stateful and receive mutations from `DevToolsViewModel` (not ViewModels)
- `AlohomoraTopBar` is shared; each panel implements its own content

### Design tokens

All spacing, radius, colors, and typography come from `alohomora-ui/.../theme/`. **Full reference:
[`alohomora-ui/DESIGN_SYSTEM.md`](alohomora-ui/DESIGN_SYSTEM.md)** (token tables, component catalog,
do/don't, contribution guide).

- **Dimens** (`MaterialTheme.dimens`, `Dimens.kt`): `margin` (xs 4 / sm 8 / md 12 / lg 16 / xl 20 /
  xxl 24 / xxxl 32 / huge 48 / fab 88), `icon` (xs 12 / sm 14 / md 16 / lg 20 / standard 24 / xl
  36 /
  illustration 80), `stroke` (thin 0.5 / small 1 / medium 2). **There is no `corner` token** —
  corners
  are `Shape`s, not `Dp`s (see below).
- **Shapes** (`MaterialTheme.shapes`, `Shape.kt`): `extraSmall` 4 / `small` 8 / `medium` 12 /
  `large` 16 / `extraLarge` 28, plus `AlohomoraBottomSheetShape` (top corners only). This is the
  single corner scale; a former parallel `AlohomoraDimens.Corner` was deleted.
- **Typography** (`MaterialTheme.typography`, `Type.kt`): **not** stock M3 fonts. Three bundled
  families, one `Regular` face each — Instrument Serif (`display*`/`headline*`), Newsreader
  (`title*`), JetBrains Mono (`body*`/`label*`). Weight is pinned to `Normal` everywhere; **emphasis
  is carried by size and colour, never `FontWeight`** (a fake weight has no bundled face and Skia
  synthesises it differently per platform).
- **Colors:** Material roles via `MaterialTheme.colorScheme` (primary, surface, onSurface,
  onSurfaceVariant, outline, scrim, surfaceContainer*, …), plus semantic status tokens via
  `MaterialTheme.alohomoraColors` (`accent`, `success`, `successContainer`, `warning`, `info`,
  `fatal`). Five themes (`material`, `monochrome`, `dracula`, `nord`, `solarized`) × light/dark,
  selected through `AppTheme(themeId, isDark)`.

Use tokens everywhere. A hardcoded `dp` or `Color(0xFF…)` is a refactoring smell. Prefer the
`Alohomora*` component wrappers over raw `androidx.compose.material3.*`.

### Icons (Lucide)

Icons are hand-translated Lucide paths using `ImageVector.Builder` with `arcToRelative` for curves.
All icon files are in `alohomora-ui/src/commonMain/kotlin/.../ui/icons/`:

```kotlin
val Icons.MyIcon: ImageVector
    get() {
        if (_myIcon != null) return _myIcon!!
        _myIcon = ImageVector.Builder(
            name = "MyIcon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, /* ... */) {
                moveTo(12f, 2f)
                lineTo(12f, 22f)
                // etc.
            }
        }.build()
        return _myIcon!!
    }

private var _myIcon: ImageVector? = null
```

To add a new icon: find it on [https://lucide.dev/icons/](https://lucide.dev/icons/), copy the SVG
path data, translate `<path>` and `<circle>` elements into `moveTo()`, `lineTo()`,
`arcToRelative()`, etc., and wrap in `path { ... }` block. Reference the existing icons for curve
translation patterns.

## Platform-Specific Notes

### iOS

- **Test on physical device only** — the Simulator has incomplete Compose support and permissions,
  leading to false negatives
- `alohomoraURLSessionConfiguration()` is required to intercept URLSession; `URLSession.shared`
  cannot be intercepted
- iOS sheet presentation uses `UIViewControllerRepresentative` wrapping the Compose
  `ComposeViewController`
- The nav stack on iOS is managed by SwiftUI, not Compose (outer shell in Swift, inner console in
  Compose)

### Android

- `AlohomoraInitializer : Initializer<Unit>` auto-runs via AndroidX Startup; no manual call needed
- DevTools can overlay on any Activity or fall back to a notification if no foreground Activity
  exists
- `DevToolsDatabaseInspector.android.kt` directly queries the Room database; iOS has a separate
  `actual` implementation
- Traffic, events, and database schemas survive app backgrounding and rotation

### Desktop

- Embedded terminal (`TerminalView` via pty4j) is **desktop-only** and not available on mobile
- ADB port forwarding handles TCP tunneling; the desktop app never has raw socket access to the
  device
- Device selection is per-window; closing a window closes the ADB tunnel for that device

## Key Conventions

- **`expect`/`actual` boundaries:** `DevToolsDatabaseInspector`, `DevToolsTcpServer`,
  `isDebugBuild`, `platformModule`, `CacheRepositoryImpl`, `DatabaseRepositoryImpl`, `ShareManager`
  all have platform-specific `actual` implementations. **When modifying these, update all three (
  Android, iOS, Desktop).**
- **API validation:** Only `alohomora` and `alohomora-noop` are API-validated (not `desktopApp`,
  `showcaseApp`, `alohomora-common`, `alohomora-ui`). Run `./gradlew apiCheck` before any commit
  that changes the public surface; run `./gradlew apiDump` to update the `.api` golden files.
- **Noop parity is enforced by `./gradlew consumerParity`**, which the root `check` depends on. It
  diffs the `Alohomora` object's member list out of both klib dumps and fails on divergence, so a
  mismatch no longer waits for a consumer's release build to surface. It depends on both `apiCheck`
  tasks, because comparing two stale dumps would report parity that does not exist — so run
  `apiDump` after any public change, then `consumerParity`. Types `alohomora` re-exports from
  `alohomora-common` (`ReplayRequest`, `CustomScreenPlugin`) appear in the noop dump but not in
  `alohomora`'s; that asymmetry is fine, the task only compares the object's own members.
- **Room migrations:** `AlohomoraDb` sets `exportSchema = false` and both platforms use
  `fallbackToDestructiveMigration(true)`, so there are no schema JSON files to update and no
  migrations to write — but **bumping `version` wipes every captured traffic entry, event, error and
  span on the next launch.** Bump it (with a comment saying what changed) whenever an entity gains
  or loses a column; skipping the bump crashes at startup instead.
- **Slack integration** exists in both `alohomora` (mobile) and `desktopApp` via
  `SlackShareService`.
- **Naming:** The console uses consistent terminology across all three platforms (Traffic, Traces,
  Events, Database, Errors, Cache, Config, Git History). Names should not be repeated in UI (e.g.,
  top bar says "Traffic", content should not re-title itself).
- **Vocabulary:** Use the same terms everywhere:
  - Network requests = **Traffic** (not "Trace", "API Logs", or "Traffic Logs")
  - Distributed traces = **Traces**. One record is a **Trace** (every span sharing a `traceId`); its
    parts are **Spans**. "Trace" now means *only* this. Identifiers that used `trace` to mean
    traffic
    were renamed (`TrafficDetailsSideSheet`, `selectedTrafficForSheet`, `trafficId`); the wire names
    `REQUEST_REPLAY_TRACE`, `RequestClearMessage.traces` and `ReplayResultMessage.sourceTraceId`
    keep
    the old spelling for interop and are the **only** exceptions. Note the trap:
    `RequestClearMessage.traces` clears *traffic* — spans are cleared by `.spans`.
  - The time-sliced span view is the **waterfall** (not "gantt" or "timeline")
  - Re-sending a captured traffic = **Replay** (not "resend", "retry" or "relay")
  - User/system events = **Events** (not "Telemetry")
  - Database + key-value store = **Database** (not "Vault")
  - Crashes/exceptions = **Errors** (not "Crashes" or "Exceptions")
  - Preferences/SharedPreferences/UserDefaults = **Cache**
  - Build metadata = **BuildMetadata** (or **Config**)
  - Git history = **GitHistory** (records are `GitHistoryCommit`)
  - API response mocking = **Mock Rules** (not "stubs" or "fakes"). One rule is a **MockRule**
  - Network simulation = **Throttling** (not "network conditioning" or "traffic shaping"). Presets
    are **ThrottleProfile** values
  - `{{placeholder}}` syntax in mock bodies = **Generators** or **Templates** (resolved by
    `TemplateEngine`)

## Authentication & Connection Flow

The console implements trust-on-first-use (TOFU) authentication:

1. Desktop connects to device with a probe (empty token if no prior pairing, or a stored token)
2. Device responds with either `AuthOtpRequiredMessage` (if new device) or `AuthSuccessMessage` (if
   recognized)
3. If new, desktop shows `OtpPromptDialog` asking for the 4-digit code the device displays
4. Desktop sends `AuthResponseMessage` with the code
5. Device validates, generates a token, and responds with `AuthSuccessMessage` carrying the new
   token
6. Desktop stores the token locally and can reconnect silently next time

**Key:** The wire-level message shapes decide the entire flow. `AuthOtpRequiredMessage` is
essential — without it, the device window would land in `AwaitingAuth` with no input rendered. See
`AuthHandshakeTest` for the contract.

## Testing Notes

- **There is no `:alohomora:test` task.** The library is a KMP module with no JVM target, so its
  tests run per platform:
  - `./gradlew :alohomora:iosSimulatorArm64Test` — `commonTest` + `iosTest`
  - `./gradlew :alohomora:testAndroidHostTest` — `commonTest` + `androidHostTest`
- **`commonTest` only reaches Android because `withHostTest {}` is set** on the android target in
  `alohomora/build.gradle.kts`. Without it the build emits a *warning* and silently compiles
  `commonTest` for iOS alone, so the Android half of every `expect`/`actual` pair goes untested. If
  you add a platform actual, add a test that runs on that platform — `CrashHandlerTest` is in
  `androidHostTest` precisely because `Thread.setDefaultUncaughtExceptionHandler` has no iOS
  analogue.
- **Compose UI tests cannot live in `commonTest`.** `runComposeUiTest` reads
  `android.os.Build.FINGERPRINT` on the Android host and NPEs in a plain JVM unit test;
  `ComposeTest` is in `iosTest` for that reason. Robolectric would be the alternative and is not
  worth a test-only Android runtime in the library.
- **Process-global one-shot state needs a test reset.** `ErrorCapture.claimFatal()` is deliberately
  never reset in production, so `resetFatalClaimForTest()` exists and both error tests call it in
  `@BeforeTest`. Without that, whichever test ran a crash handler first decided the other's result.
- **No commas in backticked test names.** Kotlin/Native rejects them (
  `Name contains illegal characters: ","`), so any test in a source set that compiles for iOS —
  `commonTest` in either `:alohomora` or `:alohomora-common` — fails to compile while `jvmTest`
  passes happily. Reword rather than reach for a comma; only a full `check` (or an iOS target
  compile) catches it.
- **`alohomora-common` now has a `commonTest` source set**, run with
  `./gradlew :alohomora-common:jvmTest`. Note there is **no `withHostTest {}`** on its android
  target, so those tests reach the JVM and iOS targets but *not* the Android host — fine for the
  pure trace logic that lives there (`TraceTree`, `TraceTimeScale`, `TraceSummary`, `SpanSelfTime`),
  but do not put an `expect`/`actual` test there expecting Android coverage.
- **Prefer a pure test over a Compose one, and move the arithmetic out to make that possible.** The
  waterfall's minimum-bar-width clamp originally lived inside a `DrawScope`, where nothing could
  reach it; it moved to `TraceWindow.barGeometry` purely so `TraceTimeScaleTest` could assert it.
  `TraceWaterfallTest` is deliberately three tests covering only what a real composition can show (a
  zero-duration span still gets laid out, collapse hides a subtree, a click reports the right span).
- **Desktop tests:** `./gradlew :desktopApp:test` (includes connection, message serialization, HAR
  import, mock export round-trip, and traffic-to-mock mapping tests)
- **Mock/template tests:** `TemplateEngineTest` and `MockGeneratorsTest` are in `alohomora-common`'s
  `commonTest` — run with `./gradlew :alohomora-common:jvmTest`. They cover plain passthrough,
  single/multiple placeholders, parameterised generators, unknown placeholder passthrough, and
  output format validation. The "no commas in backticked names" rule applies here too.
- **Flaky UI tests:** The Compose test harness can be brittle with timing. If a test flakes,
  increase timeouts or break the assertion into smaller steps
- **Message round-trips:** Use `DevToolsProtocol.encodeEnvelope()` and `decodeFrame()` to verify
  message serialization round-trips (see `AuthHandshakeTest` for the pattern)

### Instrumentation (device) tests

The console UI is only exercisable on a device — `runComposeUiTest` NPEs on the Android host, as
above — so it is covered by two instrumented suites, split by what each can actually observe.

```bash
./gradlew :alohomora:connectedAndroidDeviceTest    # the console, seeded and driven directly
./gradlew :showcaseApp:connectedDebugAndroidTest   # the library inside a real host app
```

**Physical device only.** No Gradle Managed Device is configured; the emulator system image is a
multi-GB download and nothing here needs a reproducible headless target yet. Turn *off* "Don't keep
activities" before running — it destroys backgrounded activities and breaks every
navigate-and-return assertion.

- **`check` compiles them but never runs them.** The connected tasks hang off `connectedCheck`/
  `deviceCheck`, and the KMP device-test component is built without a Kotlin `testRegistry`, so
  `allTests` misses it too. Root `check` therefore depends on `assembleAndroidDeviceTest` and
  `assembleDebugAndroidTest` — enough to type-check every device test with no device attached.
- **`androidDeviceTest` does not see `commonTest`.** AGP gives the device-test compilation a `None`
  source-set tree, unlike `androidHostTest` which sits on the `test` tree. Shared fixtures live in
  `androidDeviceTest` itself (`ConsoleTestRule`, `Seed`, `ConsoleHost`, `ConsoleAssertions`). Do not
  force the tree name to "fix" it — that drags every `commonTest` file into a device compilation.
  `internal` declarations are visible regardless: AGP puts the main compilation's classes.jar on
  `friendPaths`, which is what lets a test open `Routes.TrafficDetails(id)` directly. A declaration
  in this source set that names an `internal` type in its *signature* must itself be `internal`.
- **`withDeviceTest {}` now depends on `withHostTest {}`.** AGP's device-test DSL info reads
  `androidTestOnJvmOptions!!.enableCoverage`; removing the host-test block NPEs at configuration
  time. `applicationId` inside `withDeviceTest {}` is dead code in AGP 9.2.1 — the test APK is
  always `<namespace>.test`.
- **`Alohomora.config` is null in `:alohomora`'s device tests** — the Gradle plugin is not applied
  to the library itself, so `ServiceLoader` finds no `AlohomoraConfig`. Init still succeeds; every
  consumer is null-safe. But it means **Config, Git History and the Slack affordance can only be
  covered from `:showcaseApp`**, where the plugin generates a real config into the debug variant.
  In `:alohomora` they get empty-state tests only.
- **Seed through the repositories, not `Alohomora.record*`.** The public ingestion methods are
  fire-and-forget on `Dispatchers.Default`, which the Compose clock cannot see, so an assertion
  straight after one races. `Seed` writes through the internal repositories inside `runBlocking`;
  `RecordApiTest` covers the public path and polls for exactly this reason.
- **No fatal-crash tests on device.** `installCrashHandler` runs from a `ContentProvider` before the
  runner exists, so the handler it chains to is ART's `KillApplicationHandler`: a genuinely uncaught
  exception kills the process and takes the whole class with it. The chaining contract stays in
  `androidHostTest/CrashHandlerTest`. showcaseApp's "Crash" FAB is tagged but deliberately never
  tapped; `ShowcaseTestTags.RECORD_ERROR` drives the caught path instead.
- **Reset in `@Before`, never `@After`.** A test that fails mid-way skips its own cleanup and the
  next one inherits its rows. `ConsoleTestRule` does all of it on the way in.
- **The whole run shares one `Alohomora`.** `initInternal` returns early once `koinApplication` is
  set and there is no teardown, so one Koin container, one Room file (which survives between runs)
  and one `PluginRegistry` serve every class. Never call `Alohomora.init()` from a test — the
  startup provider already did, and a second call is a silent no-op.
- **Test tags come from `AlohomoraTestTags`** in `alohomora-ui` — public so both suites and any
  consumer can address the console, and free of `.api` churn because `alohomora-ui` is not
  api-validated. Putting tags in `:alohomora` `commonMain` instead *would* change
  `alohomora.klib.api`.
- **A tagged `AlohomoraTextField` is not the editable node.** The caller's modifier lands on a
  wrapping `Column`; the `SetText` action belongs to the `BasicTextField` beneath, and Compose does
  not merge an editable field into its parent. Use `onTextFieldIn(tag)`, not
  `onNodeWithTag(tag).performTextInput(...)`.
- **Infinite animations hang `waitForIdle()`.** `ConnectionStatusDot` pulses via
  `infiniteRepeatable` when connected, so no test may put the console into a connected state without
  taking manual control of `mainClock`.

<!-- code-review-graph MCP tools -->

## MCP Tools: code-review-graph

**IMPORTANT: This project has a knowledge graph. ALWAYS use the
code-review-graph MCP tools BEFORE using Grep/Glob/Read to explore
the codebase.** The graph is faster, cheaper (fewer tokens), and gives
you structural context (callers, dependents, test coverage) that file
scanning cannot.

### When to use graph tools FIRST

- **Exploring code**: `semantic_search_nodes_tool` or `query_graph_tool` instead of Grep
- **Understanding impact**: `get_impact_radius_tool` instead of manually tracing imports
- **Code review**: `detect_changes_tool` + `get_review_context_tool` instead of reading entire files
- **Finding relationships**: `query_graph_tool` with callers_of/callees_of/imports_of/tests_for
- **Architecture questions**: `get_architecture_overview_tool` + `list_communities_tool`

Fall back to Grep/Glob/Read **only** when the graph doesn't cover what you need.

### Key Tools

| Tool                             | Use when                                               |
|----------------------------------|--------------------------------------------------------|
| `detect_changes_tool`            | Reviewing code changes — gives risk-scored analysis    |
| `get_review_context_tool`        | Need source snippets for review — token-efficient      |
| `get_impact_radius_tool`         | Understanding blast radius of a change                 |
| `get_affected_flows_tool`        | Finding which execution paths are impacted             |
| `query_graph_tool`               | Tracing callers, callees, imports, tests, dependencies |
| `semantic_search_nodes_tool`     | Finding functions/classes by name or keyword           |
| `get_architecture_overview_tool` | Understanding high-level codebase structure            |
| `refactor_tool`                  | Planning renames, finding dead code                    |

### Workflow

1. The graph auto-updates on file changes (via hooks).
2. Use `detect_changes_tool` for code review.
3. Use `get_affected_flows_tool` to understand impact.
4. Use `query_graph_tool` pattern="tests_for" to check coverage.
