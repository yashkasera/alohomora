#!/usr/bin/env bash
# Type-checks the closure convenience layer and the bare-closure usage sample
# against the published AlohomoraKit XCFramework (no framework rebuild).
#
# Usage: Validation/typecheck.sh [path/to/AlohomoraKit.xcframework]
# With no argument, downloads the framework pinned in Package.swift.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

XCF="${1:-}"
if [[ -z "$XCF" ]]; then
  URL="https://github.com/yashkasera/Alohomora/releases/download/v1.2.0/AlohomoraKit.xcframework.zip"
  echo "==> Downloading $URL"
  curl -sL -o "$WORK/akit.zip" "$URL"
  unzip -q "$WORK/akit.zip" -d "$WORK/xcf"
  XCF="$(find "$WORK/xcf" -maxdepth 1 -name '*.xcframework' | head -1)"
fi

SLICE="$XCF/ios-arm64-simulator"
[[ -d "$SLICE" ]] || { echo "ERROR: simulator slice not found at $SLICE" >&2; exit 1; }

TARGET="arm64-apple-ios15.0-simulator"

echo "==> [1/2] Building AlohomoraKitSwift module (full typecheck incl. adapter conformances)"
xcrun -sdk iphonesimulator swiftc \
  -target "$TARGET" \
  -F "$SLICE" \
  -emit-module -emit-module-path "$WORK/AlohomoraKitSwift.swiftmodule" \
  -module-name AlohomoraKitSwift \
  "$ROOT/Sources/AlohomoraKitSwift/ClosurePluginAPIs.swift"

echo "==> [2/2] Type-checking bare-closure usage sample"
xcrun -sdk iphonesimulator swiftc \
  -target "$TARGET" \
  -F "$SLICE" -I "$WORK" \
  -typecheck -module-name Sample \
  "$ROOT/Validation/ClosureUsageSample.swift"

echo "==> OK: closure-based plugin APIs compile against the published XCFramework."
