// Swift-native manual traffic capture.
//
// Kotlin default arguments are not bridged, so the generated
// `recordTraffic(id:status:url:…:mockedBy:)` makes a Swift caller pass all twenty arguments, with
// numbers boxed as KotlinInt/KotlinLong. This wraps it for traffic Alohomora cannot intercept
// itself: a custom networking stack, or a Flutter/React Native bridge forwarding requests made on
// the other side. Additive and non-breaking.

import Foundation

@_exported import AlohomoraKit

/// One request/response exchange to record with ``Alohomora/recordTraffic(_:)``.
///
/// Only `url` is required. Where a field is left nil it is derived from the others: scheme, host,
/// path and query from `url` (the consoles group and filter by them), content types from a
/// `Content-Type` header, and sizes from the UTF-8 length of the bodies.
public struct AlohomoraTraffic {
    public var url: String
    public var method: String
    public var status: Int?
    /// The status line's reason phrase, or an error description for a request that failed.
    public var message: String?
    public var requestHeaders: [String: [String]]?
    public var requestBody: String?
    public var requestContentType: String?
    public var responseHeaders: [String: [String]]?
    public var responseBody: String?
    public var responseContentType: String?
    /// When the request was sent. Defaults to the moment the value is created.
    public var startedAt: Date
    /// How long the exchange took, in seconds.
    public var duration: TimeInterval?
    public var requestSize: Int64?
    public var responseSize: Int64?
    /// Leave nil to have Alohomora assign one.
    public var id: String?

    public init(
        url: String,
        method: String = "GET",
        status: Int? = nil,
        message: String? = nil,
        requestHeaders: [String: [String]]? = nil,
        requestBody: String? = nil,
        requestContentType: String? = nil,
        responseHeaders: [String: [String]]? = nil,
        responseBody: String? = nil,
        responseContentType: String? = nil,
        startedAt: Date = Date(),
        duration: TimeInterval? = nil,
        requestSize: Int64? = nil,
        responseSize: Int64? = nil,
        id: String? = nil
    ) {
        self.url = url
        self.method = method
        self.status = status
        self.message = message
        self.requestHeaders = requestHeaders
        self.requestBody = requestBody
        self.requestContentType = requestContentType
        self.responseHeaders = responseHeaders
        self.responseBody = responseBody
        self.responseContentType = responseContentType
        self.startedAt = startedAt
        self.duration = duration
        self.requestSize = requestSize
        self.responseSize = responseSize
        self.id = id
    }
}

extension Alohomora {
    /// Records one exchange in the Traffic console.
    ///
    /// ```swift
    /// Alohomora.shared.recordTraffic(
    ///     AlohomoraTraffic(
    ///         url: "https://api.example.com/v1/pay",
    ///         method: "POST",
    ///         status: 200,
    ///         requestHeaders: ["Content-Type": ["application/json"]],
    ///         requestBody: payload,
    ///         responseBody: responseText,
    ///         startedAt: sentAt,
    ///         duration: elapsed
    ///     )
    /// )
    /// ```
    ///
    /// Use this only for traffic Alohomora cannot see. URLSession traffic is captured automatically
    /// through ``alohomoraURLSessionConfiguration()``, and recording it here as well would show
    /// every request twice.
    public func recordTraffic(_ traffic: AlohomoraTraffic) {
        let components = URLComponents(string: traffic.url)
        recordTraffic(
            id: traffic.id ?? "",
            status: traffic.status.map { KotlinInt(int: Int32(clamping: $0)) },
            url: traffic.url,
            message: traffic.message,
            method: traffic.method,
            scheme: components?.scheme,
            host: components?.host,
            path: components.flatMap { $0.percentEncodedPath.isEmpty ? nil : $0.percentEncodedPath },
            query: components?.percentEncodedQuery,
            requestBody: traffic.requestBody,
            responseBody: traffic.responseBody,
            // Milliseconds, like every other timestamp Alohomora stores.
            time: KotlinLong(longLong: Int64((traffic.startedAt.timeIntervalSince1970 * 1000).rounded())),
            duration: traffic.duration.map { KotlinLong(longLong: Int64(($0 * 1000).rounded())) },
            requestHeaders: traffic.requestHeaders,
            requestContentType: traffic.requestContentType ?? Self.contentType(in: traffic.requestHeaders),
            responseContentType: traffic.responseContentType ?? Self.contentType(in: traffic.responseHeaders),
            responseHeaders: traffic.responseHeaders,
            requestSize: Self.size(traffic.requestSize, body: traffic.requestBody),
            responseSize: Self.size(traffic.responseSize, body: traffic.responseBody),
            mockedBy: nil
        )
    }

    private static func contentType(in headers: [String: [String]]?) -> String? {
        headers?.first { $0.key.caseInsensitiveCompare("Content-Type") == .orderedSame }?.value.first
    }

    private static func size(_ explicit: Int64?, body: String?) -> KotlinLong? {
        guard let bytes = explicit ?? body.map({ Int64($0.utf8.count) }) else { return nil }
        return KotlinLong(longLong: bytes)
    }
}
