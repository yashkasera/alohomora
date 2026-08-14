package io.github.yashkasera.alohomora.domain.repository

import io.github.yashkasera.alohomora.common.DatabaseInfo
import io.github.yashkasera.alohomora.common.TableData
import io.github.yashkasera.alohomora.common.TableSchema
import kotlinx.coroutines.flow.Flow

internal interface DatabaseRepository {

    fun listDatabases(): Flow<List<DatabaseInfo>>

    fun listTables(databasePath: String): Flow<List<String>>

    fun getTableData(
        databasePath: String,
        tableName: String,
        limit: Int = 100,
    ): Flow<TableData>

    fun getTableSchema(
        databasePath: String,
        tableName: String,
    ): Flow<TableSchema>

    fun executeQuery(
        databasePath: String,
        query: String,
    ): Flow<QueryResult>
}

internal data class QueryResult(
    val data: TableData,
    val executionTimeMs: Long,
    val rowsAffected: Int,
    val success: Boolean = true,
    val errorMessage: String? = null,
)
