package io.github.yashkasera.alohomora.common

import kotlin.concurrent.Volatile
import kotlin.time.TimeSource

/**
 * Pacing for the DevTools liveness probe.
 *
 * Part of the wire contract, not a local tuning knob: the device promises to send a
 * [PingMessage] every [PING_INTERVAL_MILLIS], and both peers size their silence window off
 * that promise. Changing either constant changes what the other side is entitled to assume.
 */
object DevToolsHeartbeat {
    /**
     * How often the device pings an otherwise idle client.
     *
     * Cheap enough to ignore — a PING frame is a few dozen bytes over loopback plus a USB
     * transport that is already carrying captured response bodies.
     */
    const val PING_INTERVAL_MILLIS: Long = 5_000

    /**
     * How long a peer may send nothing at all before it is presumed gone.
     *
     * Four ping intervals. The point of the margin is that a single dropped or delayed frame
     * must not end a healthy session, while a genuinely dead peer still frees the device's
     * connection slot in well under a minute rather than never.
     */
    const val SILENCE_TIMEOUT_MILLIS: Long = PING_INTERVAL_MILLIS * 4
}

/**
 * Tracks when a DevTools peer last proved it was alive.
 *
 * Exists because a read timeout cannot answer the question this does. After authentication the
 * desktop legitimately sends nothing for long stretches while it only receives streams, so a
 * bare socket read timeout kills healthy idle sessions — one was tried on the device and
 * reverted for exactly that reason. TCP keepalive is no help either: on an `adb forward` the
 * device's socket is to the on-device adb daemon over loopback, and that socket stays
 * genuinely, verifiably healthy after the desktop process at the far end of the USB transport
 * is gone. Only an end-to-end round trip through the whole path distinguishes "idle but alive"
 * from "dead peer", which is what the ping provokes and this records.
 *
 * Deliberately counts *any* inbound frame, not just pongs. A session streaming traffic is
 * proving its liveness continuously; the ping only exists to manufacture a frame when nothing
 * else is happening.
 *
 * Safe to share across coroutines: [recordSignOfLife] is a single `Long` store and
 * [isPeerSilent] a single load, so the worst interleaving loses one refresh and shortens the
 * window by an interval — never long enough to condemn a peer that is answering.
 *
 * @param elapsedMillis monotonic milliseconds since this object was created. Injectable so the
 *   policy can be tested without waiting out real time.
 */
class DevToolsLiveness(
    private val silenceTimeoutMillis: Long = DevToolsHeartbeat.SILENCE_TIMEOUT_MILLIS,
    private val elapsedMillis: () -> Long = monotonicElapsedMillis(),
) {
    @Volatile
    private var lastSignOfLifeMillis: Long = elapsedMillis()

    /** Records that a frame arrived from the peer. */
    fun recordSignOfLife() {
        lastSignOfLifeMillis = elapsedMillis()
    }

    fun silentForMillis(): Long = elapsedMillis() - lastSignOfLifeMillis

    /**
     * Whether the peer has been silent long enough to presume it gone.
     *
     * Callers must gate on the peer actually supporting the heartbeat before consulting this —
     * a peer that predates PING/PONG answers nothing and would be condemned on its first
     * idle stretch.
     */
    fun isPeerSilent(): Boolean = silentForMillis() > silenceTimeoutMillis
}

/**
 * A monotonic elapsed-millis reader.
 *
 * Monotonic rather than wall clock so a clock adjustment — NTP, or a developer's laptop waking
 * from sleep — cannot make a live peer look silent for hours.
 */
private fun monotonicElapsedMillis(): () -> Long {
    val start = TimeSource.Monotonic.markNow()
    return { start.elapsedNow().inWholeMilliseconds }
}
