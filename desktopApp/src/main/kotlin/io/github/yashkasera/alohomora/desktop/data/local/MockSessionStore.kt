package io.github.yashkasera.alohomora.desktop.data.local

import io.github.yashkasera.alohomora.common.MockRule
import io.github.yashkasera.alohomora.desktop.data.config.LocalConfigStore
import io.github.yashkasera.alohomora.desktop.domain.config.ConfigKind
import java.util.prefs.Preferences
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Persists mock sets as versioned config artifacts through [LocalConfigStore] (`mocks/{slug}.json`),
 * so they share the LOCAL/TEAM store and byte-stable output with journeys and deep links. The former
 * `index.json` is retired: identity is the artifact's id, "when" comes from the file's modification
 * time, and the personal "last active" pointer lives in a pref rather than a versioned file.
 *
 * This is only the persistence backend. [NetworkRulesViewModel] remains the single writer and the
 * runtime source of truth, so the MCP mock tools that route through it never desync.
 */
class MockSessionStore(
    private val local: LocalConfigStore = LocalConfigStore(),
) {
    private val prefs = Preferences.userRoot()
        .node("io/github/yashkasera/alohomora/desktop/mocks")

    suspend fun listSessions(): List<MockSessionSummary> =
        local.list(ConfigKind.MockSets).map { session ->
            MockSessionSummary(
                id = session.id,
                name = session.name,
                ruleCount = session.rules.size,
                updatedAt = local.lastModified(ConfigKind.MockSets, session.id),
            )
        }

    suspend fun loadSession(id: String): MockSession? =
        local.list(ConfigKind.MockSets).firstOrNull { it.id == id }

    suspend fun saveSession(session: MockSession) = local.save(ConfigKind.MockSets, session)

    suspend fun deleteSession(id: String) {
        local.delete(ConfigKind.MockSets, id)
        if (loadLastActiveId() == id) setLastActive(null)
    }

    fun setLastActive(id: String?) {
        if (id.isNullOrBlank()) prefs.remove(KEY_LAST_ACTIVE) else prefs.put(KEY_LAST_ACTIVE, id)
    }

    suspend fun loadLastActive(): MockSession? = loadLastActiveId()?.let { loadSession(it) }

    @OptIn(ExperimentalUuidApi::class)
    fun newSession(name: String, rules: List<MockRule>): MockSession =
        MockSession(id = Uuid.random().toString(), name = name, rules = rules)

    private fun loadLastActiveId(): String? = prefs.get(KEY_LAST_ACTIVE, null)?.ifBlank { null }

    private companion object {
        const val KEY_LAST_ACTIVE = "last_active"
    }
}
