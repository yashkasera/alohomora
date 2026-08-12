package io.github.yashkasera.alohomora.desktop.data.local

import io.github.yashkasera.alohomora.common.MockRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MockExportFormatTest {

    private val sampleRules = listOf(
        MockRule(
            id = "r1",
            enabled = true,
            urlPattern = "/api/users",
            method = "GET",
            statusCode = 200,
            responseBody = """{"name": "{{name}}"}""",
            contentType = "application/json",
        ),
        MockRule(
            id = "r2",
            enabled = false,
            urlPattern = "/api/orders",
            isRegex = true,
            method = "POST",
            statusCode = 201,
            responseBody = """{"id": "{{uuid}}"}""",
        ),
    )

    @Test
    fun roundTripSerialization() {
        val envelope = MockExportEnvelope(
            name = "Test session",
            exportedAt = 1000L,
            rules = sampleRules,
        )
        val json = exportJson.encodeToString(MockExportEnvelope.serializer(), envelope)
        val decoded = exportJson.decodeFromString(MockExportEnvelope.serializer(), json)
        assertEquals(envelope.name, decoded.name)
        assertEquals(envelope.rules.size, decoded.rules.size)
        assertEquals(envelope.rules[0].urlPattern, decoded.rules[0].urlPattern)
        assertEquals(envelope.rules[0].responseBody, decoded.rules[0].responseBody)
        assertEquals(envelope.rules[1].isRegex, decoded.rules[1].isRegex)
    }

    @Test
    fun versionDefaultsToOne() {
        val envelope = MockExportEnvelope(name = "x", exportedAt = 0, rules = emptyList())
        assertEquals(1, envelope.version)
    }

    @Test
    fun templatePlaceholdersPreservedLiterally() {
        val envelope = MockExportEnvelope(
            name = "Templates",
            exportedAt = 0,
            rules = listOf(
                MockRule(
                    id = "t1",
                    urlPattern = "/test",
                    responseBody = """{"id": "{{uuid}}", "amount": {{amount(10,500)}}}""",
                ),
            ),
        )
        val json = exportJson.encodeToString(MockExportEnvelope.serializer(), envelope)
        val decoded = exportJson.decodeFromString(MockExportEnvelope.serializer(), json)
        assertTrue(decoded.rules[0].responseBody.contains("{{uuid}}"))
        assertTrue(decoded.rules[0].responseBody.contains("{{amount(10,500)}}"))
    }

    @Test
    fun toSessionCreatesNewId() {
        val envelope = MockExportEnvelope(name = "Imported", exportedAt = 0, rules = sampleRules)
        val session = envelope.toSession()
        assertTrue(session.id.isNotBlank())
        assertEquals("Imported", session.name)
        assertEquals(2, session.rules.size)
    }

    @Test
    fun toExportEnvelopeUsesSessionName() {
        val session = MockSession(
            id = "s1",
            name = "My session",
            rules = sampleRules,
            createdAt = 100,
            updatedAt = 200,
        )
        val envelope = session.toExportEnvelope()
        assertEquals("My session", envelope.name)
        assertEquals(2, envelope.rules.size)
    }
}
