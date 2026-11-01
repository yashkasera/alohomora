package io.github.yashkasera.alohomora.traffic

import io.github.yashkasera.alohomora.replay.ReplayMarker
import platform.Foundation.NSURLProtocol
import platform.Foundation.NSURLRequest
import platform.Foundation.allHTTPHeaderFields

/**
 * How a URLSession replay tells [AlohomoraURLProtocol] which captured trace it reproduces.
 *
 * The iOS counterpart of OkHttp's `ReplayTag` and Ktor's `AlohomoraReplayOfKey`: an `NSURLProtocol`
 * property travels with the `URLRequest` into the protocol but is never serialised onto the wire, so
 * a request signed in app code before it reaches the session is not invalidated by a header the
 * protocol then strips. `urlSessionReplayHandler` in `AlohomoraKitSwift` sets it for you.
 *
 * ```swift
 * let marked = (request as NSURLRequest).mutableCopy() as! NSMutableURLRequest
 * URLProtocol.setProperty(replay.sourceTraceId, forKey: AlohomoraReplayKeys.shared.PROPERTY_KEY, in: marked)
 * ```
 *
 * Exposed here because Swift cannot see `ReplayMarker`: it lives in `:alohomora-common`, which the
 * framework does not export, so no `:alohomora` signature ever pulled it into the header. Named apart
 * from `AlohomoraReplayOfKey`, which is the Ktor attribute and a different mechanism.
 */
object AlohomoraReplayKeys {

    /** `NSURLProtocol` property key whose value is the source trace id. Never reaches the wire. */
    const val PROPERTY_KEY: String = "AlohomoraReplayOf"

    /**
     * The last-resort wire header, still honoured for handlers written before [PROPERTY_KEY]
     * existed. Prefer the property: see `ReplayMarker.HEADER` for why a header breaks signing.
     */
    const val HEADER: String = ReplayMarker.HEADER
}

/**
 * The trace [request] replays, or null for organic traffic.
 *
 * The property wins over the header so a handler that sets both — mid-migration, say — links by the
 * channel that cannot have been signed. Pulled out of `startLoading` so the precedence is testable
 * without a live network.
 */
internal fun replayOfFor(request: NSURLRequest): String? {
    val property = NSURLProtocol.propertyForKey(AlohomoraReplayKeys.PROPERTY_KEY, inRequest = request)
        as? String
    if (!property.isNullOrBlank()) return property
    return request.allHTTPHeaderFields
        ?.entries
        ?.firstOrNull { (k, _) -> (k as? String)?.equals(ReplayMarker.HEADER, true) == true }
        ?.value as? String
}
