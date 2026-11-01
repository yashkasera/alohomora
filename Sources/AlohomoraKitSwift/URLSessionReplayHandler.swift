// A ready-made replay handler for apps that talk to their backend through plain URLSession.
// OkHttp and Ktor apps get `okHttpReplayHandler` / `ktorReplayHandler` from the Kotlin side;
// without this, a URLSession app had to hand-write the header stripping and replay linking below.

import Foundation

@_exported import AlohomoraKit

/// A replay handler that re-sends a captured request through `session`.
///
/// Alohomora never sends a replay itself: the request goes back out through the app's own
/// session, so the console sees whatever the app's real networking produces. `session` **must**
/// have Alohomora's protocol installed — build it from `alohomoraURLSessionConfiguration()` —
/// because the response is captured by that protocol like any other request, and a session
/// without it would send the replay where the console can never see it. The handler refuses such
/// a session rather than reporting a replay that will never appear.
///
/// A binary body (`bodyBase64`) is sent as the exact bytes captured, under the captured content
/// type, so a multipart upload keeps the boundary its bytes were written with. The handler conforms
/// to `BinaryReplayHandler` for that reason; the return type is unchanged.
///
/// The replay is linked to its source through a `URLProtocol` property, never a header, so
/// nothing is added to the request that a signature could cover. If the endpoint is signed in app
/// code, pass `resign`: it runs on the final request just before sending, so the token or
/// signature is computed over the body being replayed rather than the one that was captured.
///
/// ```swift
/// let session = URLSession(configuration: Alohomora.shared.alohomoraURLSessionConfiguration())
/// Alohomora.shared.registerReplayHandler(handler: urlSessionReplayHandler(session: session))
///
/// // Signed endpoint: recompute the signature over what is actually being sent.
/// Alohomora.shared.registerReplayHandler(handler: urlSessionReplayHandler(session: session) { request in
///     request.setValue(try await signer.signature(for: request), forHTTPHeaderField: "X-Signature")
/// })
/// ```
///
/// - Parameters:
///   - session: the app's own session, with Alohomora's protocol installed.
///   - resign: optional hook that re-derives auth or signature headers on the outgoing request.
public func urlSessionReplayHandler(
    session: URLSession,
    resign: ((inout URLRequest) async throws -> Void)? = nil
) -> Alohomora_commonTrafficReplayHandler {
    URLSessionTrafficReplayHandler(session: session, resign: resign)
}

// BinaryReplayHandler refines Alohomora_commonTrafficReplayHandler, so conforming to it is both:
// the registry reads the refinement to decide whether a binary body may be handed over at all.
private final class URLSessionTrafficReplayHandler: NSObject, BinaryReplayHandler {
    private let session: URLSession
    private let resign: ((inout URLRequest) async throws -> Void)?

    // Decided once: URLSession copies its configuration at init, so this cannot change later.
    private let captureInstalled: Bool

    init(session: URLSession, resign: ((inout URLRequest) async throws -> Void)?) {
        self.session = session
        self.resign = resign
        self.captureInstalled = alohomoraProtocolClass.map { ours in
            session.configuration.protocolClasses?.contains { ObjectIdentifier($0) == ObjectIdentifier(ours) } ?? false
        } ?? true
    }

    func replay(request replay: Alohomora_commonReplayRequest) async throws -> Alohomora_commonReplayOutcome {
        guard captureInstalled else {
            return ReplayOutcomes.shared.failed(
                reason: "This URLSession does not have Alohomora's protocol installed, so the replay "
                    + "would not be captured. Build it from alohomoraURLSessionConfiguration()."
            )
        }
        guard let url = URL(string: replay.url) else {
            return ReplayOutcomes.shared.failed(reason: "Not a valid URL: \(replay.url)")
        }

        var request = URLRequest(url: url)
        // Trimmed and uppercased as in ktorReplayHandler: a ReplayRequest can be built by a caller
        // that never went through the console's editor.
        request.httpMethod = replay.method.trimmingCharacters(in: .whitespaces).uppercased()

        for (name, values) in replay.headers where !cfNetworkManagedHeaders.contains(name.lowercased()) {
            for value in values {
                request.addValue(value, forHTTPHeaderField: name)
            }
        }
        // Header names are case-insensitive in URLRequest, so this cannot add a second Content-Type
        // beside a captured one; it only fills in when the headers carried none.
        if let contentType = replay.contentType, request.value(forHTTPHeaderField: "Content-Type") == nil {
            request.setValue(contentType, forHTTPHeaderField: "Content-Type")
        }
        if let base64 = replay.bodyBase64 {
            // The registry has already checked it decodes; this guards a caller that bypassed it.
            guard let bytes = Data(base64Encoded: base64) else {
                return ReplayOutcomes.shared.failed(reason: "The binary request body is not valid base64.")
            }
            request.httpBody = bytes
        } else if let body = replay.body {
            request.httpBody = Data(body.utf8)
        }

        if let resign {
            do {
                try await resign(&request)
            } catch {
                return ReplayOutcomes.shared.failed(reason: "Re-signing failed: \(error.localizedDescription)")
            }
        }

        // Marked after `resign`, not before: the property never reaches the wire, so its position
        // cannot affect a signature, and applying it last survives a hook that builds a fresh
        // URLRequest instead of mutating the one it was given.
        let marked = (request as NSURLRequest).mutableCopy() as! NSMutableURLRequest
        URLProtocol.setProperty(replay.sourceTraceId, forKey: AlohomoraReplayKeys.shared.PROPERTY_KEY, in: marked)

        do {
            // The response is discarded: AlohomoraURLProtocol records it, stamped with replayOf,
            // which is how the console finds it. A non-2xx is a real answer and shows up as one —
            // only a transport failure is a replay failure.
            _ = try await session.data(for: marked as URLRequest)
            return ReplayOutcomes.shared.sent(traceId: nil)
        } catch {
            return ReplayOutcomes.shared.failed(reason: error.localizedDescription)
        }
    }
}

/// Alohomora's protocol class, found as whatever `alohomoraURLSessionConfiguration()` adds to a
/// default configuration. `AlohomoraURLProtocol` cannot be named from Swift — Kotlin subclasses of
/// ObjC classes are marked unavailable in the header — and looking it up by runtime name would
/// couple this check to the framework's `baseName`, which the Kotlin side deliberately stopped
/// doing. Nil only if nothing was added, in which case the check is skipped rather than refusing
/// every replay.
private let alohomoraProtocolClass: AnyClass? = {
    let defaults = Set((URLSessionConfiguration.default.protocolClasses ?? []).map(ObjectIdentifier.init))
    return Alohomora.shared.alohomoraURLSessionConfiguration().protocolClasses?.first {
        !defaults.contains(ObjectIdentifier($0))
    }
}()

/// Headers CFNetwork owns and sets from the request actually being sent, so a captured value can
/// only contradict it. Forwarding a captured `Content-Length` that no longer matches an edited
/// body, or a `Content-Encoding` claiming a plain-text body is compressed, makes the task fail with
/// `NSURLErrorDataLengthExceedsMaximum` (-1103) before anything reaches the server. Lower-cased
/// for comparison; header names are case-insensitive.
private let cfNetworkManagedHeaders: Set<String> = [
    "accept-encoding",
    "content-encoding",
    "content-length",
    "host",
    "connection",
    "proxy-connection",
    "transfer-encoding",
]
