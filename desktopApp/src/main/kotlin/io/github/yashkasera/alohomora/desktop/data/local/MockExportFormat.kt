package io.github.yashkasera.alohomora.desktop.data.local

import io.github.yashkasera.alohomora.common.MockRule
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class MockExportEnvelope(
    val version: Int = 1,
    val name: String,
    val exportedAt: Long,
    val rules: List<MockRule>,
)

internal val exportJson = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
    ignoreUnknownKeys = true
    encodeDefaults = true
}

fun MockSession.toExportEnvelope(): MockExportEnvelope = MockExportEnvelope(
    name = name,
    exportedAt = System.currentTimeMillis(),
    rules = rules,
)

fun MockExportEnvelope.toSession(): MockSession {
    val now = System.currentTimeMillis()
    return MockSession(
        id = kotlin.uuid.Uuid.random().toString(),
        name = name,
        rules = rules,
        createdAt = now,
        updatedAt = now,
    )
}
