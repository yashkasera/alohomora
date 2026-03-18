package io.github.yashkasera.alohomora.domain.repository

import io.github.yashkasera.alohomora.common.DatabaseInfo
import io.github.yashkasera.alohomora.common.TableData
import io.github.yashkasera.alohomora.common.TableSchema
import kotlinx.coroutines.flow.Flow

/**
 * Repository for vault/database browsing operations.
 */
internal interface VaultRepository {

    /**
     * Retrieves a list of all available databases.
     *
     * @return Flow of database list
     */
    fun listDatabases(): Flow<List<DatabaseInfo>>

    /**
     * Retrieves a list of all tables in the specified database.
     *
     * @param databasePath Path to the database file
     * @return Flow of table names
     */
    fun listTables(databasePath: String): Flow<List<String>>

    /**
     * Retrieves data from a specific table.
     *
     * @param databasePath Path to the database file
     * @param tableName Name of the table
     * @param limit Maximum number of rows to retrieve
     * @return Flow of table data
     */
    fun getTableData(
        databasePath: String,
        tableName: String,
        limit: Int = 100
    ): Flow<TableData>

    /**
     * Retrieves the schema information for a specific table.
     *
     * @param databasePath Path to the database file
     * @param tableName Name of the table
     * @return Flow of table schema
     */
    fun getTableSchema(
        databasePath: String,
        tableName: String
    ): Flow<TableSchema>

    /**
     * Executes a custom SQL query.
     *
     * @param databasePath Path to the database file
     * @param query SQL query to execute
     * @return Flow of query result with execution metadata
     */
    fun executeQuery(
        databasePath: String,
        query: String
    ): Flow<QueryResult>
}

/**
 * Result of executing a SQL query.
 */
data class QueryResult(
    val data: TableData,
    val executionTimeMs: Long,
    val rowsAffected: Int,
    val success: Boolean = true,
    val errorMessage: String? = null
)
