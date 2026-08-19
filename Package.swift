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
let package = Package(
    name: "AlohomoraKit",
    platforms: [.iOS(.v15)],
    products: [
        .library(
            name: "AlohomoraKit",
            targets: ["AlohomoraKit"]
        ),
    ],
    targets: [
        .binaryTarget(
            name: "AlohomoraKit",
            url: "https://github.com/yashkasera/Alohomora/releases/download/v1.0.0/AlohomoraKit.xcframework.zip",
            checksum: "bcc4731574f21c594773659c641c8ceb71260eecf5bf3dfb659ee16422c30042"
        ),
    ]
)
