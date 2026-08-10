package io.github.yashkasera.alohomora.vpn

import io.github.yashkasera.alohomora.common.ThrottleProfile
import io.github.yashkasera.alohomora.common.ThrottleProfiles

/**
 * Token bucket rate limiter for VPN packet throughput.
 *
 * Fills at [bytesPerSecond], with burst capacity equal to one second's worth of tokens.
 * [consume] blocks the calling thread until enough tokens are available.
 *
 * Thread-safe: [consume] and [update] synchronize on the instance.
 *
 * DNS traffic (port 53) should bypass this entirely — throttled DNS breaks everything.
 */
internal class TokenBucketThrottle {

    private var bytesPerSecond: Long = 0
    private var latencyMs: Long = 0
    private var tokens: Long = 0
    private var lastRefill: Long = System.nanoTime()
    private var enabled: Boolean = false

    private val seenSessions = HashSet<SessionKey>()

    @Synchronized
    fun update(profile: ThrottleProfile) {
        bytesPerSecond = profile.downloadBytesPerSec
        latencyMs = profile.latencyMs
        enabled = profile.name != ThrottleProfiles.NONE.name && bytesPerSecond > 0
        if (enabled) {
            tokens = bytesPerSecond
            lastRefill = System.nanoTime()
        }
        seenSessions.clear()
    }

    /**
     * Blocks until [bytes] tokens are available, enforcing the configured rate.
     *
     * Returns immediately if throttling is disabled (profile is NONE).
     */
    @Synchronized
    fun consume(bytes: Int) {
        if (!enabled) return

        refill()

        while (tokens < bytes) {
            val deficit = bytes - tokens
            val waitNanos = deficit * 1_000_000_000L / bytesPerSecond
            val waitMs = (waitNanos / 1_000_000).coerceAtLeast(1)
            try {
                @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                (this as Object).wait(waitMs)
            } catch (_: InterruptedException) {
                return
            }
            refill()
        }

        tokens -= bytes
    }

    /**
     * Injects latency for the first packet of a new TCP/UDP session.
     *
     * Simulates connection setup delay. Not called for DNS.
     */
    fun injectLatencyIfNewSession(key: SessionKey) {
        if (!enabled || latencyMs <= 0) return
        val isNew = synchronized(this) { seenSessions.add(key) }
        if (isNew) {
            try {
                Thread.sleep(latencyMs)
            } catch (_: InterruptedException) {}
        }
    }

    private fun refill() {
        val now = System.nanoTime()
        val elapsed = now - lastRefill
        if (elapsed <= 0) return

        val newTokens = elapsed * bytesPerSecond / 1_000_000_000L
        if (newTokens > 0) {
            tokens = (tokens + newTokens).coerceAtMost(bytesPerSecond)
            lastRefill = now
        }
    }
}
