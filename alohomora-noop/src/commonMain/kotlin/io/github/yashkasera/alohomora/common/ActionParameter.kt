package io.github.yashkasera.alohomora.common

/**
 * No-op mirror of `:alohomora-common`'s `ActionParameter`.
 *
 * **Keep the constructor in lockstep with the real declaration.**
 */
@Suppress("unused")
data class ActionParameter(
    val key: String,
    val label: String,
    val type: String = "string",
    val defaultValue: String? = null,
    val options: List<String>? = null,
    val required: Boolean = true,
)
