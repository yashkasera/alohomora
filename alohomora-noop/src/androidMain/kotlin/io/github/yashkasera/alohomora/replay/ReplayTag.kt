package io.github.yashkasera.alohomora.replay

/**
 * No-op mirror of `:alohomora`'s `ReplayTag`.
 *
 * Nothing reads the tag in a release build, but a custom OkHttp replay handler constructs one and has to
 * compile against both artifacts. Kept a `data class` so equality and `copy` match the debug shape.
 */
@Suppress("unused")
data class ReplayTag(val sourceTraceId: String)
