package io.github.yashkasera.alohomora.devtools

import io.github.yashkasera.alohomora.common.AppDatabaseInfo
import io.github.yashkasera.alohomora.common.DatabaseSchemaSnapshot
import io.github.yashkasera.alohomora.common.DatabaseTableSnapshot
import io.github.yashkasera.alohomora.data.db.AlohomoraDb

internal expect class DevToolsDatabaseInspector(
    database: AlohomoraDb,
    appDatabaseProvider: DevToolsAppDatabaseProvider,
) {
    fun listDatabases(): List<AppDatabaseInfo>
    fun loadSchema(databaseName: String): DatabaseSchemaSnapshot
    fun loadTable(databaseName: String, tableName: String, limit: Int): DatabaseTableSnapshot
    fun updateCell(
        databaseName: String,
        tableName: String,
        primaryKeys: Map<String, String>,
        columnName: String,
        newValue: String?,
    ): Boolean
}
