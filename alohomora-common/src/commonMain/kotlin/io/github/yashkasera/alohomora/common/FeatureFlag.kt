package io.github.yashkasera.alohomora.common

import kotlinx.serialization.Serializable

@Serializable
data class FeatureFlag(
    val key: String,
    val value: String,
    val source: String? = null,
    val type: String? = null,
    val metadata: Map<String, String>? = null,
)
