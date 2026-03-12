package io.github.yashkasera.alohomora.data.repository

import io.github.yashkasera.alohomora.domain.repository.QueryResult
import io.github.yashkasera.alohomora.presentation.ui.screens.vault.DatabaseInfo
import io.github.yashkasera.alohomora.presentation.ui.screens.vault.TableData
import io.github.yashkasera.alohomora.presentation.ui.screens.vault.TableSchema

/**
 * iOS implementation of platform-specific database accessor.
 * Currently returns empty data - full implementation requires platform-specific SQLite access.
 */
internal actual class PlatformDatabaseAccessor actual constructor()

internal actual suspend fun PlatformDatabaseAccessor.listDatabases(): List<DatabaseInfo> {
    // TODO: Implement iOS-specific database discovery using CoreData or SQLite.swift
    return emptyList()
}

internal actual suspend fun PlatformDatabaseAccessor.listTables(databasePath: String): List<String> {
    // TODO: Implement iOS-specific table listing
    return emptyList()
}

internal actual suspend fun PlatformDatabaseAccessor.getTableData(
    databasePath: String,
    tableName: String,
    limit: Int
): TableData {
    // TODO: Implement iOS-specific table data retrieval
    return TableData(emptyList(), emptyList())
}

internal actual suspend fun PlatformDatabaseAccessor.getTableSchema(
    databasePath: String,
    tableName: String
): TableSchema {
    // TODO: Implement iOS-specific schema retrieval
    return TableSchema(name = tableName, columns = emptyList(), primaryKey = null, indexes = emptyList())
}

internal actual suspend fun PlatformDatabaseAccessor.executeQuery(
    databasePath: String,
    query: String
): QueryResult {
    // TODO: Implement iOS-specific query execution
    return QueryResult(
        data = TableData(emptyList(), emptyList()),
        executionTimeMs = 0,
        rowsAffected = 0,
        success = false,
        errorMessage = "iOS database access not yet implemented"
    )
}
