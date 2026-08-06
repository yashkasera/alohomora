// swift-tools-version: 5.9
import PackageDescription

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
            path: "alohomora/build/XCFrameworks/debug/AlohomoraKit.xcframework"
        ),
    ]
)
