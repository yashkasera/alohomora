package io.github.yashkasera.alohomora.desktop.data.config

import io.github.yashkasera.alohomora.common.journey.JourneyDefinition
import io.github.yashkasera.alohomora.common.journey.JourneyStep
import io.github.yashkasera.alohomora.desktop.domain.config.ConfigKind
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class LocalConfigStoreTest {

    private lateinit var baseDir: File
    private lateinit var store: LocalConfigStore

    @BeforeTest
    fun setUp() {
        baseDir = Files.createTempDirectory("alohomora-config-test").toFile()
        store = LocalConfigStore(baseDir)
    }

    @AfterTest
    fun tearDown() {
        baseDir.deleteRecursively()
    }

    private fun journey(id: String, name: String, steps: List<String> = listOf("a")) =
        JourneyDefinition(
            id = id,
            name = name,
            steps = steps.map { JourneyStep(id = "s-$it", eventName = it) },
        )

    @Test
    fun `saves and lists an artifact`() = runTest {
        val j = journey("j1", "Checkout happy path")
        store.save(ConfigKind.Journeys, j)

        val listed = store.list(ConfigKind.Journeys)
        assertEquals(listOf(j), listed)
    }

    @Test
    fun `filename is a readable slug of the name`() = runTest {
        store.save(ConfigKind.Journeys, journey("j1", "Checkout Happy Path!"))

        val files = File(baseDir, "journeys").listFiles()!!.map { it.name }
        assertEquals(listOf("checkout-happy-path.json"), files)
    }

    @Test
    fun `re-saving unchanged content is byte identical`() = runTest {
        val j = journey("j1", "Checkout")
        store.save(ConfigKind.Journeys, j)
        val file = File(baseDir, "journeys").listFiles()!!.single()
        val first = file.readText()

        store.save(ConfigKind.Journeys, j)
        val second = file.readText()

        assertEquals(first, second)
    }

    @Test
    fun `rename keeps the original file rather than churning a new one`() = runTest {
        store.save(ConfigKind.Journeys, journey("j1", "Original name"))
        store.save(ConfigKind.Journeys, journey("j1", "A different name"))

        val files = File(baseDir, "journeys").listFiles()!!.map { it.name }
        assertEquals(listOf("original-name.json"), files)
        assertEquals("A different name", store.list(ConfigKind.Journeys).single().name)
    }

    @Test
    fun `two artifacts with the same name get distinct files`() = runTest {
        store.save(ConfigKind.Journeys, journey("j1", "Checkout"))
        store.save(ConfigKind.Journeys, journey("j2", "Checkout"))

        val files = File(baseDir, "journeys").listFiles()!!.map { it.name }.sorted()
        assertEquals(listOf("checkout-2.json", "checkout.json"), files)
        assertEquals(2, store.list(ConfigKind.Journeys).size)
    }

    @Test
    fun `delete removes the artifact by id`() = runTest {
        store.save(ConfigKind.Journeys, journey("j1", "Checkout"))
        store.delete(ConfigKind.Journeys, "j1")

        assertTrue(store.list(ConfigKind.Journeys).isEmpty())
    }

    @Test
    fun `listing an absent kind dir yields empty`() = runTest {
        assertTrue(store.list(ConfigKind.Journeys).isEmpty())
    }

    @Test
    fun `an unparseable file is skipped rather than failing the list`() = runTest {
        store.save(ConfigKind.Journeys, journey("j1", "Checkout"))
        File(baseDir, "journeys/garbage.json").writeText("{ not valid")

        val listed = store.list(ConfigKind.Journeys)
        assertEquals("j1", listed.single().id)
        assertNull(listed.firstOrNull { it.id != "j1" })
    }
}
