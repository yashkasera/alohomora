package io.github.yashkasera.alohomora.traffic

import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.AlohomoraImpl
import io.github.yashkasera.alohomora.common.CapturedRequestBody
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
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFRunLoopGetCurrent
import platform.CoreFoundation.CFRunLoopPerformBlock
import platform.CoreFoundation.CFRunLoopWakeUp
import platform.Foundation.CFBridgingRetain
import platform.Foundation.HTTPBody
import platform.Foundation.HTTPBodyStream
import platform.Foundation.HTTPMethod
import platform.Foundation.NSCachedURLResponse
import platform.Foundation.NSCocoaErrorDomain
import platform.Foundation.NSData
import platform.Foundation.NSDefaultRunLoopMode
import platform.Foundation.NSError
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSInputStream
import platform.Foundation.NSMutableData
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSRunLoop
import platform.Foundation.NSStringFromClass
import platform.Foundation.NSURL
import platform.Foundation.NSURLAuthenticationChallenge
import platform.Foundation.NSURLAuthenticationChallengeSenderProtocol
import platform.Foundation.NSURLCacheStoragePolicy
import platform.Foundation.NSURLCredential
import platform.Foundation.NSURLProtocol
import platform.Foundation.NSURLProtocolClientProtocol
import platform.Foundation.NSURLProtocolMeta
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURLResponse
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionAuthChallengeDisposition
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionDataDelegateProtocol
import platform.Foundation.NSURLSessionDataTask
import platform.Foundation.NSURLSessionResponseAllow
import platform.Foundation.NSURLSessionTask
import platform.Foundation.NSURLErrorDomain
import platform.Foundation.NSURLErrorRequestBodyStreamExhausted
import platform.Foundation.NSUserCancelledError
import platform.Foundation.allHTTPHeaderFields
import platform.Foundation.appendData
import platform.Foundation.create
import platform.Foundation.setAllHTTPHeaderFields
import platform.Foundation.setHTTPBody
import platform.Foundation.valueForHTTPHeaderField
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.NSObject
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_after
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_time
import platform.posix.memcpy

private const val HANDLED_KEY = "AlohomoraHandled"

/**
 * Upper bound on a captured request or response body, in bytes, matching the OkHttp interceptor's
 * `MAX_BODY_BYTES` and the Ktor plugin's `MAX_BODY_CHARS`. Only the stored copy is bounded: the full
 * request reaches the server (see [AlohomoraURLProtocol.startLoading]) and the full response reaches
 * the app.
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
 *
 * ## Forwarding must be invisible
 *
 * Governing rule, as on the OkHttp and Ktor paths: **instrumentation must never change the outcome
 * of the host app's request.** The request is re-issued on a private forwarding session, so anything
 * that session would otherwise decide on its own is handed back to the app's session instead:
 * authentication challenges (SSL pinning, self-signed staging certificates, client certificates)
 * and redirects. Client callbacks are delivered on the thread `startLoading` ran on, as the loading
 * system requires, and never after `stopLoading`.
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
    private var httpResponse: NSHTTPURLResponse? = null
    private var startTime = 0L
    private var traceId = ""
    private var replayOf: String? = null

    // Only the first MAX_BODY_BYTES + 1 bytes are kept for capture. The whole response used to be
    // buffered and decoded to a String, so a large download was held twice over just for display.
    private val responseCapture = CappedBodyCapture(MAX_BODY_BYTES.toInt())

    // The run loop of the thread startLoading ran on, and the modes it was running in. Every
    // client callback has to be delivered there. Held as the NSRunLoop, not a bare CFRunLoopRef,
    // because the Kotlin reference keeps the run loop retained for as long as a block may target it.
    private var clientRunLoop: NSRunLoop? = null
    private var clientModes: List<String> = listOfNotNull(NSDefaultRunLoopMode)

    private val gate = ClientCallbackGate()
    private val challengeSender = ForwardedChallengeSender()

    // One trace per load. A redirect records its 3xx leg itself, and the cancellation that follows
    // must not record it a second time as a failure. Read and written only on the forwarding
    // session's serial delegate queue, or in startLoading before that session exists.
    private var persisted = false

    // Captured at startLoading, read at persist time. The request body must be drained from the
    // body stream before the forwarding task consumes it (a stream is one-shot), so it cannot be
    // re-read from `request` in persistTrace the way HTTPBody could be.
    private var capturedRequestBody: String? = null
    private var capturedRequestSize: Long? = null
    private var capturedRequestBodyTruncated = false
    private var capturedRequestBodyBase64: String? = null

    @OptIn(ExperimentalUuidApi::class, BetaInteropApi::class)
    override fun startLoading() {
        traceId = Uuid.random().toString()
        startTime = Clock.System.now().toEpochMilliseconds()
        captureClientThread()
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
                    onClientThread {
                        if (gate.claimTerminal()) client?.URLProtocol(this, didFailWithError = error)
                    }
                    persistTrace(error)
                    return
                }
                captureRequestBody(drained)
            }
        }

        // Every Kotlin step on this path runs under an Objective-C caller, where an uncaught
        // exception terminates the app rather than failing one request. A lookup that throws is
        // treated as no match, so the request goes to the network as it would without Alohomora.
        val mockRule = runCatching {
            NetworkRuleEngine.findMatch(request.URL?.absoluteString ?: "", request.HTTPMethod ?: "GET")
        }.getOrNull()
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
        // Before the cancel: the cancelled task still reports didCompleteWithError(-999), and the
        // loading system must hear nothing more from this instance once it has stopped it.
        gate.stop()
        dataTask?.cancel()
        session?.invalidateAndCancel()
        challengeSender.cancelAll()
    }

    /**
     * Records where client callbacks must go. Called first thing in [startLoading], on the client
     * thread. The loader may run that thread's run loop in a private mode, so the current mode is
     * kept alongside the default one; a block queued for the default mode alone could wait there
     * until the load times out.
     */
    private fun captureClientThread() {
        val runLoop = NSRunLoop.currentRunLoop
        clientRunLoop = runLoop
        clientModes = listOfNotNull(NSDefaultRunLoopMode, runLoop.currentMode).distinct()
    }

    /**
     * Runs [block] on the client thread: inline when already there (startLoading's synchronous
     * paths), otherwise queued on its run loop.
     *
     * The forwarding session calls its delegate on its own queue, and the throttle timer fires on a
     * global queue; Apple requires every client callback on the thread startLoading ran on.
     * `CFRunLoopPerformBlock` is used rather than `performSelector:onThread:`, which in Kotlin/Native
     * would need an `@ObjCAction` trampoline plus a hand-rolled queue of pending callbacks. CF run
     * loop functions are thread-safe, blocks run in the order queued, and `CFRunLoopWakeUp` is
     * needed because a sleeping run loop does not notice a newly queued block on its own.
     */
    private fun onClientThread(block: () -> Unit) {
        val runLoop = clientRunLoop?.getCFRunLoop() ?: return
        if (CFRunLoopGetCurrent() == runLoop) {
            block()
            return
        }
        // CFRunLoopPerformBlock copies the modes into a set of its own, so this reference is ours
        // to release straight away.
        val modes = CFBridgingRetain(clientModes)
        CFRunLoopPerformBlock(runLoop, modes, block)
        CFRelease(modes)
        CFRunLoopWakeUp(runLoop)
    }

    /**
     * Records [data] as the captured request body, mirroring the OkHttp and Ktor paths.
     *
     * Decoded strictly as UTF-8; a body that is not text (protobuf, binary multipart parts) keeps
     * its bytes as base64 when it is within [MAX_BODY_BYTES], so replay can send it back unchanged,
     * and the placeholder alone when it is not. Text is capped at [MAX_BODY_BYTES]; the full body
     * still reaches the server, only this copy is bounded.
     */
    private fun captureRequestBody(data: NSData?) {
        if (data == null) return
        capturedRequestSize = data.length.toLong()
        val captured = runCatching {
            capturedRequestBody(
                data = data,
                maxBytes = MAX_BODY_BYTES.toInt(),
                contentType = request.valueForHTTPHeaderField("Content-Type"),
            )
        }.getOrDefault(CapturedRequestBody(TrafficEntry.UNABLE_PARSE_MESSAGE))
        capturedRequestBody = captured.text
        capturedRequestBodyTruncated = captured.truncated
        capturedRequestBodyBase64 = captured.base64
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
        // Decided once, before any data arrives: an event stream would otherwise be retained until it
        // ends, and a binary body only ever decodes to the placeholder anyway.
        runCatching {
            CappedBodyCapture.placeholderFor(didReceiveResponse.MIMEType)?.let(responseCapture::skip)
        }

        // The client hears about the response before the task is allowed to continue, so its data
        // callbacks, queued after this one on the same run loop, cannot overtake it.
        val deliver = {
            onClientThread {
                if (gate.isOpen) {
                    client?.URLProtocol(
                        this,
                        didReceiveResponse = didReceiveResponse,
                        cacheStoragePolicy = NSURLCacheStoragePolicy.NSURLCacheStorageNotAllowed,
                    )
                }
            }
            completionHandler(NSURLSessionResponseAllow)
        }
        val latencyMs = runCatching { NetworkRuleEngine.throttle.latencyMs }.getOrDefault(0L)
        if (latencyMs > 0) {
            // A global queue, not main: the delay must not occupy the app's main thread, and the
            // client callback is routed to the client thread by onClientThread either way.
            dispatch_after(
                dispatch_time(DISPATCH_TIME_NOW, latencyMs * 1_000_000L),
                dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u),
                deliver,
            )
        } else {
            deliver()
        }
    }

    override fun URLSession(
        session: NSURLSession,
        dataTask: NSURLSessionDataTask,
        didReceiveData: NSData,
    ) {
        runCatching { retainForCapture(didReceiveData) }
        onClientThread {
            if (gate.isOpen) client?.URLProtocol(this, didLoadData = didReceiveData)
        }
    }

    override fun URLSession(
        session: NSURLSession,
        task: NSURLSessionTask,
        didCompleteWithError: NSError?,
    ) {
        onClientThread {
            if (gate.claimTerminal()) {
                if (didCompleteWithError != null) {
                    client?.URLProtocol(this, didFailWithError = didCompleteWithError)
                } else {
                    client?.URLProtocolDidFinishLoading(this)
                }
            }
        }
        session.finishTasksAndInvalidate()
        persistTrace(error = didCompleteWithError)
    }

    /**
     * Reports a redirect to the client instead of following it here.
     *
     * Followed silently, the app's own `willPerformHTTPRedirection` never ran, so a redirect policy
     * (refusing cross-host hops, stripping a header before one) was bypassed whenever Alohomora was
     * installed. This is Apple's CustomHTTPProtocol pattern: hand the client the new request with
     * our recursion flag removed, so the load the system issues for it re-enters this protocol and
     * is captured on its own; then cancel this task and fail with `NSUserCancelledError`, which the
     * loading system treats as this instance stepping aside rather than as a failed request.
     */
    override fun URLSession(
        session: NSURLSession,
        task: NSURLSessionTask,
        willPerformHTTPRedirection: NSHTTPURLResponse,
        newRequest: NSURLRequest,
        completionHandler: (NSURLRequest?) -> Unit,
    ) {
        val redirect = (newRequest.mutableCopy() as NSMutableURLRequest).also {
            NSURLProtocol.removePropertyForKey(HANDLED_KEY, inRequest = it)
        }
        onClientThread {
            if (gate.claimTerminal()) {
                client?.URLProtocol(this, wasRedirectedToRequest = redirect, redirectResponse = willPerformHTTPRedirection)
                // The loader may stop this instance from inside the call above, once it has started
                // the new load; after that it must not hear from us again.
                if (!gate.isStopped) {
                    client?.URLProtocol(
                        this,
                        didFailWithError = NSError.errorWithDomain(NSCocoaErrorDomain, NSUserCancelledError, null),
                    )
                }
            }
        }
        task.cancel()
        // Answered so the forwarding session is not left waiting; with the task already cancelled,
        // declining the redirect only lets it finish.
        completionHandler(null)

        // The 3xx leg is a trace of its own, as it would be in any proxy. The cancellation reported
        // next by didCompleteWithError is ours, not the app's, and is not recorded.
        httpResponse = willPerformHTTPRedirection
        persistTrace(error = null)
    }

    /**
     * Hands an authentication challenge to the client, so the host app's session delegate decides.
     *
     * Without this the forwarding session applied system default handling to every challenge: SSL
     * pinning stopped rejecting anything, and self-signed staging certificates and client
     * certificates stopped working, but only in builds with Alohomora installed. The challenge is
     * rewrapped with [ForwardedChallengeSender] as its sender; whichever sender method the client calls completes
     * the forwarding session's handler with the matching disposition. An app with no challenge
     * delegate is answered with default handling by the loading system, which is what happened before.
     *
     * Only the task-level method is implemented: URLSession routes session-level challenges (server
     * trust, client certificates) to it when the session-level one is absent.
     */
    override fun URLSession(
        session: NSURLSession,
        task: NSURLSessionTask,
        didReceiveChallenge: NSURLAuthenticationChallenge,
        completionHandler: (NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Unit,
    ) {
        val forwarded = challengeSender.forward(didReceiveChallenge, completionHandler)
        onClientThread {
            val client = client
            when {
                !gate.isOpen -> challengeSender.answer(forwarded, ChallengeReply.CANCEL)
                client == null -> challengeSender.answer(forwarded, ChallengeReply.PERFORM_DEFAULT_HANDLING)
                else -> client.URLProtocol(this, didReceiveAuthenticationChallenge = forwarded)
            }
        }
    }

    // endregion

    /** Copies only the leading bytes the capture still wants; the rest of the chunk is never copied. */
    private fun retainForCapture(data: NSData) {
        val length = data.length.toLong()
        val wanted = minOf(responseCapture.bytesWanted().toLong(), length).toInt()
        val prefix = ByteArray(wanted)
        val source = data.bytes
        if (wanted > 0 && source != null) {
            prefix.usePinned { memcpy(it.addressOf(0), source, wanted.toULong()) }
        }
        responseCapture.append(if (source == null) ByteArray(0) else prefix, length)
    }

    @OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
    private fun deliverMockResponse(mockRule: MockRule) {
        val url = request.URL ?: return
        val bodyBytes = mockRule.responseBody.encodeToByteArray()
        // addressOf(0) is out of bounds on an empty array, so a mock with no body (a 204, say)
        // threw here, under an Objective-C caller.
        val bodyData = if (bodyBytes.isEmpty()) {
            NSData()
        } else {
            bodyBytes.usePinned { pinned ->
                NSData.create(bytes = pinned.addressOf(0), length = bodyBytes.size.toULong())
            }
        }

        // K/N 2.4 exposes initWithURL:statusCode:HTTPVersion:headerFields: but with
        // platform-specific parameter names. Using the overload found in the generated stubs.
        @Suppress("DEPRECATION")
        val response = NSHTTPURLResponse(
            url, mockRule.statusCode.toLong(), "HTTP/1.1",
            mapOf("Content-Type" to mockRule.contentType),
        )

        // Inline: this runs inside startLoading, on the client thread, before any stop is possible.
        onClientThread {
            if (gate.claimTerminal()) {
                client?.URLProtocol(
                    this,
                    didReceiveResponse = response,
                    cacheStoragePolicy = NSURLCacheStoragePolicy.NSURLCacheStorageNotAllowed,
                )
                client?.URLProtocol(this, didLoadData = bodyData)
                client?.URLProtocolDidFinishLoading(this)
            }
        }

        runCatching { recordMock(url, mockRule) }
    }

    /** Records a delivered mock. Best-effort: the response has already reached the client. */
    private fun recordMock(url: NSURL, mockRule: MockRule) {
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
                requestBodyBase64 = capturedRequestBodyBase64,
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

    /**
     * Records the finished load. Called from the session delegate and from [startLoading], both
     * under an Objective-C caller, after the client has already been told the outcome — so a
     * capture failure is dropped here instead of terminating the app.
     */
    private fun persistTrace(error: NSError?) {
        if (persisted) return
        persisted = true
        runCatching { buildAndPersistTrace(error) }
    }

    @OptIn(BetaInteropApi::class)
    private fun buildAndPersistTrace(error: NSError?) {
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

        val outcome = trafficOutcome(
            httpStatusCode = httpResponse?.statusCode?.toInt(),
            httpStatusText = httpResponse?.let { NSHTTPURLResponse.localizedStringForStatusCode(it.statusCode) },
            transportError = error?.localizedDescription,
        )
        val responseBody = responseCapture.result()

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
                requestBodyBase64 = capturedRequestBodyBase64,
                requestHeaders = requestHeaders,
                requestContentType = requestHeaders["Content-Type"]?.firstOrNull(),
                requestSize = capturedRequestSize,
                time = startTime,
                status = outcome.status,
                message = outcome.message,
                // No body at all when no response arrived, rather than an empty one.
                responseBody = responseBody.text.takeIf { httpResponse != null },
                responseBodyTruncated = responseBody.truncated,
                responseHeaders = responseHeaders,
                responseContentType = responseHeaders["Content-Type"]?.firstOrNull(),
                responseSize = responseCapture.totalBytes,
                duration = endTime - startTime,
                replayOf = replayOf,
            ),
        )
    }
}

/**
 * The sender of every challenge [AlohomoraURLProtocol] forwards to its client: the client answers by
 * calling one of these methods, and the answer completes the forwarding session's handler.
 *
 * A separate internal object rather than the protocol itself, so the sender methods and the
 * `NSURLAuthenticationChallengeSender` conformance stay out of the public class's API.
 */
@OptIn(ExperimentalForeignApi::class)
internal class ForwardedChallengeSender : NSObject(), NSURLAuthenticationChallengeSenderProtocol {

    private val pending =
        PendingChallenges<NSURLAuthenticationChallenge, (NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Unit>()

    /** Wraps [challenge] with this object as its sender and holds [completionHandler] until it is answered. */
    fun forward(
        challenge: NSURLAuthenticationChallenge,
        completionHandler: (NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Unit,
    ): NSURLAuthenticationChallenge =
        NSURLAuthenticationChallenge(authenticationChallenge = challenge, sender = this).also {
            pending.add(it, completionHandler)
        }

    fun answer(challenge: NSURLAuthenticationChallenge, reply: ChallengeReply, credential: NSURLCredential? = null) {
        pending.take(challenge)?.invoke(reply.disposition(), credential)
    }

    /** For `stopLoading`: an unanswered challenge holds the forwarding task open, and the client will never answer it now. */
    fun cancelAll() {
        pending.drain().forEach { it(ChallengeReply.CANCEL.disposition(), null) }
    }

    override fun useCredential(credential: NSURLCredential, forAuthenticationChallenge: NSURLAuthenticationChallenge) {
        answer(forAuthenticationChallenge, ChallengeReply.USE_CREDENTIAL, credential)
    }

    override fun continueWithoutCredentialForAuthenticationChallenge(challenge: NSURLAuthenticationChallenge) {
        answer(challenge, ChallengeReply.CONTINUE_WITHOUT_CREDENTIAL)
    }

    override fun cancelAuthenticationChallenge(challenge: NSURLAuthenticationChallenge) {
        answer(challenge, ChallengeReply.CANCEL)
    }

    override fun performDefaultHandlingForAuthenticationChallenge(challenge: NSURLAuthenticationChallenge) {
        answer(challenge, ChallengeReply.PERFORM_DEFAULT_HANDLING)
    }

    override fun rejectProtectionSpaceAndContinueWithChallenge(challenge: NSURLAuthenticationChallenge) {
        answer(challenge, ChallengeReply.REJECT_PROTECTION_SPACE)
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
