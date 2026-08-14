// swift-tools-version: 5.9
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
            checksum: "0000000000000000000000000000000000000000000000000000000000000000"
        ),
    ]
)
