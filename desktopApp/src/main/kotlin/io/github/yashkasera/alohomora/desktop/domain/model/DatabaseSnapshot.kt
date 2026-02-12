package io.github.yashkasera.alohomora.desktop.domain.model

data class DatabaseInfo(
    val name: String,
    val path: String,
)

data class DatabaseTableColumn(
    val name: String,
    val type: String,
    val notNull: Boolean,
    val primaryKey: Boolean,
    val defaultValue: String? = null,
)

data class DatabaseTableSchema(
    val name: String,
    val columns: List<DatabaseTableColumn>,
    val primaryKey: String?,
    val indexes: List<String>,
)

data class DatabaseSchema(
    val databaseName: String? = null,
    val tables: List<String>,
    val schemas: List<DatabaseTableSchema>,
)

data class DatabaseTable(
    val databaseName: String? = null,
    val name: String,
    val columns: List<String>,
    val rows: List<Map<String, String?>>,
)

data class DatabaseSnapshot(
    val databases: List<DatabaseInfo> = emptyList(),
    val selectedDatabase: DatabaseInfo? = null,
    val schema: DatabaseSchema? = null,
    val table: DatabaseTable? = null,
)
