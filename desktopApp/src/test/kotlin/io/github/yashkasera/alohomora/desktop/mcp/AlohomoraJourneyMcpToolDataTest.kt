package io.github.yashkasera.alohomora.desktop.mcp

import io.github.yashkasera.alohomora.common.Event
import io.github.yashkasera.alohomora.common.deeplink.DeepLinkDef
import io.github.yashkasera.alohomora.common.journey.JourneyDefinition
import io.github.yashkasera.alohomora.common.journey.JourneyStep
import io.github.yashkasera.alohomora.desktop.FakeDevToolsRepository
import io.github.yashkasera.alohomora.desktop.data.config.ConfigStoreFacade
import io.github.yashkasera.alohomora.desktop.data.config.LocalConfigStore
import io.github.yashkasera.alohomora.desktop.domain.config.ConfigKind
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class AlohomoraJourneyMcpToolDataTest {

    private lateinit var baseDir: File
    private lateinit var store: ConfigStoreFacade

    @BeforeTest
    fun setUp() {
        baseDir = Files.createTempDirectory("alohomora-mcp-journey-test").toFile()
        store = ConfigStoreFacade(LocalConfigStore(baseDir))
    }

    @AfterTest
    fun tearDown() {
        baseDir.deleteRecursively()
    }

    private fun event(name: String, time: Long) = Event(name = name, properties = null, time = time)

    private val journey = JourneyDefinition(
        id = "j1",
        name = "Checkout",
        steps = listOf(JourneyStep(id = "s1", eventName = "login"), JourneyStep(id = "s2", eventName = "pay")),
    )

    @Test
    fun `list_journeys projects saved journeys`() = runBlocking {
        LocalConfigStore(baseDir).save(ConfigKind.Journeys, journey)

        val array = AlohomoraJourneyMcpToolData.listJourneys(store, scope = null).jsonArray
        assertEquals(1, array.size)
        val obj = array.single().jsonObject
        assertEquals("Checkout", obj["name"]!!.jsonPrimitive.content)
        assertEquals("2", obj["stepCount"]!!.jsonPrimitive.content)
    }

    @Test
    fun `verify_journey grades against captured events`() = runBlocking {
        LocalConfigStore(baseDir).save(ConfigKind.Journeys, journey)
        val repo = FakeDevToolsRepository(events = listOf(event("login", 1), event("pay", 2)))

        val report = AlohomoraJourneyMcpToolData.verifyJourney(store, repo, "j1").jsonObject
        assertEquals("PASSED", report["status"]!!.jsonPrimitive.content)
    }

    @Test
    fun `verify_journey reports an error for an unknown id`() = runBlocking {
        val repo = FakeDevToolsRepository()
        val result = AlohomoraJourneyMcpToolData.verifyJourney(store, repo, "missing").jsonObject
        assertTrue(result.containsKey("error"))
    }

    @Test
    fun `list_deeplinks projects and filters by module`() = runBlocking {
        val local = LocalConfigStore(baseDir)
        local.save(ConfigKind.DeepLinks, DeepLinkDef(id = "d1", name = "Verify", module = "kyc", uriTemplate = "a://k"))
        local.save(ConfigKind.DeepLinks, DeepLinkDef(id = "d2", name = "Home", module = "cards", uriTemplate = "a://c"))

        assertEquals(2, AlohomoraJourneyMcpToolData.listDeepLinks(store, module = null).jsonArray.size)
        val kyc = AlohomoraJourneyMcpToolData.listDeepLinks(store, module = "kyc").jsonArray
        assertEquals(1, kyc.size)
        assertEquals("Verify", kyc.single().jsonObject["name"]!!.jsonPrimitive.content)
    }
}
