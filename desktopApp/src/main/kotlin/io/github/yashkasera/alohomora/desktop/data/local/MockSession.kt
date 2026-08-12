package io.github.yashkasera.alohomora.desktop.data.local

import io.github.yashkasera.alohomora.common.MockRule
import kotlinx.serialization.Serializable

@Serializable
data class MockSession(
    val id: String,
    val name: String,
    val rules: List<MockRule>,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class MockSessionIndex(
    val lastActiveSessionId: String? = null,
    val sessions: List<MockSessionSummary> = emptyList(),
)

@Serializable
data class MockSessionSummary(
    val id: String,
    val name: String,
    val ruleCount: Int,
    val updatedAt: Long,
)
