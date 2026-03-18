package io.github.yashkasera.alohomora.common

data class DatabaseInfo(
    val name: String,
    val path: String,
)

data class TableColumn(
    val name: String,
    val type: String,
)

data class TableData(
    val columns: List<TableColumn>,
    val rows: List<Map<String, String>>,
)

data class TableSchema(
    val name: String,
    val columns: List<TableColumn>,
    val primaryKey: String?,
    val indexes: List<String>,
)
