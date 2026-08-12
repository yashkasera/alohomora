package io.github.yashkasera.alohomora.common

/**
 * No-op mirror of `:alohomora-common`'s `FeatureFlag`.
 *
 * Duplicated rather than shared because `:alohomora-noop` deliberately does not depend on
 * `:alohomora-common`. What matters for a consumer compiling against both artifacts is the
 * fully qualified name and the shape, and those are identical.
 *
 * **Keep the constructor in lockstep with the real declaration.**
 */
data class FeatureFlag(
    val key: String,
    val value: String,
    val source: String? = null,
    val type: String? = null,
    val metadata: Map<String, String>? = null,
)
