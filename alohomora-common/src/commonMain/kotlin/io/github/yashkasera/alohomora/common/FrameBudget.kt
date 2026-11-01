package io.github.yashkasera.alohomora.common

/**
 * Keeps device-to-desktop frames under [DevToolsProtocol.MAX_PAYLOAD_BYTES].
 *
 * The reader enforces that cap, so a frame over it is not slow, it is rejected outright, and the
 * desktop used to reconnect and receive the same oversized snapshot forever. Bodies are the only
 * part of a frame that can grow without bound in practice, so they are what gets shed.
 */
object FrameBudget {

    /**
     * Characters of request + response body an INITIAL_STATE may carry across all its traffic.
     *
     * Half the frame cap. The rest is headroom for JSON escaping and for the other snapshots in the
     * same frame: 100 errors with full stack traces can alone run to a couple of MB.
     */
    const val SNAPSHOT_BODY_BUDGET_CHARS: Long = DevToolsProtocol.MAX_PAYLOAD_BYTES / 2L

    /** Stands in for a body that was left out of a frame. The entry on the device is untouched. */
    const val BODY_OMITTED_MESSAGE: String =
        "Body omitted from this connection's snapshot to stay within the frame size limit."

    /**
     * Keeps bodies on the first entries in [traffic] until [budgetChars] is spent, and strips them
     * from every entry after that. Pass entries newest first, as the snapshot query returns them,
     * so the bodies a developer is most likely to open are the ones kept.
     */
    fun fitBodiesToBudget(traffic: List<TrafficEntry>, budgetChars: Long): List<TrafficEntry> {
        var spent = 0L
        var exhausted = false
        return traffic.map { entry ->
            if (exhausted) return@map entry.withoutBodies()
            val size = entry.bodyChars()
            if (spent + size > budgetChars) {
                exhausted = true
                entry.withoutBodies()
            } else {
                spent += size
                entry
            }
        }
    }

    /**
     * [message] with every traffic body stripped, or null when it carries no traffic to shed.
     * The last resort before a frame is dropped.
     */
    fun withoutBodies(message: DevToolsMessage): DevToolsMessage? = when (message) {
        is InitialStateMessage -> message.copy(
            payload = message.payload.copy(traffic = message.payload.traffic.map { it.withoutBodies() }),
        )

        is StreamTrafficMessage -> message.copy(traffic = message.traffic.withoutBodies())
        else -> null
    }

    // The base64 counts in full: a 1 MB binary upload is 1.33 MB of it, and it is the field that
    // carries the bytes, so leaving it out would let a snapshot of uploads overrun the frame.
    private fun TrafficEntry.bodyChars(): Long =
        (requestBody?.length ?: 0).toLong() +
            (requestBodyBase64?.length ?: 0).toLong() +
            (responseBody?.length ?: 0).toLong()

    /**
     * This entry with both bodies replaced by [BODY_OMITTED_MESSAGE] and flagged truncated.
     *
     * Flagged rather than nulled: a null request body reads as "this request had no body", and
     * replay would then send an empty one. A truncated body is refused by `replayBlockedReason`.
     * A binary body's base64 goes with it and is flagged the same way, so its replay is refused
     * instead of falling back to the placeholder text.
     */
    private fun TrafficEntry.withoutBodies(): TrafficEntry {
        val shedRequest = !requestBody.isNullOrEmpty() || !requestBodyBase64.isNullOrEmpty()
        val shedResponse = !responseBody.isNullOrEmpty()
        return copy(
            requestBody = if (shedRequest) BODY_OMITTED_MESSAGE else requestBody,
            requestBodyBase64 = if (shedRequest) null else requestBodyBase64,
            requestBodyTruncated = requestBodyTruncated || shedRequest,
            responseBody = if (shedResponse) BODY_OMITTED_MESSAGE else responseBody,
            responseBodyTruncated = responseBodyTruncated || shedResponse,
        )
    }
}
