package io.github.yashkasera.alohomora.desktop.data.config

import io.github.yashkasera.alohomora.desktop.domain.config.ConfigItem
import io.github.yashkasera.alohomora.desktop.domain.config.ConfigKind
import io.github.yashkasera.alohomora.desktop.domain.config.ConfigScope
import io.github.yashkasera.alohomora.desktop.domain.config.ConfigState
import io.github.yashkasera.alohomora.desktop.domain.config.ConfigStore
import io.github.yashkasera.alohomora.desktop.domain.config.Proposal
import io.github.yashkasera.alohomora.desktop.domain.config.SyncStatus

/**
 * Routes [ConfigStore] calls by scope: LOCAL to [LocalConfigStore], TEAM to the git-backed store.
 *
 * The git store is null until a repo is connected (Phase 2), so today TEAM lists empty and the team
 * actions report NotConnected rather than pretending. This keeps the LOCAL path fully usable with zero
 * setup — the whole reason the seam exists.
 */
class ConfigStoreFacade(
    private val local: LocalConfigStore = LocalConfigStore(),
    // private val team: GitConfigStore? = null,   // wired in Phase 2
) : ConfigStore {

    override suspend fun <T> list(kind: ConfigKind<T>, scope: ConfigScope): List<ConfigItem<T>> =
        when (scope) {
            ConfigScope.LOCAL -> local.list(kind).map {
                ConfigItem(value = it, scope = ConfigScope.LOCAL, state = ConfigState.LOCAL)
            }
            ConfigScope.TEAM -> emptyList()
        }

    override suspend fun <T> saveLocal(kind: ConfigKind<T>, item: T) = local.save(kind, item)

    override suspend fun <T> deleteLocal(kind: ConfigKind<T>, id: String) = local.delete(kind, id)

    override suspend fun <T> shareWithTeam(kind: ConfigKind<T>, item: T): Proposal =
        error("Team config is not connected. Connect a repository first (Phase 2).")

    override suspend fun sync(): SyncStatus = SyncStatus.NotConnected
}
