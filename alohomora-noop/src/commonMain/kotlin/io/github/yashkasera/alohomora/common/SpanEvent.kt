package io.github.yashkasera.alohomora.common

/**
 * No-op mirror of `:alohomora-common`'s `SpanEvent`.
 *
 * Duplicated rather than shared because `:alohomora-noop` deliberately does not depend on
 * `:alohomora-common` — the same reason `ReplayRequest` and `CustomScreenPlugin` are duplicated here.
 * What matters for a consumer compiling one `Alohomora.recordSpan(...)` call site against both
 * artifacts is the fully qualified name and the shape, and those are identical.
 *
 * The real declaration is `@Serializable` because it crosses the DevTools wire. Nothing here is ever
 * serialised, so the annotation — and the serialization plugin it would require in this module — is
 * omitted. **Keep the constructor in lockstep with the real declaration.**
 */
data class SpanEvent(
    val name: String,
    /** Epoch nanoseconds, matching the real declaration's unit. */
    val epochNanos: Long,
    val attributes: Map<String, String>? = null,
)
