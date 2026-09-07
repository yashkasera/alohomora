package io.github.yashkasera.alohomora.desktop.data.local

import io.github.yashkasera.alohomora.common.MockRule
import io.github.yashkasera.alohomora.desktop.data.config.LocalConfigStore
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class MockSessionStoreTest {

    private lateinit var baseDir: File
    private lateinit var store: MockSessionStore

    @BeforeTest
    fun setUp() {
        baseDir = Files.createTempDirectory("alohomora-mock-store-test").toFile()
        store = MockSessionStore(LocalConfigStore(baseDir))
    }

    @AfterTest
    fun tearDown() {
        store.setLastActive(null) // do not leave the test pointer in the developer's real prefs
        baseDir.deleteRecursively()
    }

    private fun rule() = MockRule(id = "r1", urlPattern = "/api/users", responseBody = "{}")

    @Test
    fun `saves and lists a mock set with a mtime derived timestamp`() = runBlocking {
        val session = store.newSession("My mocks", listOf(rule()))
        store.saveSession(session)

        val summaries = store.listSessions()
        assertEquals(1, summaries.size)
        assertEquals("My mocks", summaries.single().name)
        assertEquals(1, summaries.single().ruleCount)
        assertTrue(summaries.single().updatedAt > 0)
        assertEquals(session.id, store.loadSession(session.id)?.id)
    }

    @Test
    fun `the persisted artifact does not carry a mutable timestamp`() = runBlocking {
        store.saveSession(store.newSession("My mocks", listOf(rule())))
        val file = File(baseDir, "mocks").listFiles()!!.single()
        assertEquals("my-mocks.json", file.name)
        assertFalse(file.readText().contains("updatedAt"))
        assertFalse(file.readText().contains("createdAt"))
    }

    @Test
    fun `last active round-trips and clears on delete`() = runBlocking {
        val session = store.newSession("My mocks", listOf(rule()))
        store.saveSession(session)
        store.setLastActive(session.id)
        assertEquals(session.id, store.loadLastActive()?.id)

        store.deleteSession(session.id)
        assertTrue(store.listSessions().isEmpty())
        assertNull(store.loadLastActive())
    }
}
