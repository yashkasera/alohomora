package io.github.yashkasera.alohomora.common.deeplink

import kotlinx.serialization.Serializable

/**
 * A version-controlled deep link, defined as a **template with typed parameters** rather than a
 * concrete URL. A concrete URL is an instance of a definition. This turns the store into a reviewed
 * contract that drives docs, a validated builder, example checks, and agent discovery.
 *
 * Carries the shared config fields ([id]/[name]/[description]/[schemaVersion]); [module] is both a
 * grouping axis and the directory level in the repo layout (`deeplinks/{module}/{name}.json`).
 */
@Serializable
data class DeepLinkDef(
    val id: String,
    val name: String,
    val module: String,
    val flow: String? = null,
    val description: String = "",
    val schemaVersion: Int = 1,
    /** e.g. `fampay://kyc/verify/{userId}?step={step}` — placeholders are `{name}`. */
    val uriTemplate: String,
    val params: List<DeepLinkParam> = emptyList(),
    /** Known-good concrete URLs, validated against [uriTemplate] + [params] on save and in tests. */
    val examples: List<String> = emptyList(),
)

@Serializable
data class DeepLinkParam(
    val name: String,
    val type: ParamType,
    val required: Boolean = true,
    /** For [ParamType.ENUM]. */
    val allowedValues: List<String> = emptyList(),
    val description: String = "",
    val example: String? = null,
)

enum class ParamType { STRING, INT, BOOL, ENUM, UUID }
