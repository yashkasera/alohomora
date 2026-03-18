## Overview

This project is a developer observability and introspection toolkit designed to make software systems legible while they are running.

Modern applications fail less often because of syntax errors and more often because of hidden state, invisible side effects, unclear causality, and tools that only show fragments of the truth. Logs are scattered. Databases feel opaque. Events happen, but no one can see them as a system.

This tool exists to collapse that distance.

It provides a unified environment for inspecting APIs, databases, events, background jobs, and application state in real time—without requiring invasive instrumentation, fragile dashboards, or postmortem archaeology.

The goal is not monitoring theater.
The goal is understanding.

## Installation & Gradle Setup (`build.gradle.kts`)

Alohomora is designed for **full observability in debug builds** and **zero-overhead behavior in release builds**.

### 1) Add variant-specific dependencies

```kotlin
dependencies {
    // Full Alohomora runtime for local development and debugging
    debugImplementation("io.github.yashkasera:alohomora:1.0.0")

    /* no-op */ implementation for production builds (same public API)
    releaseImplementation("io.github.yashkasera:alohomora-noop:1.0.0")
}
```

### 2) GitHub Packages setup

Publish and consume from GitHub Packages:

- Repository URL: `https://maven.pkg.github.com/<owner>/<repo>`
- Credentials (Gradle properties or env vars):
  - `gpr.user` / `GITHUB_ACTOR`
  - `gpr.key` / `GH_PACKAGES_TOKEN` / `GITHUB_TOKEN`

### 3) Public API quick usage

```kotlin
Alohomora.init()

Alohomora.log(message = "App started")
Alohomora.recordTelemetry(name = "screen_opened", properties = mapOf("screen" to "home"))

Alohomora.recordTrace(
    status = 200,
    url = "https://api.example.com/users",
    method = "GET",
)

Alohomora.startDevToolsServer(8080)
Alohomora.stopDevToolsServer()

Alohomora.registerAppDatabase(name = "app.db")
Alohomora.excludeAppDatabase(name = "cache.db")
Alohomora.clearAppDatabaseOverrides()
```

### 4) Plugin APIs

```kotlin
Alohomora.registerPlugin(plugin)
Alohomora.unregisterPlugin(pluginId = "my-plugin")
val plugins = Alohomora.getPlugins()
```

For deeper implementation details, see `NOOP_IMPLEMENTATION_GUIDE.md` (if present in your checkout).

## Core Philosophy

This project is built on a few non-negotiable principles:

- Nothing important should be invisible
- Observability should be interactive, not forensic
- Errors are signals, not crimes
- Developers should be able to answer “what just happened?” in seconds, not hours

The tool favors explanation over aggregation, structure over noise, and developer agency over automation magic.

## Key Features

### API Logging & Inspection

All inbound and outbound API activity is captured as structured, queryable data.

This includes:

- Request and response bodies (with configurable redaction)
- Headers and metadata
- Timing and latency breakdowns
- Error states and retries
- Correlation IDs across services

Instead of scrolling through text logs, developers can:

- Inspect individual requests as first-class objects
- Compare multiple requests side by side
- Trace a single request across internal boundaries
- Replay requests in a controlled environment

The system treats API calls as events with context, not lines of text.

### Database Inspection & State Diffing

Databases are not black boxes here.

The tool provides:

- Live inspection of database state
- Query-level visibility (including generated ORM queries)
- Before/after snapshots for writes
- Visual diffs of record mutations
- Relationship-aware navigation between entities

Rather than asking “what query ran?”, developers can ask:

- What changed?
- When did it change?
- What triggered the change?
- What else changed as a result?

This is especially useful for debugging cascading updates, background jobs, and race conditions.

### Event Visualizer

At the heart of the project is an event-first mental model.

Everything—API calls, database writes, background jobs, cache updates, messages, webhooks—is treated as an event in time.

The event visualizer provides:

- A timeline view of system activity
- Causal chains between events
- Grouping by request, user action, or workflow
- Zoomable, filterable views from milliseconds to minutes
- The ability to “scrub” through execution history

This makes asynchronous systems understandable as stories, not mysteries.

### Request & Workflow Tracing

The tool automatically builds traces that follow execution across:

- Services
- Queues
- Workers
- Scheduled jobs
- External integrations

Traces are not just performance metrics. They are narratives:

- What started the workflow
- What steps occurred
- Where decisions were made
- Where things slowed down or failed

This is particularly useful for systems built around events, queues, or serverless functions where traditional stack traces fall apart.

### Structured Logging (Without the Pain)

Logs are treated as structured data by default.

Features include:

- Schema-aware log entries
- Automatic correlation with requests and events
- Log levels that actually mean something
- Filtering without regex spelunking
- Context propagation without manual plumbing

Plain text logs are supported, but they are not the primary interface. The system assumes logs should be queryable artifacts, not prose.

### Error & Anomaly Exploration

Errors are elevated to first-class citizens.

The tool provides:

- Grouping of related failures
- Visibility into surrounding system state
- The ability to jump from an error to the exact events that caused it
- Detection of “weird but not broken” behavior (unexpected but valid states)

Instead of asking “why did this crash?”, developers can ask:

- Why did the system think this was reasonable?
- What assumptions were violated?
- Where did reality diverge from expectation?

### Replay & Simulation (Planned / Optional)

For supported environments, the system can:

- Replay API requests or events
- Simulate workflows using recorded inputs
- Test fixes against real historical scenarios

This is not about load testing. It’s about time travel for debugging.

### Developer-Centric UI

The interface is built for developers, not executives.

Design priorities:

- Fast navigation
- Keyboard-first workflows
- Dense but readable information
- Minimal dashboards, maximal drill-down
- No charts without underlying data access

The UI assumes curiosity and rewards exploration.

## Typical Use Cases

- Debugging “it only happens sometimes” bugs
- Understanding unfamiliar legacy systems
- Tracing data corruption or phantom updates
- Inspecting third-party API behavior
- Making asynchronous workflows comprehensible
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
