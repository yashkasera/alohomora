package io.github.yashkasera.alohomora.data.repository

import io.github.yashkasera.alohomora.common.DatabaseInfo
import io.github.yashkasera.alohomora.common.TableData
import io.github.yashkasera.alohomora.common.TableSchema
import io.github.yashkasera.alohomora.domain.repository.QueryResult
import io.github.yashkasera.alohomora.domain.repository.VaultRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Platform-specific database accessor for vault operations.
 * Implemented in androidMain and iosMain.
 */
internal expect class PlatformDatabaseAccessor()

/**
 * Implementation of [VaultRepository] that provides database browsing capabilities.
 */
internal class VaultRepositoryImpl(
    private val platformAccessor: PlatformDatabaseAccessor = PlatformDatabaseAccessor(),
) : VaultRepository {

    override fun listDatabases(): Flow<List<DatabaseInfo>> = flow {
        val databases = platformAccessor.listDatabases()
        emit(databases)
    }.flowOn(Dispatchers.IO)

    override fun listTables(databasePath: String): Flow<List<String>> = flow {
        val tables = platformAccessor.listTables(databasePath)
        emit(tables)
    }.flowOn(Dispatchers.IO)

    override fun getTableData(
        databasePath: String,
        tableName: String,
        limit: Int,
    ): Flow<TableData> = flow {
        val data = platformAccessor.getTableData(databasePath, tableName, limit)
        emit(data)
    }.flowOn(Dispatchers.IO)

    override fun getTableSchema(
        databasePath: String,
        tableName: String,
    ): Flow<TableSchema> = flow {
        val schema = platformAccessor.getTableSchema(databasePath, tableName)
        emit(schema)
    }.flowOn(Dispatchers.IO)

    override fun executeQuery(
        databasePath: String,
        query: String,
    ): Flow<QueryResult> = flow {
        try {
            val result = platformAccessor.executeQuery(databasePath, query)
            emit(result)
        } catch (e: Exception) {
            emit(
                QueryResult(
                    data = TableData(emptyList(), emptyList()),
                    executionTimeMs = 0,
                    rowsAffected = 0,
                    success = false,
                    errorMessage = e.message ?: "Unknown error occurred",
                ),
            )
        }
    }.flowOn(Dispatchers.IO)
}

/**
 * Expected platform-specific database operations.
 * These will be implemented in platform-specific source sets.
 */
internal expect suspend fun PlatformDatabaseAccessor.listDatabases(): List<DatabaseInfo>
internal expect suspend fun PlatformDatabaseAccessor.listTables(databasePath: String): List<String>
internal expect suspend fun PlatformDatabaseAccessor.getTableData(
    databasePath: String,
    tableName: String,
    limit: Int,
): TableData

internal expect suspend fun PlatformDatabaseAccessor.getTableSchema(
    databasePath: String,
    tableName: String,
): TableSchema

internal expect suspend fun PlatformDatabaseAccessor.executeQuery(
    databasePath: String,
    query: String,
): QueryResult
