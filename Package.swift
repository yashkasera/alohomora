// swift-tools-version: 5.9

// Alohomora — developer observability & debugging toolkit for mobile apps.
//
// Captures network traffic, distributed traces, events, database state, cache,
// and errors from a running debug build and streams them in real time to a
// companion desktop app over ADB / TCP.
//
// Repository : https://github.com/yashkasera/Alohomora
// Docs       : https://yashkasera.github.io/alohomora
// License    : Apache-2.0
//
// This package distributes a pre-built XCFramework for iOS.

import PackageDescription

// URL and checksum are updated automatically by the release workflow.
//
// The AlohomoraKit product vends both the binary XCFramework and AlohomoraKitSwift,
// a thin source layer that `@_exported`-imports the binary and adds closure-based
// convenience inits for the plugin handler APIs. `import AlohomoraKitSwift` gets both.
let package = Package(
    name: "AlohomoraKit",
    platforms: [.iOS(.v15)],
    products: [
        .library(
            name: "AlohomoraKit",
            targets: ["AlohomoraKit", "AlohomoraKitSwift"]
        ),
    ],
    targets: [
        .binaryTarget(
            name: "AlohomoraKit",
            url: "https://github.com/yashkasera/Alohomora/releases/download/v1.3.0/AlohomoraKit.xcframework.zip",
            checksum: "b96579b5d186b8f8371e35ad413b7b386c91239c877657ee9688280d59d5d56c"
        ),
        .target(
            name: "AlohomoraKitSwift",
            dependencies: ["AlohomoraKit"]
        ),
    ]
)
