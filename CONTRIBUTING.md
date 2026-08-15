# Contributing to Alohomora

Contributions are welcome. This guide covers the workflow and checks you need to pass.

## Getting Started

1. Fork the repository
2. Clone your fork
3. Create a branch from `main`
4. Make your changes
5. Open a pull request against `main`

## Prerequisites

- JDK 17+
- Android SDK (for library and showcase builds)
- ADB on PATH (for desktop app device features)

## Build & Test

```bash
./gradlew assemble                        # Build everything
./gradlew :alohomora-common:jvmTest       # Shared model/protocol tests
./gradlew :desktopApp:test                # Desktop app tests
./gradlew apiCheck                        # Binary API compatibility
./gradlew consumerParity                  # Verify noop mirrors the real module
```

All four must pass before a PR can be merged. CI runs them automatically.

## API Changes

If you modify the public API surface of `:alohomora` or `:alohomora-noop`:

1. Run `./gradlew apiDump` to update the `.api` golden files
2. Run `./gradlew consumerParity` to verify noop still mirrors the real module
3. If you added a method to `Alohomora`, add the matching no-op in `alohomora-noop`

## Code Style

- Kotlin, following the conventions already in the codebase
- No comments unless the *why* is non-obvious
- Use design tokens from `alohomora-ui` for all UI work (no hardcoded dp/colors)
- Use existing `Alohomora*` component wrappers over raw Material 3 components

## Terminology

Use the project's vocabulary consistently. See `CLAUDE.md` → Vocabulary section for the canonical terms (Traffic, Traces, Events, Replay, etc.).

## Testing

- Pure logic tests go in `commonTest` or `jvmTest`
- No commas in backticked test names (Kotlin/Native rejects them)
- Compose UI tests go in `iosTest` (not `commonTest` — `runComposeUiTest` NPEs on Android host)
- Prefer extracting testable logic out of Compose over writing Compose tests

## Pull Request Guidelines

- Keep PRs focused — one concern per PR
- Fill in the PR template
- Add screenshots or recordings for UI changes
- Respond to review feedback promptly

## Reporting Issues

Use the issue templates for [bugs](.github/ISSUE_TEMPLATE/bug_report.yml) and [feature requests](.github/ISSUE_TEMPLATE/feature_request.yml).

For security vulnerabilities, see [SECURITY.md](SECURITY.md).
