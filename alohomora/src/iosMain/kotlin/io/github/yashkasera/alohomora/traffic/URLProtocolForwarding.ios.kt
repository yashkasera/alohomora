package io.github.yashkasera.alohomora.traffic

import io.github.yashkasera.alohomora.common.CapturedBody
import io.github.yashkasera.alohomora.common.CapturedRequestBody
import io.github.yashkasera.alohomora.common.TrafficEntry
import kotlinx.atomicfu.locks.ReentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSURLSessionAuthChallengeCancelAuthenticationChallenge
import platform.Foundation.NSURLSessionAuthChallengeDisposition
import platform.Foundation.NSURLSessionAuthChallengePerformDefaultHandling
import platform.Foundation.NSURLSessionAuthChallengeRejectProtectionSpace
import platform.Foundation.NSURLSessionAuthChallengeUseCredential
import platform.posix.memcpy

// The parts of AlohomoraURLProtocol's forwarding that do not need a live loading system, pulled out
// so they can be unit tested. Everything that does (challenge round trips, redirects, run loop
// delivery) is exercised only on a device.

/**
 * Decides which `NSURLProtocolClient` callbacks may still be delivered.
 *
 * The loading system forbids any client callback after `stopLoading`, and expects exactly one
 * terminal callback (finish, fail, or a redirect followed by its cancellation). The forwarding
 * task keeps reporting after both: cancelling it produces a `didCompleteWithError(-999)`, and a
 * throttled response lands whenever its timer fires. Checked on the client thread at the moment of
 * delivery, which is also where `stopLoading` runs, so a stop always wins over a callback that was
 * merely queued before it.
 */
internal class ClientCallbackGate {
    private val lock = ReentrantLock()
    private var stopped = false
    private var finished = false

    /** True while non-terminal callbacks (response, data, challenge) may be delivered. */
    val isOpen: Boolean get() = lock.withLock { !stopped && !finished }

    /** True once `stopLoading` has run, whether or not the terminal callback was delivered first. */
    val isStopped: Boolean get() = lock.withLock { stopped }

    /** Called from `stopLoading`. Permanent. */
    fun stop() = lock.withLock { stopped = true }

    /** Claims the one terminal callback. True exactly once, and never after [stop]. */
    fun claimTerminal(): Boolean = lock.withLock {
        if (stopped || finished) return@withLock false
        finished = true
        true
    }
}

/**
 * Authentication challenges forwarded to the client and not yet answered, each with the forwarding
 * session's completion handler that the answer must reach.
 *
 * A handler that is never called stalls the forwarding task, and with it the host app's request,
 * forever. So [drain] exists for `stopLoading`, and [take] falls back to the oldest pending entry
 * when the challenge it is handed is not one we created: a task never has two challenges outstanding
 * (the loader waits for a disposition before it continues), and answering the only pending one is
 * better than hanging if the loading system ever hands back a copy instead of our object.
 */
internal class PendingChallenges<C : Any, H : Any> {
    private val lock = ReentrantLock()
    private val pending = mutableListOf<Pair<C, H>>()

    fun add(challenge: C, handler: H) = lock.withLock { pending += challenge to handler }

    fun take(challenge: C): H? = lock.withLock {
        val index = pending.indexOfFirst { it.first == challenge }.takeIf { it >= 0 }
            ?: if (pending.isEmpty()) return@withLock null else 0
        pending.removeAt(index).second
    }

    fun drain(): List<H> = lock.withLock { pending.map { it.second }.also { pending.clear() } }
}

/** How the client answered a forwarded challenge, one per `NSURLAuthenticationChallengeSender` method. */
internal enum class ChallengeReply {
    USE_CREDENTIAL,
    CONTINUE_WITHOUT_CREDENTIAL,
    CANCEL,
    PERFORM_DEFAULT_HANDLING,
    REJECT_PROTECTION_SPACE,
}

/**
 * The URLSession disposition that reproduces [reply] on the forwarding session.
 *
 * `continueWithoutCredential` has no disposition of its own: it is `UseCredential` with a nil
 * credential, which is how the loading system itself translates it. Mapping it to default handling
 * instead would silently re-apply system trust evaluation the app had chosen to skip.
 */
internal fun ChallengeReply.disposition(): NSURLSessionAuthChallengeDisposition = when (this) {
    ChallengeReply.USE_CREDENTIAL,
    ChallengeReply.CONTINUE_WITHOUT_CREDENTIAL,
    -> NSURLSessionAuthChallengeUseCredential

    ChallengeReply.CANCEL -> NSURLSessionAuthChallengeCancelAuthenticationChallenge
    ChallengeReply.PERFORM_DEFAULT_HANDLING -> NSURLSessionAuthChallengePerformDefaultHandling
    ChallengeReply.REJECT_PROTECTION_SPACE -> NSURLSessionAuthChallengeRejectProtectionSpace
}

/**
 * The response body a capture stores, built from the first `maxBytes + 1` bytes only.
 *
 * The client still receives every byte; this is the display copy. The extra byte makes overflow
 * observed rather than inferred, as on the OkHttp path: Content-Length is absent for chunked
 * responses and describes the compressed size for gzipped ones, which URLSession has already
 * inflated by the time the protocol sees the data.
 */
internal class CappedBodyCapture(private val maxBytes: Int) {
    private val chunks = mutableListOf<ByteArray>()
    private var retained = 0
    private var placeholder: String? = null

    /** Every byte the client received, retained or not. */
    var totalBytes: Long = 0
        private set

    /**
     * Stops retaining and records [text] instead. For bodies that must not be decoded at all:
     * known-binary types, and event streams, which would otherwise be held until they end.
     */
    fun skip(text: String) {
        placeholder = text
        chunks.clear()
        retained = 0
    }

    /** How many leading bytes of the next chunk are still worth copying. */
    fun bytesWanted(): Int = if (placeholder != null) 0 else (maxBytes + 1 - retained).coerceAtLeast(0)

    /** Records a chunk of [chunkLength] bytes whose first bytes are [prefix] (at most [bytesWanted]). */
    fun append(prefix: ByteArray, chunkLength: Long) {
        totalBytes += chunkLength
        val keep = minOf(prefix.size, bytesWanted())
        if (keep == 0) return
        chunks += if (keep == prefix.size) prefix else prefix.copyOf(keep)
        retained += keep
    }

    /** The stored body and whether it is cut short. A body that is not text gets the shared placeholder. */
    fun result(): CapturedResponseBody {
        placeholder?.let { return CapturedResponseBody(it, truncated = false) }
        val bytes = ByteArray(retained)
        var offset = 0
        for (chunk in chunks) {
            chunk.copyInto(bytes, offset)
            offset += chunk.size
        }
        val text = CapturedBody.decodeText(bytes, maxBytes)
            ?: return CapturedResponseBody(TrafficEntry.UNABLE_PARSE_MESSAGE, truncated = false)
        return CapturedResponseBody(text, truncated = totalBytes > maxBytes)
    }

    companion object {
        /** The placeholder for a response body that must not be read, or null to capture it. */
        fun placeholderFor(contentType: String?): String? = when {
            CapturedBody.isEventStream(contentType) -> CapturedBody.STREAMED_PLACEHOLDER
            CapturedBody.isKnownBinary(contentType) -> TrafficEntry.UNABLE_PARSE_MESSAGE
            else -> null
        }
    }
}

internal data class CapturedResponseBody(val text: String, val truncated: Boolean)

/**
 * The request body a capture stores for [data], copied out of it one prefix only.
 *
 * At most `maxBytes + 1` bytes leave the NSData: a larger body is stored cut short (text) or as
 * the placeholder (binary) either way, and the extra byte is what lets the decode see the cut.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun capturedRequestBody(data: NSData, maxBytes: Int, contentType: String?): CapturedRequestBody {
    val totalBytes = data.length.toLong()
    val count = minOf(totalBytes, maxBytes + 1L).toInt()
    val prefix = ByteArray(count)
    // addressOf(0) is out of bounds on an empty array, and an empty body has nothing to copy.
    if (count > 0) prefix.usePinned { memcpy(it.addressOf(0), data.bytes, count.toULong()) }
    return CapturedBody.requestBody(prefix, totalBytes, maxBytes, contentType)
}

internal data class TrafficOutcome(val status: Int?, val message: String?)

/**
 * The status and message a finished load is stored with.
 *
 * Status is an HTTP status or nothing. A transport failure used to be stored as its NSError code,
 * so every cancelled SwiftUI `.task` appeared as a request answered with status -999, and timeouts
 * and offline failures as -1001 and -1009, which the consoles colour and filter as if the server had
 * sent them. A failure after a response arrived (the connection dropping mid-body) keeps the
 * server's status, but the message reports the failure, since that is what the app saw.
 */
internal fun trafficOutcome(
    httpStatusCode: Int?,
    httpStatusText: String?,
    transportError: String?,
): TrafficOutcome = TrafficOutcome(status = httpStatusCode, message = transportError ?: httpStatusText)
