package io.github.yashkasera.alohomora.desktop

import io.github.yashkasera.alohomora.common.DevToolsMessage
import io.github.yashkasera.alohomora.common.DevToolsProtocol
import io.github.yashkasera.alohomora.common.FeatureFlag
import io.github.yashkasera.alohomora.common.FeatureFlagsSnapshotMessage
import io.github.yashkasera.alohomora.common.InitialStateMessage
import io.github.yashkasera.alohomora.common.InitialStatePayload
import io.github.yashkasera.alohomora.common.DatabaseSchemaSnapshot
import io.github.yashkasera.alohomora.desktop.data.local.FeatureFlagStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class FeatureFlagsProtocolTest {

    private fun roundTrip(message: DevToolsMessage): DevToolsMessage =
        assertNotNull(DevToolsProtocol.decodeFrame(DevToolsProtocol.encodeEnvelope(message)))

    private fun decodePayload(json: String): DevToolsMessage =
        assertNotNull(DevToolsProtocol.decodeEnvelope(json.encodeToByteArray()))

    private val sample = FeatureFlag(
        key = "dark_mode_v2",
        value = "true",
        source = "Firebase Remote Config",
        type = "feature_flag",
        metadata = mapOf("variant" to "treatment"),
    )

    @Test
    fun `a feature flags snapshot survives the wire intact`() {
        val decoded = assertIs<FeatureFlagsSnapshotMessage>(
            roundTrip(FeatureFlagsSnapshotMessage(1, listOf(sample))),
        )
        assertEquals(1, decoded.flags.size)
        assertEquals(sample, decoded.flags.first())
    }

    @Test
    fun `feature flags in the initial snapshot survive the wire intact`() {
        val message = InitialStateMessage(
            sequence = 1,
            payload = initialState(featureFlags = listOf(sample)),
        )
        val decoded = assertIs<InitialStateMessage>(roundTrip(message))
        assertEquals(listOf(sample), decoded.payload.featureFlags)
    }

    @Test
    fun `a snapshot from an older app decodes with no feature flags`() {
        val withoutFlags = """
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

        val decoded = assertIs<InitialStateMessage>(decodePayload(withoutFlags))
        assertEquals(emptyList(), decoded.payload.featureFlags)
    }

    @Test
    fun `the desktop store replaces flags and sorts by key`() {
        val store = FeatureFlagStore()
        val flags = listOf(
            sample.copy(key = "z_flag"),
            sample.copy(key = "a_flag"),
            sample.copy(key = "m_flag"),
        )
        store.replace(flags)

        assertEquals(listOf("a_flag", "m_flag", "z_flag"), store.flags.value.map { it.key })
    }

    @Test
    fun `clear empties the store`() {
        val store = FeatureFlagStore()
        store.replace(listOf(sample))
        assertEquals(1, store.flags.value.size)

        store.clear()
        assertEquals(emptyList(), store.flags.value)
    }

    @Test
    fun `a flag with null optional fields round-trips`() {
        val minimal = FeatureFlag(key = "simple", value = "42")
        val decoded = assertIs<FeatureFlagsSnapshotMessage>(
            roundTrip(FeatureFlagsSnapshotMessage(1, listOf(minimal))),
        )
        val flag = decoded.flags.first()
        assertEquals("simple", flag.key)
        assertEquals("42", flag.value)
        assertEquals(null, flag.source)
        assertEquals(null, flag.type)
        assertEquals(null, flag.metadata)
    }

    private fun initialState(featureFlags: List<FeatureFlag>) =
        InitialStatePayload(
            events = emptyList(),
            traffic = emptyList(),
            databaseSchema = DatabaseSchemaSnapshot(
                databaseName = null,
                tables = emptyList(),
                schemas = emptyList(),
            ),
            cacheKeys = emptyList(),
            featureFlags = featureFlags,
        )
}
