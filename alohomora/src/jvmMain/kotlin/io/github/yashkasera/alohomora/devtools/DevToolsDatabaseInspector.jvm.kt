package io.github.yashkasera.alohomora.devtools

import io.github.yashkasera.alohomora.common.AppDatabaseInfo
import io.github.yashkasera.alohomora.common.DatabaseSchemaSnapshot
import io.github.yashkasera.alohomora.common.DatabaseTableSnapshot
import io.github.yashkasera.alohomora.data.db.AlohomoraDb

internal actual class DevToolsDatabaseInspector actual constructor(
    database: AlohomoraDb,
    appDatabaseProvider: DevToolsAppDatabaseProvider,
) {
    actual fun listDatabases(): List<AppDatabaseInfo> {
        return emptyList()
    }

    actual fun loadSchema(databaseName: String): DatabaseSchemaSnapshot {
        return DatabaseSchemaSnapshot(
            databaseName = databaseName,
            tables = emptyList(),
            schemas = emptyList(),
        )
    }

    actual fun loadTable(databaseName: String, tableName: String, limit: Int): DatabaseTableSnapshot {
        return DatabaseTableSnapshot(
            databaseName = databaseName,
            name = tableName,
            columns = emptyList(),
            rows = emptyList(),
        )
    }
}
