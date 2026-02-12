package io.github.yashkasera.alohomora.desktop.data.local

import io.github.yashkasera.alohomora.desktop.domain.model.DatabaseSnapshot
import io.github.yashkasera.alohomora.desktop.domain.repository.DatabaseRepository
import kotlinx.coroutines.flow.StateFlow

class DatabaseRepositoryImpl(
    private val store: DatabaseSnapshotStore,
) : DatabaseRepository {
    override val snapshot: StateFlow<DatabaseSnapshot> = store.snapshot

    override fun selectDatabase(name: String) {
        val database = store.snapshot.value.databases.firstOrNull { it.name == name } ?: return
        store.selectDatabase(database)
    }
}
