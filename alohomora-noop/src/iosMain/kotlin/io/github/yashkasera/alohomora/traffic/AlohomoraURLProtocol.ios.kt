package io.github.yashkasera.alohomora.traffic

import io.github.yashkasera.alohomora.Alohomora
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSCachedURLResponse
import platform.Foundation.NSURLProtocol
import platform.Foundation.NSURLProtocolClientProtocol
import platform.Foundation.NSURLProtocolMeta
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionDataDelegateProtocol

/**
 * No-op mirror of `:alohomora`'s `AlohomoraURLProtocol`.
 *
 * Declines every request ([canInitWithRequest] returns false) and captures nothing, so a release
 * build that installs it into a session configuration pays no interception cost. The public shape
 * matches the debug class exactly.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Suppress("unused")
class AlohomoraURLProtocol :
    NSURLProtocol,
    NSURLSessionDataDelegateProtocol {

    @OverrideInit
    constructor(
        request: NSURLRequest,
        cachedResponse: NSCachedURLResponse?,
        client: NSURLProtocolClientProtocol?,
    ) : super(request, cachedResponse, client)

    companion object : NSURLProtocolMeta() {

        override fun canInitWithRequest(request: NSURLRequest): Boolean = false

        override fun canonicalRequestForRequest(request: NSURLRequest): NSURLRequest = request

        fun register(): Boolean = false

        fun unregister() = Unit
    }

    override fun startLoading() = Unit

    override fun stopLoading() = Unit
}

// Swift-facing API on the Alohomora singleton — mirrors the debug extension functions so a release
// build resolves the same symbols. All no-ops: nothing is registered or intercepted.

/** No-op mirror of `:alohomora`'s `Alohomora.registerURLProtocol`. */
@Suppress("UnusedReceiverParameter", "unused")
fun Alohomora.registerURLProtocol(): Boolean = false

/** No-op mirror of `:alohomora`'s `Alohomora.installURLProtocol`. */
@OptIn(ExperimentalForeignApi::class)
@Suppress("UnusedReceiverParameter", "unused")
fun Alohomora.installURLProtocol(configuration: NSURLSessionConfiguration): Boolean = false

/** No-op mirror of `:alohomora`'s no-argument `Alohomora.alohomoraURLSessionConfiguration`. */
@OptIn(ExperimentalForeignApi::class)
fun Alohomora.alohomoraURLSessionConfiguration(): NSURLSessionConfiguration =
    NSURLSessionConfiguration.defaultSessionConfiguration

/** No-op mirror of `:alohomora`'s `Alohomora.alohomoraURLSessionConfiguration(base)`. */
@OptIn(ExperimentalForeignApi::class)
@Suppress("UnusedReceiverParameter")
fun Alohomora.alohomoraURLSessionConfiguration(
    base: NSURLSessionConfiguration,
): NSURLSessionConfiguration = base

/** No-op mirror of `:alohomora`'s `Alohomora.unregisterURLProtocol`. */
@Suppress("UnusedReceiverParameter", "unused")
fun Alohomora.unregisterURLProtocol() = Unit
