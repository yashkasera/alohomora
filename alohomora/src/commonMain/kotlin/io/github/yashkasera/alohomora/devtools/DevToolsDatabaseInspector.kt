package io.github.yashkasera.alohomora.devtools

import io.github.yashkasera.alohomora.data.db.AlohomoraDb

internal expect class DevToolsDatabaseInspector(
    database: AlohomoraDb,
    appDatabaseProvider: DevToolsAppDatabaseProvider,
) {
    fun listDatabases(): List<AppDatabaseInfo>
    fun loadSchema(databaseName: String): DatabaseSchemaSnapshot
    fun loadTable(databaseName: String, tableName: String, limit: Int): DatabaseTableSnapshot
}
