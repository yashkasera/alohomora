package io.github.yashkasera.alohomora.traffic

import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.AlohomoraInternal
import io.github.yashkasera.alohomora.common.HeaderRedaction
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.replay.ReplayMarker
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCClass
import platform.Foundation.HTTPBody
import platform.Foundation.HTTPMethod
import platform.Foundation.NSCachedURLResponse
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSHTTPURLResponse
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
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.allHTTPHeaderFields
import platform.Foundation.appendData
import platform.Foundation.create
import platform.Foundation.setAllHTTPHeaderFields

private const val HANDLED_KEY = "AlohomoraHandled"

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
@OptIn(ExperimentalForeignApi::class)
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

    @OptIn(ExperimentalUuidApi::class)
    override fun startLoading() {
        traceId = Uuid.random().toString()
        startTime = Clock.System.now().toEpochMilliseconds()
        // Read off allHTTPHeaderFields rather than valueForHTTPHeaderField, which cinterop does not
        // expose on NSURLRequest. Matched case-insensitively, as HTTP header names are.
        replayOf = request.allHTTPHeaderFields
            ?.entries
            ?.firstOrNull { (k, _) -> (k as? String)?.equals(ReplayMarker.HEADER, true) == true }
            ?.value as? String

        // Tag the request so canInitWithRequest returns false for this copy, preventing
        // infinite recursion when the internal session issues the same request.
        val tagged = (request.mutableCopy() as NSMutableURLRequest).also {
            NSURLProtocol.setProperty("1", forKey = HANDLED_KEY, inRequest = it)
            // Alohomora injected this on the way in; it must not reach the server. Rewriting the
            // whole dictionary rather than clearing the single field: cinterop's
            // setValue:forHTTPHeaderField: overload collides with NSObject.setValue(_:forKey:).
            if (replayOf != null) {
                it.setAllHTTPHeaderFields(
                    request.allHTTPHeaderFields?.filterKeys { key ->
                        (key as? String)?.equals(ReplayMarker.HEADER, true) != true
                    },
                )
            }
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

    // region NSURLSessionDataDelegate

    override fun URLSession(
        session: NSURLSession,
        dataTask: NSURLSessionDataTask,
        didReceiveResponse: NSURLResponse,
        completionHandler: (Long) -> Unit,
    ) {
        httpResponse = didReceiveResponse as? NSHTTPURLResponse
        client?.URLProtocol(
            this,
            didReceiveResponse = didReceiveResponse,
            cacheStoragePolicy = NSURLCacheStoragePolicy.NSURLCacheStorageNotAllowed,
        )
        completionHandler(NSURLSessionResponseAllow)
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

        AlohomoraInternal.recordTraffic(
            TrafficEntry(
                id = traceId,
                url = url?.absoluteString,
                method = req.HTTPMethod,
                scheme = url?.scheme,
                host = url?.host,
                path = url?.path,
                query = url?.query,
                requestBody = req.HTTPBody?.let { body ->
                    NSString.create(data = body, encoding = NSUTF8StringEncoding)?.toString()
                        ?: TrafficEntry.UNABLE_PARSE_MESSAGE
                },
                requestHeaders = requestHeaders,
                requestContentType = requestHeaders["Content-Type"]?.firstOrNull(),
                requestSize = req.HTTPBody?.length?.toLong(),
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
@OptIn(ExperimentalForeignApi::class)
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
fun Alohomora.unregisterURLProtocol() {
    NSURLProtocol.unregisterClass(alohomoraProtocolClass)
}
