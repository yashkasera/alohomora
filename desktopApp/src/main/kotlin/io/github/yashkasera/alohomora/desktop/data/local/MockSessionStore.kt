package io.github.yashkasera.alohomora.desktop.data.local

import io.github.yashkasera.alohomora.common.MockRule
import java.io.File
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class MockSessionStore {

    private val json = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
        ignoreUnknownKeys = true
    }

    private val baseDir: File by lazy {
        val home = System.getProperty("user.home")
        File(home, ".alohomora/mock-sessions")
    }

    private fun indexFile() = File(baseDir, "index.json")
    private fun sessionFile(id: String) = File(baseDir, "$id.json")

    suspend fun listSessions(): List<MockSessionSummary> = withContext(Dispatchers.IO) {
        readIndex().sessions
    }

    suspend fun loadSession(id: String): MockSession? = withContext(Dispatchers.IO) {
        val file = sessionFile(id)
        if (!file.exists()) return@withContext null
        runCatching { json.decodeFromString<MockSession>(file.readText()) }.getOrNull()
    }

    suspend fun saveSession(session: MockSession): Unit = withContext(Dispatchers.IO) {
        baseDir.mkdirs()
        sessionFile(session.id).writeText(json.encodeToString(MockSession.serializer(), session))
        val index = readIndex()
        val summary = MockSessionSummary(
            id = session.id,
            name = session.name,
            ruleCount = session.rules.size,
            updatedAt = session.updatedAt,
        )
        val updated = index.copy(
            sessions = index.sessions.filter { it.id != session.id } + summary,
        )
        writeIndex(updated)
    }

    suspend fun deleteSession(id: String): Unit = withContext(Dispatchers.IO) {
        sessionFile(id).delete()
        val index = readIndex()
        val updated = index.copy(
            sessions = index.sessions.filter { it.id != id },
            lastActiveSessionId = if (index.lastActiveSessionId == id) null
            else index.lastActiveSessionId,
        )
        writeIndex(updated)
    }

    suspend fun setLastActive(id: String?): Unit = withContext(Dispatchers.IO) {
        val index = readIndex()
        if (index.lastActiveSessionId != id) {
            writeIndex(index.copy(lastActiveSessionId = id))
        }
    }

    suspend fun loadLastActive(): MockSession? = withContext(Dispatchers.IO) {
        val index = readIndex()
        val id = index.lastActiveSessionId ?: return@withContext null
        loadSession(id)
    }

    @OptIn(ExperimentalUuidApi::class)
    fun newSession(name: String, rules: List<MockRule>): MockSession {
        val now = System.currentTimeMillis()
        return MockSession(
            id = Uuid.random().toString(),
            name = name,
            rules = rules,
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun readIndex(): MockSessionIndex {
        val file = indexFile()
        if (!file.exists()) return MockSessionIndex()
        return runCatching { json.decodeFromString<MockSessionIndex>(file.readText()) }
            .getOrDefault(MockSessionIndex())
    }

    private fun writeIndex(index: MockSessionIndex) {
        baseDir.mkdirs()
        indexFile().writeText(json.encodeToString(MockSessionIndex.serializer(), index))
    }
}
