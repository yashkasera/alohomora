## What

<!-- One-line summary of what this PR does. -->

## Why

<!-- What problem does it solve? Link to an issue if applicable. -->

## How

<!-- Brief description of the approach. Skip if obvious from the diff. -->

## Checklist

- [ ] Builds without errors (`./gradlew assemble`)
- [ ] Tests pass (`./gradlew :alohomora-common:jvmTest :desktopApp:test`)
- [ ] API compatibility verified (`./gradlew apiCheck`) — or `apiDump` run if public API changed
- [ ] Noop parity maintained (`./gradlew consumerParity`) — if `Alohomora` object was modified
- [ ] Screenshots/recordings attached (for UI changes)
