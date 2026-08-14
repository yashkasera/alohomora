package io.github.yashkasera.alohomora.desktop

import io.github.yashkasera.alohomora.common.DatabaseSchemaSnapshot
import io.github.yashkasera.alohomora.common.DevToolsMessage
import io.github.yashkasera.alohomora.common.DevToolsProtocol
import io.github.yashkasera.alohomora.common.InitialStateMessage
import io.github.yashkasera.alohomora.common.InitialStatePayload
import io.github.yashkasera.alohomora.common.MockRule
import io.github.yashkasera.alohomora.common.SetMockRulesMessage
import io.github.yashkasera.alohomora.common.SetThrottleProfileMessage
import io.github.yashkasera.alohomora.common.ThrottleProfile
import io.github.yashkasera.alohomora.common.ThrottleProfiles
import io.github.yashkasera.alohomora.common.UnknownMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NetworkRulesProtocolTest {

    private fun roundTrip(message: DevToolsMessage): DevToolsMessage =
        assertNotNull(DevToolsProtocol.decodeFrame(DevToolsProtocol.encodeEnvelope(message)))

    private fun decodePayload(json: String): DevToolsMessage =
        assertNotNull(DevToolsProtocol.decodeEnvelope(json.encodeToByteArray()))

    @Test
    fun `SetThrottleProfileMessage round-trips`() {
        val msg = SetThrottleProfileMessage(
            sequence = 1,
            profile = ThrottleProfiles.SLOW_3G,
        )
        val decoded = assertIs<SetThrottleProfileMessage>(roundTrip(msg))
        assertEquals(ThrottleProfiles.SLOW_3G, decoded.profile)
        assertEquals(400L, decoded.profile.latencyMs)
        assertEquals(40_000L, decoded.profile.downloadBytesPerSec)
    }

    @Test
    fun `SetMockRulesMessage with multiple rules round-trips`() {
        val rules = listOf(
            MockRule(
                id = "rule-1",
                enabled = true,
                urlPattern = "/api/users",
                isRegex = false,
                method = "GET",
                statusCode = 200,
                responseBody = """{"users": []}""",
                contentType = "application/json",
            ),
            MockRule(
                id = "rule-2",
                enabled = false,
                urlPattern = ".*\\.png$",
                isRegex = true,
                method = null,
                statusCode = 404,
                responseBody = "Not Found",
                contentType = "text/plain",
            ),
        )
        val msg = SetMockRulesMessage(sequence = 2, rules = rules)
        val decoded = assertIs<SetMockRulesMessage>(roundTrip(msg))
        assertEquals(2, decoded.rules.size)
        assertEquals("rule-1", decoded.rules[0].id)
        assertEquals("GET", decoded.rules[0].method)
        assertTrue(decoded.rules[0].enabled)
        assertEquals("rule-2", decoded.rules[1].id)
        assertFalse(decoded.rules[1].enabled)
        assertTrue(decoded.rules[1].isRegex)
    }

    @Test
    fun `SetMockRulesMessage with empty list round-trips`() {
        val msg = SetMockRulesMessage(sequence = 3, rules = emptyList())
        val decoded = assertIs<SetMockRulesMessage>(roundTrip(msg))
        assertTrue(decoded.rules.isEmpty())
    }

    @Test
    fun `InitialStatePayload with networkRulesSupported true round-trips`() {
        val payload = initialState(networkRulesSupported = true)
        val msg = InitialStateMessage(sequence = 1, payload = payload)
        val decoded = assertIs<InitialStateMessage>(roundTrip(msg))
        assertTrue(decoded.payload.networkRulesSupported)
    }

    @Test
    fun `an older InitialStatePayload without networkRulesSupported defaults to false`() {
        val json = """
            {
                "type": "INITIAL_STATE",
                "sequence": 1,
                "payload": {
                    "events": [],
                    "traffic": [],
                    "databases": [],
                    "databaseSchema": { "tables": [], "schemas": [] },
                    "cacheKeys": []
                }
            }
        """.trimIndent()
        val decoded = assertIs<InitialStateMessage>(decodePayload(json))
        assertFalse(decoded.payload.networkRulesSupported)
    }

    @Test
    fun `an older device ignores SET_THROTTLE_PROFILE as UnknownMessage`() {
        val json = """{"type":"FICTIONAL_FUTURE_TYPE","sequence":1}"""
        val decoded = decodePayload(json)
        assertIs<UnknownMessage>(decoded)
    }

    @Test
    fun `custom throttle profile values round-trip`() {
        val custom =
            ThrottleProfile(name = "custom", latencyMs = 1000, downloadBytesPerSec = 10_000)
        val msg = SetThrottleProfileMessage(sequence = 1, profile = custom)
        val decoded = assertIs<SetThrottleProfileMessage>(roundTrip(msg))
        assertEquals("custom", decoded.profile.name)
        assertEquals(1000L, decoded.profile.latencyMs)
        assertEquals(10_000L, decoded.profile.downloadBytesPerSec)
    }

    private fun initialState(networkRulesSupported: Boolean = false) =
        InitialStatePayload(
            events = emptyList(),
            traffic = emptyList(),
            databaseSchema = DatabaseSchemaSnapshot(
                databaseName = null,
                tables = emptyList(),
                schemas = emptyList(),
            ),
            cacheKeys = emptyList(),
            networkRulesSupported = networkRulesSupported,
        )
}
