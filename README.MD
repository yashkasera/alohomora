[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

## Overview

This project is a developer observability and introspection toolkit designed to make software systems legible while they are running.

Modern applications fail less often because of syntax errors and more often because of hidden state, invisible side effects, unclear causality, and tools that only show fragments of the truth. Logs are scattered. Databases feel opaque. Events happen, but no one can see them as a system.

This tool exists to collapse that distance.

It provides a unified environment for inspecting network traffic, databases, events and application state in real time—without requiring invasive instrumentation, fragile dashboards, or postmortem archaeology.

The goal is not monitoring theater.
The goal is understanding.

## Installation & Setup

Alohomora is designed for **full observability in debug builds** and **zero-overhead behavior in release builds**.

### Dependencies

```kotlin
dependencies {
    // Full Alohomora runtime for local development and debugging
    debugImplementation("io.github.yashkasera:alohomora:1.0.0")

    /* no-op */ implementation for production builds (same public API)
    releaseImplementation("io.github.yashkasera:alohomora-noop:1.0.0")
}
```

### GitHub Packages

Publish and consume from GitHub Packages:

- Repository URL: `https://maven.pkg.github.com/<owner>/<repo>`
- Credentials (Gradle properties or env vars):
  - `gpr.user` / `GITHUB_ACTOR`
  - `gpr.key` / `GH_PACKAGES_TOKEN` / `GITHUB_TOKEN`

### Build metadata (Android)

Apply the Gradle plugin to your app module. It runs `git log`, `git rev-parse` and
`git status --porcelain` at build time and code-generates an `AlohomoraConfig` that Alohomora picks up
automatically, feeding the Config and Git History panels. Without it those panels show `unknown`.

```kotlin
plugins {
    id("io.github.yashkasera.alohomora")
}

alohomora {
    enabledVariants = setOf("debug")   // default: setOf("debug")
    maxCommits = 50                    // default: 10
    versionName = project.version.toString()
    versionCode = 1
    slackWebhookUrl = "https://hooks.slack.com/services/…"   // optional
}
```

`versionName` has no default and the generator task requires it, so leave it out and the build fails.
`slackWebhookUrl` is emitted as a plaintext constant and is **dropped for any non-debuggable variant**
— it would otherwise be recoverable from release dex with `strings`.

For the iOS equivalent, see [iOS build metadata](#ios-build-metadata-xcode-run-script-phase).

### Public API

```kotlin
// Android auto-initializes via AndroidX Startup; call this only on iOS.
Alohomora.init()

Alohomora.recordEvent(name = "screen_opened", properties = mapOf("screen" to "home"))

// Uncaught exceptions are captured automatically; this is for ones you caught.
// See "Errors" below, including the Swift form.
Alohomora.recordError(throwable, place = "SyncWorker")

// Only for clients Alohomora does not intercept — OkHttp and Ktor capture
// automatically. See "Traffic Capture & Inspection" below.
Alohomora.recordTraffic(
    status = 200,
    url = "https://api.example.com/users",
    method = "GET",
)

// Alohomora ships no tracing SDK — you adapt whichever tracer you already run.
// Timestamps are epoch NANOSECONDS. See "Traces" below.
Alohomora.recordSpan(traceId, spanId, name = "GET /posts", startEpochNanos, endEpochNanos)
Alohomora.recordSpan(name = "image_decode", durationNanos = elapsed)

// Defaults to 53999, which is also what the desktop app defaults to.
Alohomora.startDevToolsServer()
Alohomora.stopDevToolsServer()

// Shake the device to open the console — on by default on both platforms.
// Turn it off if it clashes with a gesture your app already owns.
Alohomora.setShakeToOpenEnabled(false)

Alohomora.registerAppDatabase(name = "app.db")
Alohomora.excludeAppDatabase(name = "cache.db")
Alohomora.clearAppDatabaseOverrides()

// Lets the console re-send a captured request through your client, so your
// interceptors re-sign an edited payload. See "Traffic Replay" below.
Alohomora.registerReplayHandler(ktorReplayHandler(myHttpClient))
Alohomora.clearReplayHandler()
val canReplay = Alohomora.isReplaySupported
```

### Custom screen plugins

```kotlin
Alohomora.registerPlugin(plugin)
Alohomora.unregisterPlugin(pluginId = "my-plugin")
val plugins = Alohomora.getPlugins()
```

### iOS setup

Nothing on iOS is automatic. There is no AndroidX Startup equivalent, and traffic is not captured
unless your app uses a session Alohomora hands you.

The library ships as a **static framework named `AlohomoraKit`, built by Gradle**. There is no
podspec and no `Package.swift` yet, so the app target links it from the Gradle output directory.
`showcaseIosApp` is a working reference for all of this. Add a Run Script phase before *Compile
Sources*:

```sh
cd "$SRCROOT/.."
./gradlew :alohomora:embedAndSignAppleFrameworkForXcode
```

and point the target at what it produces:

```
FRAMEWORK_SEARCH_PATHS = $(SRCROOT)/../alohomora/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)
```

Initialize at launch. **Kotlin default arguments do not become Swift defaults**, so every parameter is
required at the Swift call site — and `init()` is exported as `doInit()` because Swift reserves
`init`:

```swift
import AlohomoraKit

@main
struct MyApp: App {
    init() {
        Alohomora.shared.doInit()
        _ = Alohomora.shared.startDevToolsServer(port: 53999)
    }
    // …
}
```

#### Traffic capture

`URLSession.shared` **cannot** be intercepted: its configuration is immutable, and capture requires
Alohomora's protocol class to appear in a session's own `protocolClasses`. Build your session from
Alohomora's configuration instead:

```swift
let session = URLSession(configuration: Alohomora.shared.alohomoraURLSessionConfiguration())

// …or wrap a configuration you already customize
let session = URLSession(
    configuration: Alohomora.shared.alohomoraURLSessionConfiguration(base: myConfig)
)
```

Ktor's Darwin engine is not captured automatically either — pass the same configuration into its
engine config.

`registerURLProtocol()` hooks only the legacy `NSURLConnection` loading system, which URLSession
ignores, so it is not a substitute for the above. Background upload/download tasks and WebSocket
frames are not captured at all.

#### Opening the console

**Shake the device.** Shake-to-open is built in on both platforms and on by default — no wiring
needed. A shake toggles the console the library presents for you. Opt out if it clashes with a
gesture your app already owns:

```swift
Alohomora.shared.setShakeToOpenEnabled(enabled: false)
```

To present it yourself instead — from a toolbar button, say — `MainViewController` is a top-level
function, so Swift reaches it through `MainKt`:

```swift
struct ConsoleView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainKt.MainViewController()
    }

    func updateUIViewController(_ controller: UIViewController, context: Context) {}
}
```

**Present it with `.presentationDragIndicator(.visible)`.** iOS has no system back button, and a
Compose view inside a SwiftUI sheet consumes the vertical drag gesture, so the sheet's
swipe-to-dismiss never fires — the grab indicator is the reliable way out. (The shake-presented
console needs none of this: another shake closes it.)

### iOS build metadata (Xcode run-script phase)

On Android the Gradle plugin code-generates branch, commit sha, dirty flag and recent commits into the
build, and Alohomora picks them up automatically. An iOS app built from an `.xcodeproj` has no Gradle
build for the plugin to hook, so the same metadata is collected by a script phase instead. Without it
the Config panel shows `unknown` and GitHistory is empty; everything else works.

Add a **Run Script** phase to your app target, placed **after** *Copy Bundle Resources*:

```sh
"$SRCROOT/../scripts/alohomora-build-info.sh"
```

Then:

- **Untick "Based on dependency analysis"** and leave *Output Files* empty. Git state changes without
  any input file changing, so a cached phase ships yesterday's commit sha.
- **Set `ENABLE_USER_SCRIPT_SANDBOXING = NO`** if your project has it on (the Xcode 15+ template
  default). The sandbox blocks reading `.git`, and the manifest ends up with no git metadata.
- Nothing to add to *Copy Bundle Resources* — the script writes straight into the app bundle Xcode is
  assembling, so nothing generated lands in git.

Optional environment variables:

| Variable | Default | Purpose |
|---|---|---|
| `ALOHOMORA_MAX_COMMITS` | `50` | How much history GitHistory shows |
| `ALOHOMORA_SLACK_WEBHOOK_URL` | unset | Enables Slack sharing. **Ignored unless `CONFIGURATION` is `Debug`** — it would otherwise ship as plaintext in the `.ipa`, recoverable with `unzip`. |

`versionName`, `versionCode` and the bundle id are deliberately *not* injected: Alohomora reads them
from `Bundle.main` at runtime, so `MARKETING_VERSION` and `CURRENT_PROJECT_VERSION` stay the single
source of truth. A missing, stale or corrupt manifest is logged and ignored — it never fails
`Alohomora.init()`.

For deeper implementation details, see `NOOP_IMPLEMENTATION_GUIDE.md` (if present in your checkout).

## Core Philosophy

This project is built on a few non-negotiable principles:

- Nothing important should be invisible
- Observability should be interactive, not forensic
- Errors are signals, not crimes
- Developers should be able to answer “what just happened?” in seconds, not hours

The tool favors explanation over aggregation, structure over noise, and developer agency over automation magic.

## Key Features

### Traffic Capture & Inspection

All outbound network activity is captured as structured, queryable data.

Every entry carries:

- Method, URL, status, latency and response size
- Request and response headers, with secrets redacted at capture
- Request and response bodies, the response rendered as a navigable JSON tree
- Scheme, host, query and timestamp

What you can do with one:

- Open it in a detail view split across Overview, Request and Response tabs
- Search the list, and clear it
- Share it to Slack as text or as a ready-to-run cURL command
- **Replay** it — edit the method, URL, headers or body and send it again through your own client
  (see below)

Capture is automatic for OkHttp (`TrafficInterceptor`) and Ktor (`AlohomoraInspector`). On iOS it
requires the session configuration described above. `Alohomora.recordTraffic(...)` covers clients
Alohomora does not intercept.

### Database Inspection

Both Alohomora's own capture database and databases your app registers are browsable live.

- Pick a database, then a table, and page through rows
- Run arbitrary SQL and see results with execution time
- Read the schema: columns, types, primary keys, indexes

Register the databases you want visible:

```kotlin
Alohomora.registerAppDatabase(name = "app.db")
Alohomora.excludeAppDatabase(name = "cache.db")
```

Inspection is **read-only**. There is no row editing, no write history, and no before/after diffing —
you see current state, and SQL you run yourself.

### Events

Events are the app's own narration: a name, a timestamp, and optional string properties.

```kotlin
Alohomora.recordEvent(name = "checkout_started", properties = mapOf("cart_size" to "3"))
```

The panel lists them newest-first with search by name, a toggle to expand properties inline, and
clear. It is a searchable log of what your app said happened, in order — not an inferred causal graph.

### Cache

Key-value state, read live off the device.

- **Android:** `SharedPreferences` files, including `EncryptedSharedPreferences` (already decrypted by
  the system when read, and flagged as encrypted in the listing)
- **iOS:** `NSUserDefaults`

Keys load lazily — click one to fetch its value. Read-only; there is no editing or deletion.

### Config & Git History

Which build you are actually looking at, which is the first question worth asking when a bug will not
reproduce.

- **Config:** project name, variant, version name and code, branch, commit sha, dirty flag, build
  timestamp
- **Git History:** the last N commits compiled into the build — sha, author, subject, timestamp

Injected at build time — see [Build metadata (Android)](#build-metadata-android) and
[iOS build metadata](#ios-build-metadata-xcode-run-script-phase).

### Mock Rules & Network Throttling

The desktop app can intercept matching requests on the device and return canned responses instead of
hitting the real server. Rules are defined in the desktop, pushed to the device over the DevTools
protocol, and evaluated inside `NetworkRuleEngine` before the request leaves the app.

A rule carries a URL pattern (substring or regex), an optional HTTP method filter, a status code, a
content type, and a response body. Rules can be toggled individually or all at once.

#### Dynamic response bodies

Response bodies support `{{generator}}` placeholders that are resolved per-request on the device, so
each response carries fresh data:

| Syntax | Output |
|--------|--------|
| `{{uuid}}` | `550e8400-e29b-41d4-a716-446655440000` |
| `{{name}}` | `Jane Smith` |
| `{{firstName}}` / `{{lastName}}` | `Jane` / `Smith` |
| `{{email}}` | `jane.smith@example.com` |
| `{{int(1,100)}}` | `42` |
| `{{float(0,1)}}` | `0.7342` |
| `{{amount(10,500)}}` | `247.83` (2 decimal places) |
| `{{date(past,30)}}` | `2026-07-15` (ISO-8601) |
| `{{date(future,365)}}` | `2027-05-20` |
| `{{timestamp}}` | `1723456789000` (epoch millis) |
| `{{bool}}` | `true` or `false` |
| `{{oneOf(active,inactive,pending)}}` | `inactive` |

Unknown placeholders pass through as-is, so a body that happens to contain `{{` for some other
reason is not corrupted.

#### Persistent mock sessions

Mock rules are saved to `~/.alohomora/mock-sessions/` as JSON files. Sessions auto-save with a
500 ms debounce after every rule change. The last active session is restored on launch, so rules
survive restarts without manual re-entry.

You can maintain multiple named sessions and switch between them — one set of rules per API
environment, per feature branch, per bug reproduction.

#### Import and export

- **Export** a session as `.alohomora-mocks.json` to share with teammates or check into a repo
- **Import** from `.alohomora-mocks.json` or from a **HAR 1.2** file (only 2xx responses with a
  body are imported as rules)
- **Create a mock from any captured request** — one click in the traffic detail panel pre-fills a
  rule with the request's path, method, status, content type and response body

#### Network throttling

Five presets simulate degraded network conditions: Edge (500 ms / 50 KB/s), Slow 3G (200 ms /
100 KB/s), Fast 3G (100 ms / 300 KB/s), Slow Wi-Fi (50 ms / 1 MB/s), and None. Throttling applies
latency before the response and caps throughput on the response body.

On Android, device-wide throttling is available via a local VPN service that applies the same
profile to all traffic, not just intercepted requests.

### Deep Link Builder

The desktop app includes a deep link builder accessible from the Dashboard toolbar or the command
palette (`Cmd/Ctrl+K` → "Deep Link Builder"). It opens as a side sheet with two tabs:

- **Builder** — compose a URL from scheme, host, port, path, query parameters and fragment. A live
  preview updates as you type. "Open on device" fires the URL via `adb shell am start`
- **History** — every URL you open is persisted to `~/.alohomora/deeplink-history.json`. Click an
  entry to populate the builder; click the play button to fire it directly. Individual entries or
  the full history can be cleared

### Command Palette

`Cmd/Ctrl+K` opens a searchable command palette with every action the desktop app supports:
navigation to any panel, theme toggle, zoom controls, device actions (force stop, launch, reboot,
screenshot, Wi-Fi/data toggle, clear logcat), data actions (clear traffic/traces/events), and the
deep link builder. Each action shows its keyboard shortcut when one exists.

### Desktop-only panels

Traffic, Events, Database, Cache, Errors, Config and Git History all work for any connected device,
and so does Traces once the app has recorded a span. The desktop app adds three more that need
host-side tooling, so they are gated on device capability and hidden for an iOS device:

- **Dashboard** — live device metrics, with a quick-launch button for the deep link builder
- **Logcat** — streamed device log
- **ADB** — an embedded terminal (pty4j)

### Errors

Uncaught exceptions are captured automatically — nothing to install. The handler goes in at init and
records the exception before the app dies, with search, a stack-trace detail view,
copy-to-clipboard and clear in the console.

Caught failures you still want to see, you report:

```kotlin
try {
    sync()
} catch (e: IOException) {
    Alohomora.recordError(e)                          // place defaults to the top stack frame
    Alohomora.recordError(e, place = "SyncWorker")    // …or say something more useful
}
```

```swift
// Swift has no KotlinThrowable, so iOS uses the string form
Alohomora.shared.recordError(
    reason: "DecodingError: \(error)",
    stackTrace: Thread.callStackSymbols.joined(separator: "\n"),
    place: "PostsRepository"
)
```

**Alohomora chains to the crash handler it replaced**, so your Crashlytics or Sentry keeps working.
It never swallows the exception and the app crashes exactly as it would have.

Two limits worth knowing:

- **iOS covers Kotlin exceptions only.** An `NSException` or a Swift `fatalError` bypasses it.
  Alohomora deliberately does not install `NSSetUncaughtExceptionHandler` or signal handlers —
  competing for those is how a debug library breaks your real crash reporter.
- **Errors reach the desktop app too**, in an Errors panel with the stack trace, and mirrored into
  the Events timeline as `App.Exception` so the failure also appears in sequence.

There is still **no logging API**: `recordEvent` and `recordError` are the only ways to author a
console entry, and there are no log levels.

### Traffic Replay

Open any captured request, edit the method, URL, headers or payload, and send it again — from the
console on the device or from the desktop app.

The request is re-issued **by your app, through your own HTTP client**. That distinction is the whole
feature. Anything your client derives per-request — a payload signature, a fresh bearer token,
certificate pinning — is regenerated for the payload you just edited, so changing a body produces a
matching signature instead of a 401. A request that Alohomora built and sent itself could not do
this: it would carry the signature captured from the original body, and secret headers are stored
redacted, so they are not recoverable from a captured traffic entry at all.

Register your client once at startup:

```kotlin
// Ktor (Android and iOS)
Alohomora.registerReplayHandler(ktorReplayHandler(myHttpClient))

// OkHttp
Alohomora.registerReplayHandler(okHttpReplayHandler(myOkHttpClient))

// Anything else, or when signing happens above the client
Alohomora.registerReplayHandler { request ->
    myApi.send(request.method, request.url, request.headers, request.body)
    ReplayOutcome.Sent()
}
```

Pass the client you use for the endpoint being replayed. One without the signing interceptor produces
authentic-looking auth failures.

#### If you sign requests, name your signature headers

Capture redacts `Authorization`, `Cookie`, `X-Api-Key` and a few others, and redacted values are
dropped rather than replayed — your interceptor resupplies them. A bespoke `X-Signature` or
`X-Timestamp` is *not* on that list, so it survives into the captured entry and would be forwarded
verbatim. If your interceptor uses `addHeader` rather than `header`, the stale value then rides along
beside the fresh one. Name them once at startup:

```kotlin
ReplayHeaders.additionalStripList = setOf("X-Signature", "X-Timestamp", "X-Nonce")
```

A signature over a timestamp plus a body hash replays cleanly: your interceptor is handed the edited
request, so it hashes the new body and stamps a current timestamp. Nothing is reused from capture, and
because the timestamp differs the server will not mistake the replay for a duplicate of the original.

One caveat if you hash the body: the replayed body is reconstructed from text. JSON is UTF-8 and
round-trips byte for byte. A protobuf or otherwise binary body does not — capture decodes it as UTF-8,
so invalid sequences become replacement characters and your hash would cover corrupted bytes. Replay
is not currently blocked for that case.

#### Retrofit

Retrofit has no interceptors of its own — it delegates to OkHttp. Yours live on the `OkHttpClient` you
hand to `Retrofit.Builder()`, which is exactly why replay has to go through that client. Register it,
not the `Retrofit` instance:

```kotlin
val client = OkHttpClient.Builder()
    .addInterceptor(SigningInterceptor())   // your signing, auth, pinning…
    .addInterceptor(TrafficInterceptor())   // Alohomora capture
    .build()

val retrofit = Retrofit.Builder()
    .client(client)
    .baseUrl("https://api.example.com/")
    .build()

Alohomora.registerReplayHandler(okHttpReplayHandler(client))
```

Interceptor order does not matter for replay: the marker linking a replay to its original travels in
an OkHttp request tag, not a header, so nothing Alohomora adds can end up inside a signature.

If your signing happens *above* OkHttp — in a repository, or a Retrofit `CallAdapter`, or a manual
`sign(payload)` call in your data layer — no client-level handler can regenerate it. Use the lambda
form and call your own signing code inside it.

Until a handler is registered the action is hidden in both consoles rather than offered and broken.
It is also hidden per-request for anything that cannot be faithfully reproduced: bodies too large to
have been captured in full, and multipart or streaming bodies. Replaying a truncated body would send
silently corrupted data and make your server look at fault.

Replay is a real request with real side effects. There is no dry run.

This is not load testing. It is time travel for debugging.

### Traces

Distributed traces from the tracer your app already runs, rendered as a waterfall in the desktop app:
a trace groups spans by trace id, nests them by parent, and shows each one's name, kind, duration,
status, attributes and events.

**Alohomora depends on no tracing SDK.** It exposes one function, and you write a short adapter from
whatever tracer you have:

```kotlin
Alohomora.recordSpan(
    traceId = "4bf92f3577b34da6a3ce929d0e0e4736",   // W3C Trace Context, 32 hex chars
    spanId = "00f067aa0ba902b7",                    // 16 hex chars
    parentSpanId = null,                            // null, or 16 zeros, for a root
    name = "GET /posts",
    startEpochNanos = start,
    endEpochNanos = end,
    kind = "CLIENT",
    statusCode = "ERROR",
    statusDescription = "HTTP 404",
    attributes = mapOf("http.response.status_code" to "404"),
    events = listOf(SpanEvent("retry", epochNanos = start + 1_000_000, attributes = null)),
    scopeName = "com.example.network",
)

// For timing one block with no surrounding trace. Renders as a single-span trace, and cannot
// be parented — use the full form whenever the work is part of something larger.
Alohomora.recordSpan(name = "image_decode", durationNanos = elapsed)
```

That inversion is deliberate, and it is the same reasoning as the deliberately absent
[`retrofitReplayHandler`](#retrofit): every tracer's export hook is vendor-specific, so a
library-side adapter would serve exactly one vendor while dragging its SDK into both published
artifacts. Handing over strings and longs instead costs you fifteen lines and costs everyone else
nothing.

Two rules matter more than the rest.

**Timestamps are epoch nanoseconds, and converting is the adapter's job.** OpenTelemetry emits
nanoseconds already. Sentry emits fractional seconds as a `Double`, so multiply by `1e9`. Anything
reporting milliseconds multiplies by `1_000_000`. Get it wrong and every span renders as a 1970 date.
Nanoseconds rather than the milliseconds Alohomora uses everywhere else because a waterfall needs
sub-millisecond resolution: at millisecond precision five sequential 200 µs spans render as five
simultaneous ones, which is a wrong picture rather than a coarse one.

**Put the adapter in `src/main`, not a debug-only source set.** `alohomora-noop` mirrors `recordSpan`
as a no-op, so the same adapter compiles and runs in release and quietly discards every span — and
R8 removes the call along with the argument construction. A `debugImplementation`-only adapter would
mean maintaining a second tracer wiring for release to fall back to. This is the point of the
inversion, not a workaround for it.

`kind` and `statusCode` take your tracer's own vocabulary as free-form strings — OpenTelemetry
`SpanKind` and `StatusCode` names, Sentry's `op` and `SpanStatus`, anything. Unrecognised values are
stored as given rather than rejected, so a newer SDK's values round-trip. Use `"ERROR"` for
`statusCode` to get the failure styling.

The Traces panel stays hidden until the app has recorded its first span. Because Alohomora ships no
tracer, there is nothing it could inspect to find out whether one is wired up, so an empty panel would
be indistinguishable from an app that will never produce a trace.

#### OpenTelemetry

A `SpanExporter` is the whole adapter. `showcaseApp` runs exactly this one — see
`showcaseApp/src/main/kotlin/.../tracing/`:

```kotlin
class AlohomoraSpanExporter : SpanExporter {

    override fun export(spans: Collection<SpanData>): CompletableResultCode {
        spans.forEach { span ->
            Alohomora.recordSpan(
                traceId = span.traceId,
                spanId = span.spanId,
                name = span.name,
                // No conversion needed: OpenTelemetry already reports epoch nanoseconds.
                startEpochNanos = span.startEpochNanos,
                endEpochNanos = span.endEpochNanos,
                // A root's parent is 16 zeros here, never null. Alohomora normalises that.
                parentSpanId = span.parentSpanId,
                kind = span.kind.name,
                statusCode = span.status.statusCode.name,
                statusDescription = span.status.description,
                attributes = span.attributes.toStringMap(),
                events = span.events.map { event ->
                    SpanEvent(
                        name = event.name,
                        epochNanos = event.epochNanos,
                        attributes = event.attributes.toStringMap(),
                    )
                },
                scopeName = span.instrumentationScopeInfo.name,
            )
        }
        // Success immediately, because recordSpan is fire-and-forget — there is nothing to
        // await. Returning an *uncompleted* result is the trap: BatchSpanProcessor's worker
        // waits on it and would stall for its full 30-second export timeout per batch.
        return CompletableResultCode.ofSuccess()
    }

    override fun flush(): CompletableResultCode = CompletableResultCode.ofSuccess()

    override fun shutdown(): CompletableResultCode = CompletableResultCode.ofSuccess()
}

// OTel attribute values are also Long, Double, Boolean and List; recordSpan takes strings.
private fun Attributes.toStringMap(): Map<String, String> =
    asMap().entries.associate { (key, value) -> key.key to value.toString() }
```

Register it as one processor among however many you already have, so the same spans still reach your
collector:

```kotlin
SdkTracerProvider.builder()
    .addSpanProcessor(BatchSpanProcessor.builder(AlohomoraSpanExporter()).build())
    .addSpanProcessor(BatchSpanProcessor.builder(otlpExporter).build())
    .build()
```

Declare the OpenTelemetry dependency in **your** module. Alohomora does not, and that is the proof
nothing leaks:

```kotlin
implementation("io.opentelemetry:opentelemetry-sdk-trace:1.51.0")
```

#### Sentry

Sentry has no exporter interface. The hook is `beforeSendTransaction`, which sees each finished
transaction on its way out:

```kotlin
SentryAndroid.init(context) { options ->
    // Two parameters, transaction and Hint — a single-parameter lambda will not compile.
    options.setBeforeSendTransaction { txn, _ ->
        txn.spans.forEach { span ->
            Alohomora.recordSpan(
                traceId = span.traceId.toString(),
                spanId = span.spanId.toString(),
                parentSpanId = span.parentSpanId?.toString(),
                // op is the operation ("http.client", "db"); description is the detail.
                name = span.description ?: span.op,
                // Sentry reports FRACTIONAL SECONDS as a Double. This multiplication is the
                // whole reason this adapter is longer than the OpenTelemetry one.
                startEpochNanos = (span.startTimestamp * 1e9).toLong(),
                // timestamp is null for an unfinished span; fall back to the start so it
                // renders as instantaneous rather than in 1970.
                endEpochNanos = span.timestamp?.let { (it * 1e9).toLong() }
                    ?: (span.startTimestamp * 1e9).toLong(),
                kind = span.op,
                // SpanStatus is an enum with no isError() helper, and OK is the only value
                // that is not a failure. An unset status is neither.
                statusCode = when (span.status) {
                    null -> "UNSET"
                    SpanStatus.OK -> "OK"
                    else -> "ERROR"
                },
                statusDescription = span.status?.apiName(),
                attributes = span.data?.mapValues { it.value.toString() } ?: span.tags,
                // events is left at its default. Sentry has no per-span event concept —
                // breadcrumbs live on the transaction with no span association.
                scopeName = span.origin,
            )
        }
        // Returned unchanged. Alohomora observes; returning null here would drop the
        // transaction and stop it reaching Sentry.
        txn
    }
}
```

The transaction's own root span is not in `txn.spans` — read it off `txn.contexts.trace`
(`getTraceId()`, `getSpanId()`, and `getOperation()`, *not* `getOp()`) with `txn.transaction` as the
name and `txn.startTimestamp` / `txn.timestamp` as the bounds. Two caveats:
`io.sentry.protocol.SentrySpan` is annotated `@ApiStatus.Internal` even though `txn.spans` is public,
so keep the mapping in this one adapter rather than spreading the type around; and `txn.spans`
returns the live backing list, so do not mutate it.

#### Datadog and Firebase Performance

Neither exposes a general span-export hook, so there is no adapter to write. Call `recordSpan` at the
trace sites instead — the same place you start and stop the vendor's span:

```kotlin
val trace = FirebasePerformance.getInstance().newTrace("checkout")
trace.start()
val startNanos = System.nanoTime()
try {
    checkout()
} finally {
    trace.stop()
    // nanoTime, not currentTimeMillis: this is a duration, and the wall clock can step
    // mid-operation. The short overload stamps the wall-clock end itself.
    Alohomora.recordSpan(name = "checkout", durationNanos = System.nanoTime() - startNanos)
}
```

Spans stream to the desktop app over the DevTools protocol like everything else, and clearing the
console clears them.

### Developer-Centric UI

The interface is built for developers, not executives.

Design priorities:

- Fast navigation
- Keyboard-first workflows
- Dense but readable information
- Minimal dashboards, maximal drill-down
- No charts without underlying data access

The UI assumes curiosity and rewards exploration.

### Desktop App Keyboard Shortcuts

The companion desktop app supports keyboard-driven workflows. All shortcuts use **Cmd** on macOS and **Ctrl** on Windows/Linux.

| Shortcut | Action |
|----------|--------|
| Cmd/Ctrl+K | Open command palette |
| Cmd/Ctrl+1..9 | Switch to Nth sidebar section |
| Cmd/Ctrl+T | Toggle dark/light theme |
| Cmd/Ctrl+/ | Open keyboard shortcuts help |
| Cmd/Ctrl+N | New window (open launcher) |
| Cmd/Ctrl+W | Close window |
| Cmd/Ctrl+= | Zoom in (50%–200%) |
| Cmd/Ctrl+- | Zoom out |
| Cmd/Ctrl+0 | Reset zoom to 100% |
| Cmd/Ctrl+Shift+S | Take device screenshot (Android) |
| Cmd/Ctrl+Shift+Backspace | Clear active panel data |
| Escape | Close side sheet / dismiss dialog |

Hold Cmd/Ctrl to see number badges on sidebar items.

## Typical Use Cases

- Debugging “it only happens sometimes” bugs
- Understanding unfamiliar legacy systems
- Answering “which build is this, and what changed in it?”
- Inspecting third-party API behavior, and replaying a request until it works
- Mocking API responses to test error states, loading screens, or edge-case payloads without a
  backend change
- Testing deep links across schemes and parameters without typing `adb shell am start` by hand
- Checking what the app actually persisted, without a rebuild
- Teaching junior developers how the system actually behaves

## What This Tool Is Not

This project is deliberately not:

- A vanity metrics dashboard
- A replacement for application logic
- A black-box AI that hides reasoning
- A logging service that sells storage
- A monitoring tool that only alerts after damage is done

It is a thinking aid, not a crutch.

## Intended Audience

This tool is for:

- Developers who debug systems, not just code
- Teams working with distributed or event-driven architectures
- People who care about correctness, causality, and clarity
- Engineers who want to understand why, not just what

## Design Assumptions

Unless explicitly configured otherwise, the system assumes:

- Debuggability matters more than micro-optimizations
- Visibility is worth a small amount of overhead
- Defaults should be safe, reversible, and inspectable
- Tooling should respect developer intelligence

## Status

This project is evolving. Features are added carefully, not hurriedly.
Every new capability must make the system more understandable, not just more powerful.
