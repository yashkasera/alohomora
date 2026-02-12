package io.github.yashkasera.alohomora.desktop.data.local

import io.github.yashkasera.alohomora.desktop.domain.model.DatabaseInfo
import io.github.yashkasera.alohomora.desktop.domain.model.DatabaseSchema
import io.github.yashkasera.alohomora.desktop.domain.model.DatabaseSnapshot
import io.github.yashkasera.alohomora.desktop.domain.model.DatabaseTable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DatabaseSnapshotStore {
    private val _snapshot = MutableStateFlow(DatabaseSnapshot())
    val snapshot: StateFlow<DatabaseSnapshot> = _snapshot.asStateFlow()

    fun replaceDatabases(databases: List<DatabaseInfo>, selectedName: String?) {
        val selected = databases.firstOrNull { it.name == selectedName } ?: databases.firstOrNull()
        _snapshot.value = _snapshot.value.copy(
            databases = databases,
            selectedDatabase = selected,
        )
    }

    fun selectDatabase(database: DatabaseInfo) {
        _snapshot.value = _snapshot.value.copy(
            selectedDatabase = database,
            schema = null,
            table = null,
        )
    }

    fun replaceSchema(schema: DatabaseSchema) {
        _snapshot.value = _snapshot.value.copy(schema = schema)
    }

    fun applySnapshot(schema: DatabaseSchema?, table: DatabaseTable?) {
        _snapshot.value = _snapshot.value.copy(
            schema = schema ?: _snapshot.value.schema,
            table = table ?: _snapshot.value.table,
        )
    }

    fun clear() {
        _snapshot.value = DatabaseSnapshot()
    }
}
