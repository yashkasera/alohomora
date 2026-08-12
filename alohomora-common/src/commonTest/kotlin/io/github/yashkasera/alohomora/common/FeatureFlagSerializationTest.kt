package io.github.yashkasera.alohomora.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FeatureFlagSerializationTest {

    private fun roundTrip(message: DevToolsMessage): DevToolsMessage =
        assertNotNull(DevToolsProtocol.decodeFrame(DevToolsProtocol.encodeEnvelope(message)))

    @Test
    fun `FeatureFlag with all fields round-trips`() {
        val flag = FeatureFlag(
            key = "checkout_v2",
            value = "true",
            source = "LaunchDarkly",
            type = "experiment",
            metadata = mapOf("cohort" to "B", "rollout" to "50"),
        )
        val decoded = assertIs<FeatureFlagsSnapshotMessage>(
            roundTrip(FeatureFlagsSnapshotMessage(1, listOf(flag))),
        )
        assertEquals(flag, decoded.flags.first())
    }

    @Test
    fun `FeatureFlag with only required fields round-trips`() {
        val flag = FeatureFlag(key = "min_flag", value = "off")
        val decoded = assertIs<FeatureFlagsSnapshotMessage>(
            roundTrip(FeatureFlagsSnapshotMessage(1, listOf(flag))),
        )
        val result = decoded.flags.first()
        assertEquals("min_flag", result.key)
        assertEquals("off", result.value)
        assertNull(result.source)
        assertNull(result.type)
        assertNull(result.metadata)
    }

    @Test
    fun `InitialStatePayload defaults featureFlags to empty when absent`() {
        val json = """
            {
              "type": "INITIAL_STATE",
              "sequence": 1,
              "payload": {
                "events": [],
                "traffic": [],
                "databaseSchema": { "databaseName": null, "tables": [], "schemas": [] },
                "cacheKeys": []
              }
            }
        """.trimIndent()
        val decoded = assertIs<InitialStateMessage>(
            assertNotNull(DevToolsProtocol.decodeEnvelope(json.encodeToByteArray())),
        )
        assertEquals(emptyList(), decoded.payload.featureFlags)
    }

    @Test
    fun `empty snapshot round-trips`() {
        val decoded = assertIs<FeatureFlagsSnapshotMessage>(
            roundTrip(FeatureFlagsSnapshotMessage(1, emptyList())),
        )
        assertEquals(emptyList(), decoded.flags)
    }
}
