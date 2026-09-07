package io.github.yashkasera.alohomora.desktop.mcp

import io.github.yashkasera.alohomora.common.deeplink.DeepLinkDef
import io.github.yashkasera.alohomora.common.deeplink.DeepLinkParam
import io.github.yashkasera.alohomora.common.deeplink.ParamType
import io.github.yashkasera.alohomora.common.journey.JourneyDefinition
import io.github.yashkasera.alohomora.common.journey.JourneyStep
import io.github.yashkasera.alohomora.desktop.data.config.ConfigStoreFacade
import io.github.yashkasera.alohomora.desktop.data.config.LocalConfigStore
import io.github.yashkasera.alohomora.desktop.domain.config.ConfigKind
import io.github.yashkasera.alohomora.desktop.domain.config.ConfigScope
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive

class AlohomoraJourneyMcpWriteDataTest {

    private lateinit var baseDir: File
    private lateinit var store: ConfigStoreFacade

    @BeforeTest
    fun setUp() {
        baseDir = Files.createTempDirectory("alohomora-mcp-write-test").toFile()
        store = ConfigStoreFacade(LocalConfigStore(baseDir))
    }

    @AfterTest
    fun tearDown() {
        baseDir.deleteRecursively()
    }

    @Test
    fun `save_journey persists a valid definition`() = runBlocking {
        val def = JourneyDefinition(
            id = "j1",
            name = "Checkout",
            steps = listOf(JourneyStep(id = "s1", eventName = "login")),
        )
        val element = json.encodeToJsonElement(JourneyDefinition.serializer(), def)

        val result = AlohomoraJourneyMcpWriteData.saveJourney(store, element)

        assertTrue(result is WriteResult.Ok)
        assertEquals("j1", store.list(ConfigKind.Journeys, ConfigScope.LOCAL).single().value.id)
    }

    @Test
    fun `save_journey rejects malformed json`() = runBlocking {
        val result = AlohomoraJourneyMcpWriteData.saveJourney(store, JsonPrimitive("not a definition"))
        assertTrue(result is WriteResult.Error)
    }

    @Test
    fun `fire_deeplink rejects an unknown id`() = runBlocking {
        val result = AlohomoraJourneyMcpWriteData.fireDeepLink(store, "device-1", "missing", emptyMap())
        assertTrue(result is WriteResult.Error)
    }

    @Test
    fun `fire_deeplink rejects arguments that fail validation`() = runBlocking {
        LocalConfigStore(baseDir).save(
            ConfigKind.DeepLinks,
            DeepLinkDef(
                id = "d1",
                name = "Verify",
                module = "kyc",
                uriTemplate = "app://kyc/{userId}",
                params = listOf(DeepLinkParam(name = "userId", type = ParamType.UUID)),
            ),
        )

        // Missing the required userId, so it never reaches adb.
        val result = AlohomoraJourneyMcpWriteData.fireDeepLink(store, "device-1", "d1", emptyMap())
        assertTrue(result is WriteResult.Error)
    }
}
