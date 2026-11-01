package io.github.yashkasera.alohomora.traffic

import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.AlohomoraImpl
import io.github.yashkasera.alohomora.common.HeaderRedaction
import io.github.yashkasera.alohomora.common.MockRule
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.devtools.NetworkRuleEngine
import io.github.yashkasera.alohomora.replay.ReplayMarker
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCClass
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.Foundation.HTTPBody
import platform.Foundation.HTTPBodyStream
import platform.Foundation.HTTPMethod
import platform.Foundation.NSCachedURLResponse
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSInputStream
import platform.Foundation.NSMutableData
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSString
import platform.Foundation.NSStringFromClass
import platform.Foundation.NSURLCacheStoragePolicy
import platform.Foundation.NSURLProtocol
import platform.Foundation.NSURLProtocolClientProtocol
import platform.Foundation.NSURLProtocolMeta
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURLResponse
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionDataDelegateProtocol
import platform.Foundation.NSURLSessionDataTask
import platform.Foundation.NSURLSessionResponseAllow
import platform.Foundation.NSURLSessionTask
import platform.Foundation.NSURLErrorDomain
import platform.Foundation.NSURLErrorRequestBodyStreamExhausted
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.allHTTPHeaderFields
import platform.Foundation.appendData
import platform.Foundation.create
import platform.Foundation.setAllHTTPHeaderFields
import platform.Foundation.setHTTPBody
import platform.Foundation.valueForHTTPHeaderField
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_after
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time

private const val HANDLED_KEY = "AlohomoraHandled"

/**
 * Upper bound on a captured request body, in bytes, matching the OkHttp interceptor's
 * `MAX_BODY_BYTES` and the Ktor plugin's `MAX_BODY_CHARS`. Only the stored copy is bounded —
 * the full body is always forwarded to the server (see [AlohomoraURLProtocol.startLoading]).
 */
private const val MAX_BODY_BYTES = 1_000_000L

/**
 * NSURLProtocol interceptor that captures HTTP/HTTPS traffic and forwards trace data to the
 * Alohomora debug tool.
 *
 * ## `URLSession.shared` cannot be intercepted
 *
 * `NSURLProtocol.registerClass` affects only the legacy `NSURLConnection` loading system.
 * **URLSession ignores globally registered protocol classes**, and `URLSession.shared` can never
 * be intercepted at all because its configuration is immutable. Interception requires this class
 * to appear in a session's own `URLSessionConfiguration.protocolClasses`.
 *
 * So build your session from [alohomoraURLSessionConfiguration] and use it in place of
 * `URLSession.shared`:
 * ```swift
 * // Swift — create once, then reuse for every request you want traced.
 * let session = URLSession(configuration: Alohomora.shared.alohomoraURLSessionConfiguration())
 * let (data, _) = try await session.data(from: url)
 * ```
 *
 * Or inject the class into a configuration you already build yourself:
 * ```swift
 * let config = URLSessionConfiguration.default
 * Alohomora.shared.installURLProtocol(configuration: config)
 * ```
 *
 * Limitations: does not intercept URLSession background upload/download tasks or WebSocket frames.
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

        override fun canInitWithRequest(request: NSURLRequest): Boolean {
            // Prevent recursive interception of the internal forwarding session.
            if (propertyForKey(HANDLED_KEY, inRequest = request) != null) return false
            val scheme = request.URL?.scheme
            return scheme == "http" || scheme == "https"
        }

        override fun canonicalRequestForRequest(request: NSURLRequest): NSURLRequest = request

        /**
         * Registers with the legacy `NSURLConnection` loading system.
         *
         * Does **not** make URLSession traffic traceable — see the class docs. Use
         * `Alohomora.installURLProtocol(configuration:)` for that.
         */
        fun register(): Boolean = NSURLProtocol.registerClass(this)

        /** Removes the legacy registration. */
        fun unregister() {
            NSURLProtocol.unregisterClass(this)
        }
    }

    private var session: NSURLSession? = null
    private var dataTask: NSURLSessionDataTask? = null
    private val accumulator = NSMutableData()
    private var httpResponse: NSHTTPURLResponse? = null
    private var startTime = 0L
    private var traceId = ""
    private var replayOf: String? = null

    // Captured at startLoading, read at persist time. The request body must be drained from the
    // body stream before the forwarding task consumes it (a stream is one-shot), so it cannot be
    // re-read from `request` in persistTrace the way HTTPBody could be.
    private var capturedRequestBody: String? = null
    private var capturedRequestSize: Long? = null
    private var capturedRequestBodyTruncated = false

    @OptIn(ExperimentalUuidApi::class, BetaInteropApi::class)
    override fun startLoading() {
        traceId = Uuid.random().toString()
        startTime = Clock.System.now().toEpochMilliseconds()
        // Out-of-band property first, wire header as the fallback — see AlohomoraReplayKeys.
        replayOf = replayOfFor(request)

        // Drain the request body up front. NSURLProtocol delivers a POST/PUT body set as a stream
        // (URLSession/Ktor-Darwin uploads) with HTTPBody == nil, so reading HTTPBody alone captured
        // nothing and left signed POST replays sending an empty body. A body stream is one-shot, so
        // the drained bytes are re-attached to the forwarded request below — otherwise the real
        // request would go out empty. GET and other bodyless requests leave this null.
        val existingBody = request.HTTPBody
        val bodyStream = if (existingBody == null) request.HTTPBodyStream else null
        val declaredLength = request.valueForHTTPHeaderField("Content-Length")?.toLongOrNull()
        var drained: NSData? = null
        when {
            existingBody != null -> captureRequestBody(existingBody)
            bodyStream == null -> Unit

            // Not drained. A body over the cap is stored truncated, which replay refuses anyway, so
            // buffering it would cost a full in-memory copy of the upload for a display placeholder.
            // The untouched stream is forwarded as-is. Only a declared length can decide this up
            // front; a chunked upload of unknown size is still drained in full.
            declaredLength != null && declaredLength > MAX_BODY_BYTES -> {
                capturedRequestSize = declaredLength
                capturedRequestBody = "Streamed body of $declaredLength bytes, not captured: over the capture limit."
                capturedRequestBodyTruncated = true
            }

            else -> {
                drained = bodyStream.drain()
                if (drained == null) {
                    // A partial body must never be forwarded, nor recorded as complete. Fail the load
                    // the way URLSession itself does when a body stream breaks.
                    val error = bodyStream.streamError ?: NSError.errorWithDomain(
                        domain = NSURLErrorDomain,
                        code = NSURLErrorRequestBodyStreamExhausted,
                        userInfo = null,
                    )
                    client?.URLProtocol(this, didFailWithError = error)
                    persistTrace(error)
                    return
                }
                captureRequestBody(drained)
            }
        }

        val mockRule = NetworkRuleEngine.findMatch(
            request.URL?.absoluteString ?: "",
            request.HTTPMethod ?: "GET",
        )
        if (mockRule != null) {
            deliverMockResponse(mockRule)
            return
        }

        // Tag the request so canInitWithRequest returns false for this copy, preventing
        // infinite recursion when the internal session issues the same request.
        val tagged = (request.mutableCopy() as NSMutableURLRequest).also {
            NSURLProtocol.setProperty("1", forKey = HANDLED_KEY, inRequest = it)
            if (replayOf != null) {
                it.setAllHTTPHeaderFields(
                    request.allHTTPHeaderFields?.filterKeys { key ->
                        (key as? String)?.equals(ReplayMarker.HEADER, true) != true
                    },
                )
            }
            // Re-attach the body we drained from the one-shot stream. Setting HTTPBody clears
            // HTTPBodyStream, which is exactly right: the stream is already spent. The byte count
            // is unchanged, so any Content-Length the app set still matches.
            drained?.let(it::setHTTPBody)
        }

        session = NSURLSession.sessionWithConfiguration(
            configuration = NSURLSessionConfiguration.defaultSessionConfiguration(),
            delegate = this,
            delegateQueue = null,
        )
        dataTask = session!!.dataTaskWithRequest(tagged)
        dataTask!!.resume()
    }

    override fun stopLoading() {
        dataTask?.cancel()
        session?.invalidateAndCancel()
    }

    /**
     * Records [data] as the captured request body, mirroring the OkHttp and Ktor paths.
     *
     * Decodes as UTF-8; a body that is not valid UTF-8 (protobuf, binary multipart parts) is stored
     * as [TrafficEntry.UNABLE_PARSE_MESSAGE] rather than corrupted, so [replayBlockedReason] refuses
     * the replay instead of putting mojibake on the wire. The stored string is capped at
     * [MAX_BODY_BYTES]; the full body still reaches the server, only this display copy is bounded.
     */
    @OptIn(BetaInteropApi::class)
    private fun captureRequestBody(data: NSData?) {
        if (data == null) return
        capturedRequestSize = data.length.toLong()
        val text = NSString.create(data = data, encoding = NSUTF8StringEncoding)?.toString()
        capturedRequestBody = when {
            text == null -> TrafficEntry.UNABLE_PARSE_MESSAGE
            text.length.toLong() > MAX_BODY_BYTES -> {
                capturedRequestBodyTruncated = true
                text.take(MAX_BODY_BYTES.toInt()) + "\n…truncated (${text.length} chars total)"
            }

            else -> text
        }
    }

    /**
     * Reads this one-shot input stream to the end and returns the bytes, or null if it fails.
     *
     * Reads the whole stream, not a bounded prefix: the bytes are re-attached to the forwarded
     * request, so a truncated read would send a corrupt request. The capture-side cap is applied
     * later, to the stored string only ([captureRequestBody]). A read error returns null rather
     * than what was read so far: treating -1 like end-of-stream forwarded a partial body and
     * recorded it as complete.
     */
    @OptIn(ExperimentalForeignApi::class)
    private fun NSInputStream.drain(): NSData? {
        val out = NSMutableData()
        val bufferSize = 64 * 1024
        val buffer = ByteArray(bufferSize)
        open()
        try {
            buffer.usePinned { pinned ->
                while (true) {
                    val bytesRead = read(pinned.addressOf(0).reinterpret(), bufferSize.toULong())
                    if (bytesRead == 0L) break
                    if (bytesRead < 0L) return null
                    out.appendData(
                        NSData.create(bytes = pinned.addressOf(0), length = bytesRead.toULong())
                            ?: return null,
                    )
                }
            }
        } finally {
            close()
        }
        return out
    }

    // region NSURLSessionDataDelegate

    override fun URLSession(
        session: NSURLSession,
        dataTask: NSURLSessionDataTask,
        didReceiveResponse: NSURLResponse,
        completionHandler: (Long) -> Unit,
    ) {
        httpResponse = didReceiveResponse as? NSHTTPURLResponse
        val throttle = NetworkRuleEngine.throttle
        if (throttle.latencyMs > 0) {
            val delta = throttle.latencyMs * 1_000_000L
            dispatch_after(
                dispatch_time(DISPATCH_TIME_NOW, delta),
                dispatch_get_main_queue(),
            ) {
                this.client?.URLProtocol(
                    this,
                    didReceiveResponse = didReceiveResponse,
                    cacheStoragePolicy = NSURLCacheStoragePolicy.NSURLCacheStorageNotAllowed,
                )
                completionHandler(NSURLSessionResponseAllow)
            }
        } else {
            client?.URLProtocol(
                this,
                didReceiveResponse = didReceiveResponse,
                cacheStoragePolicy = NSURLCacheStoragePolicy.NSURLCacheStorageNotAllowed,
            )
            completionHandler(NSURLSessionResponseAllow)
        }
    }

    override fun URLSession(
        session: NSURLSession,
        dataTask: NSURLSessionDataTask,
        didReceiveData: NSData,
    ) {
        accumulator.appendData(didReceiveData)
        client?.URLProtocol(this, didLoadData = didReceiveData)
    }

    override fun URLSession(
        session: NSURLSession,
        task: NSURLSessionTask,
        didCompleteWithError: NSError?,
    ) {
        if (didCompleteWithError != null) {
            client?.URLProtocol(this, didFailWithError = didCompleteWithError)
        } else {
            client?.URLProtocolDidFinishLoading(this)
        }
        session.finishTasksAndInvalidate()
        persistTrace(error = didCompleteWithError)
    }

    // endregion

    @OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
    private fun deliverMockResponse(mockRule: MockRule) {
        val url = request.URL ?: return
        val bodyBytes = mockRule.responseBody.encodeToByteArray()
        val bodyData = bodyBytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bodyBytes.size.toULong())
        }

        // K/N 2.4 exposes initWithURL:statusCode:HTTPVersion:headerFields: but with
        // platform-specific parameter names. Using the overload found in the generated stubs.
        @Suppress("DEPRECATION")
        val response = NSHTTPURLResponse(
            url, mockRule.statusCode.toLong(), "HTTP/1.1",
            mapOf("Content-Type" to mockRule.contentType),
        )

        client?.URLProtocol(
            this,
            didReceiveResponse = response,
            cacheStoragePolicy = NSURLCacheStoragePolicy.NSURLCacheStorageNotAllowed,
        )
        client?.URLProtocol(this, didLoadData = bodyData)
        client?.URLProtocolDidFinishLoading(this)

        val req = request
        val requestHeaders: Map<String, List<String>> = HeaderRedaction.redact(
            req.allHTTPHeaderFields
                ?.entries
                ?.mapNotNull { (k, v) -> (k as? String)?.to(listOf(v as? String ?: "")) }
                ?.filterNot { it.first.equals(ReplayMarker.HEADER, ignoreCase = true) }
                ?.toMap()
                ?: emptyMap(),
        ).orEmpty()

        AlohomoraImpl.persistTrafficEntry(
            TrafficEntry(
                id = traceId,
                url = url.absoluteString,
                method = req.HTTPMethod,
                scheme = url.scheme,
                host = url.host,
                path = url.path,
                query = url.query,
                requestBody = capturedRequestBody,
                requestBodyTruncated = capturedRequestBodyTruncated,
                requestHeaders = requestHeaders,
                requestContentType = requestHeaders["Content-Type"]?.firstOrNull(),
                requestSize = capturedRequestSize,
                time = startTime,
                status = mockRule.statusCode,
                message = "Mocked",
                responseBody = mockRule.responseBody,
                responseContentType = mockRule.contentType,
                responseSize = mockRule.responseBody.length.toLong(),
                duration = 0,
                replayOf = replayOf,
                mockedBy = mockRule.id,
            ),
        )
    }

    @OptIn(BetaInteropApi::class)
    private fun persistTrace(error: NSError?) {
        val req = request
        val url = req.URL
        val endTime = Clock.System.now().toEpochMilliseconds()

        // Redacted here, as on the OkHttp and Ktor paths. This capture site was missed when
        // header redaction was introduced, so iOS traces carried bearer tokens and cookies
        // verbatim into SQLite, over the wire, and into shared curl commands.
        val requestHeaders: Map<String, List<String>> = HeaderRedaction.redact(
            req.allHTTPHeaderFields
                ?.entries
                ?.mapNotNull { (k, v) -> (k as? String)?.to(listOf(v as? String ?: "")) }
                ?.filterNot { it.first.equals(ReplayMarker.HEADER, ignoreCase = true) }
                ?.toMap()
                ?: emptyMap(),
        ).orEmpty()

        val responseHeaders: Map<String, List<String>> = HeaderRedaction.redact(
            (httpResponse?.allHeaderFields ?: emptyMap<Any?, Any?>())
                .entries
                .mapNotNull { (k, v) -> (k as? String)?.to(listOf(v as? String ?: "")) }
                .toMap(),
        ).orEmpty()

        AlohomoraImpl.persistTrafficEntry(
            TrafficEntry(
                id = traceId,
                url = url?.absoluteString,
                method = req.HTTPMethod,
                scheme = url?.scheme,
                host = url?.host,
                path = url?.path,
                query = url?.query,
                requestBody = capturedRequestBody,
                requestBodyTruncated = capturedRequestBodyTruncated,
                requestHeaders = requestHeaders,
                requestContentType = requestHeaders["Content-Type"]?.firstOrNull(),
                requestSize = capturedRequestSize,
                time = startTime,
                status = httpResponse?.statusCode?.toInt() ?: error?.code?.toInt(),
                message = httpResponse?.let {
                    NSHTTPURLResponse.localizedStringForStatusCode(it.statusCode)
                } ?: error?.localizedDescription,
                responseBody = NSString.create(
                    data = accumulator,
                    encoding = NSUTF8StringEncoding,
                )?.toString(),
                responseHeaders = responseHeaders,
                responseContentType = responseHeaders["Content-Type"]?.firstOrNull(),
                responseSize = accumulator.length.toLong(),
                duration = endTime - startTime,
                replayOf = replayOf,
            ),
        )
    }
}

/**
 * The Objective-C class object for [AlohomoraURLProtocol].
 *
 * Taken from the companion, which extends `NSURLProtocolMeta` and therefore *is* the ObjC class.
 * Previously this was `NSClassFromString("AlohomoraKitAlohomoraURLProtocol")`, which coupled trace
 * capture to the framework's `baseName`: renaming the framework would have made every install
 * silently return false and capture nothing, with no compile error. It also resolved to null
 * outside the framework (e.g. in unit tests), making the behaviour untestable.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private val alohomoraProtocolClass: ObjCClass get() = AlohomoraURLProtocol

// Swift-facing API on the Alohomora singleton, since companion methods on a class
// whose companion extends an ObjC metaclass are not bridged as Swift type methods.

/**
 * Registers [AlohomoraURLProtocol] with the legacy `NSURLConnection` loading system.
 *
 * **This does not make URLSession traffic traceable.** URLSession ignores globally registered
 * protocol classes, so on its own this captures nothing from a modern app. Kept because it costs
 * nothing and still covers `NSURLConnection`, but every URLSession-based app needs
 * [alohomoraURLSessionConfiguration] or [installURLProtocol] as well.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Suppress("UnusedReceiverParameter", "unused")
fun Alohomora.registerURLProtocol(): Boolean =
    NSURLProtocol.registerClass(alohomoraProtocolClass)

/**
 * Inserts [AlohomoraURLProtocol] at the front of [configuration]'s protocol classes.
 *
 * This is the only supported way to trace URLSession traffic. Call it before constructing the
 * session — `URLSessionConfiguration` is copied by `URLSession(configuration:)`, so mutating the
 * configuration afterwards has no effect.
 *
 * @return true if the class was found and installed.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Suppress("UnusedReceiverParameter")
fun Alohomora.installURLProtocol(configuration: NSURLSessionConfiguration): Boolean {
    val cls = alohomoraProtocolClass
    val existing = configuration.protocolClasses.orEmpty()
    // Compared by ObjC class name, not by identity. Kotlin/Native hands back a *fresh*
    // ObjCClass wrapper on each access, so `it === cls` is never true and an identity check
    // silently re-prepends the class on every call — growing protocolClasses without bound.
    val name = NSStringFromClass(cls)
    val alreadyInstalled = existing.any {
        @Suppress("UNCHECKED_CAST")
        (it as? ObjCClass)?.let { existingClass -> NSStringFromClass(existingClass) == name } == true
    }
    // Prepended, not appended: URLSession asks each class in order whether it can handle a
    // request, and the first to say yes wins. Appending would let the built-in HTTP protocol
    // claim every request first and we would never see one.
    if (!alreadyInstalled) {
        configuration.protocolClasses = listOf(cls) + existing
    }
    return true
}

/**
 * A default URLSession configuration with Alohomora tracing installed.
 *
 * Use the resulting session **instead of `URLSession.shared`**, which cannot be traced because its
 * configuration is immutable:
 * ```swift
 * let session = URLSession(configuration: Alohomora.shared.alohomoraURLSessionConfiguration())
 * ```
 *
 * Declared as an explicit no-argument overload rather than a default parameter: Kotlin default
 * arguments are not bridged as Swift defaults, so `= NSURLSessionConfiguration.default` would
 * force every Swift caller to pass one.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
fun Alohomora.alohomoraURLSessionConfiguration(): NSURLSessionConfiguration =
    alohomoraURLSessionConfiguration(NSURLSessionConfiguration.defaultSessionConfiguration)

/**
 * Installs Alohomora tracing into [base] and returns it.
 *
 * Use this overload when you configure timeouts, default headers or caching policy yourself.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
fun Alohomora.alohomoraURLSessionConfiguration(
    base: NSURLSessionConfiguration,
): NSURLSessionConfiguration = base.also { installURLProtocol(it) }

/** Unregister the global [AlohomoraURLProtocol] interceptor. */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Suppress("UnusedReceiverParameter", "unused")
fun Alohomora.unregisterURLProtocol() {
    NSURLProtocol.unregisterClass(alohomoraProtocolClass)
}
