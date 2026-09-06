package io.github.yashkasera.alohomora.desktop.data.local

import io.github.yashkasera.alohomora.common.MockRule
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * A named set of mock rules, now a versioned config artifact ([id]/[schemaVersion]/[name]/
 * [description]) stored through `ConfigStore` under `mocks/`.
 *
 * [createdAt]/[updatedAt] are `@Transient`: git carries "when" via commit metadata, and serializing a
 * timestamp that changes on every save would churn history and break byte-stable re-saves. Runtime code
 * derives the display time from the file's modification time instead.
 */
@Serializable
data class MockSession(
    val id: String,
    val schemaVersion: Int = 1,
    val name: String,
    val description: String = "",
    val rules: List<MockRule>,
    @Transient val createdAt: Long = 0,
    @Transient val updatedAt: Long = 0,
)

@Serializable
data class MockSessionSummary(
    val id: String,
    val name: String,
    val ruleCount: Int,
    /** Derived from the file's modification time, not persisted in the artifact. */
    val updatedAt: Long,
)
