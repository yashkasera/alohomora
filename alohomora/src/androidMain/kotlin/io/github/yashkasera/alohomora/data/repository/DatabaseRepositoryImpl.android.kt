package io.github.yashkasera.alohomora.data.repository

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import io.github.yashkasera.alohomora.common.DatabaseInfo
import io.github.yashkasera.alohomora.common.TableColumn
import io.github.yashkasera.alohomora.common.TableData
import io.github.yashkasera.alohomora.common.TableSchema
import io.github.yashkasera.alohomora.domain.repository.QueryResult
import kotlin.system.measureTimeMillis

private const val TAG = "DatabaseRepository"

/**
 * Android implementation of platform-specific database accessor.
 */
internal actual class PlatformDatabaseAccessor actual constructor() {
    private var context: Context? = null

    fun setContext(context: Context) {
        this.context = context.applicationContext
    }

    internal fun requireContext(): Context {
        return context ?: throw IllegalStateException(
            "Context not set. Call setContext() before using PlatformDatabaseAccessor.",
        )
    }
}

internal actual suspend fun PlatformDatabaseAccessor.listDatabases(): List<DatabaseInfo> {
    val ctx = requireContext()
    return ctx.databaseList()
        .filter { name ->
            name.isNotBlank() &&
                name != "alohomora.db" &&
                !name.endsWith("-shm") &&
                !name.endsWith("-wal") &&
                !name.endsWith("-journal") &&
                !name.endsWith(".lck")
        }
        .map { name ->
            DatabaseInfo(
                name = name,
                path = ctx.getDatabasePath(name).absolutePath,
            )
        }
}

internal actual suspend fun PlatformDatabaseAccessor.listTables(databasePath: String): List<String> {
    val db = openDatabase(databasePath) ?: return emptyList()
    return try {
        db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'android_%' AND name NOT LIKE 'sqlite_%'",
            null,
        ).use { cursor ->
            val tables = mutableListOf<String>()
            while (cursor.moveToNext()) {
                tables.add(cursor.getString(0))
            }
            tables
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to list tables from $databasePath: ${e.message}", e)
        emptyList()
    } finally {
        db.close()
    }
}

internal actual suspend fun PlatformDatabaseAccessor.getTableData(
    databasePath: String,
    tableName: String,
    limit: Int,
): TableData {
    val db = openDatabase(databasePath)
        ?: return TableData(emptyList(), emptyList())

    return try {
        // Get column info from schema first
        val columnTypes = mutableMapOf<String, String>()
        db.rawQuery("PRAGMA table_info(`$tableName`)", null).use { schemaCursor ->
            val nameIndex = schemaCursor.getColumnIndex("name")
            val typeIndex = schemaCursor.getColumnIndex("type")
            while (schemaCursor.moveToNext()) {
                if (nameIndex != -1 && typeIndex != -1) {
                    val name = schemaCursor.getString(nameIndex)
                    val type = schemaCursor.getString(typeIndex)
                    columnTypes[name] = type
                }
            }
        }

        db.rawQuery("SELECT * FROM `$tableName` LIMIT $limit", null).use { cursor ->
            val columns = (0 until cursor.columnCount).map { index ->
                val name = cursor.getColumnName(index)
                TableColumn(
                    name = name,
                    type = columnTypes[name] ?: "TEXT",
                )
            }

            val rows = mutableListOf<Map<String, String>>()
            while (cursor.moveToNext()) {
                val row = mutableMapOf<String, String>()
                for (index in columns.indices) {
                    val columnName = columns[index].name
                    val value = when (cursor.getType(index)) {
                        Cursor.FIELD_TYPE_NULL -> "NULL"
                        Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(index).toString()
                        Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(index).toString()
                        Cursor.FIELD_TYPE_STRING -> cursor.getString(index) ?: ""
                        Cursor.FIELD_TYPE_BLOB -> {
                            val blob = cursor.getBlob(index)
                            if (blob == null) "NULL" else "BLOB(${blob.size})"
                        }

                        else -> ""
                    }
                    row[columnName] = value
                }
                rows.add(row)
            }
            TableData(columns = columns, rows = rows)
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to get table data for '$tableName' from $databasePath: ${e.message}", e)
        TableData(emptyList(), emptyList())
    } finally {
        db.close()
    }
}

internal actual suspend fun PlatformDatabaseAccessor.getTableSchema(
    databasePath: String,
    tableName: String,
): TableSchema {
    val db = openDatabase(databasePath)
        ?: return TableSchema(
            name = tableName,
            columns = emptyList(),
            primaryKey = null,
            indexes = emptyList(),
        )

    return try {
        // Get columns info
        val columns = mutableListOf<TableColumn>()
        var primaryKey: String? = null

        db.rawQuery("PRAGMA table_info(`$tableName`)", null).use { columnsCursor ->
            val nameIndex = columnsCursor.getColumnIndex("name")
            val typeIndex = columnsCursor.getColumnIndex("type")
            val pkIndex = columnsCursor.getColumnIndex("pk")

            while (columnsCursor.moveToNext()) {
                if (nameIndex == -1 || typeIndex == -1) continue
                val name = columnsCursor.getString(nameIndex)
                val type = columnsCursor.getString(typeIndex)
                val pk = pkIndex != -1 && columnsCursor.getInt(pkIndex) == 1

                if (pk) primaryKey = name
                columns.add(TableColumn(name = name, type = type))
            }
        }

        // Get indexes
        val indexes = mutableListOf<String>()
        db.rawQuery("PRAGMA index_list(`$tableName`)", null).use { indexCursor ->
            val indexNameIndex = indexCursor.getColumnIndex("name")
            while (indexCursor.moveToNext()) {
                if (indexNameIndex == -1) continue
                indexes.add(indexCursor.getString(indexNameIndex))
            }
        }

        TableSchema(
            name = tableName,
            columns = columns,
            primaryKey = primaryKey,
            indexes = indexes,
        )
    } catch (e: Exception) {
        Log.e(
            TAG,
            "Failed to get table schema for '$tableName' from $databasePath: ${e.message}",
            e,
        )
        TableSchema(
            name = tableName,
            columns = emptyList(),
            primaryKey = null,
            indexes = emptyList(),
        )
    } finally {
        db.close()
    }
}

internal actual suspend fun PlatformDatabaseAccessor.executeQuery(
    databasePath: String,
    query: String,
): QueryResult {
    val db = openDatabase(databasePath)
        ?: return QueryResult(
            data = TableData(emptyList(), emptyList()),
            executionTimeMs = 0,
            rowsAffected = 0,
            success = false,
            errorMessage = "Could not open database",
        )

    return try {
        var rowsAffected = 0
        var resultData: TableData

        val executionTimeMs = measureTimeMillis {
            db.rawQuery(query, null).use { cursor ->
                // Build columns with names only first
                val columnNames = (0 until cursor.columnCount).map { index ->
                    cursor.getColumnName(index)
                }

                // Build rows
                val rows = mutableListOf<Map<String, String>>()
                val columnTypes = mutableMapOf<String, String>()

                while (cursor.moveToNext()) {
                    val row = mutableMapOf<String, String>()
                    for (index in columnNames.indices) {
                        val columnName = columnNames[index]
                        val value = when (cursor.getType(index)) {
                            Cursor.FIELD_TYPE_NULL -> "NULL"
                            Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(index).toString()
                            Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(index).toString()
                            Cursor.FIELD_TYPE_STRING -> cursor.getString(index) ?: ""
                            Cursor.FIELD_TYPE_BLOB -> {
                                val blob = cursor.getBlob(index)
                                if (blob == null) "NULL" else "BLOB(${blob.size})"
                            }

                            else -> ""
                        }
                        row[columnName] = value

                        // Infer type from first row if not already set
                        if (!columnTypes.containsKey(columnName)) {
                            columnTypes[columnName] = when (cursor.getType(index)) {
                                Cursor.FIELD_TYPE_NULL -> "NULL"
                                Cursor.FIELD_TYPE_INTEGER -> "INTEGER"
                                Cursor.FIELD_TYPE_FLOAT -> "REAL"
                                Cursor.FIELD_TYPE_STRING -> "TEXT"
                                Cursor.FIELD_TYPE_BLOB -> "BLOB"
                                else -> "TEXT"
                            }
                        }
                    }
                    rows.add(row)
                }

                // Build columns with types (default to TEXT if no data)
                val columns = columnNames.map { name ->
                    TableColumn(name = name, type = columnTypes[name] ?: "TEXT")
                }

                rowsAffected = rows.size
                resultData = TableData(columns = columns, rows = rows)
            }
        }

        QueryResult(
            data = resultData,
            executionTimeMs = executionTimeMs,
            rowsAffected = rowsAffected,
            success = true,
        )
    } catch (e: Exception) {
        Log.e(TAG, "Failed to execute query on $databasePath: ${e.message}", e)
        QueryResult(
            data = TableData(emptyList(), emptyList()),
            executionTimeMs = 0,
            rowsAffected = 0,
            success = false,
            errorMessage = e.message ?: "Query execution failed",
        )
    } finally {
        db.close()
    }
}

private fun openDatabase(path: String): SQLiteDatabase? {
    return try {
        // Try to open with WAL mode enabled for reading (allows reading while Room is writing)
        SQLiteDatabase.openDatabase(
            path,
            null,
            SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING,
        )
    } catch (e: Exception) {
        Log.w(TAG, "Failed to open database with WAL mode at $path, trying without: ${e.message}")
        try {
            // Fallback to basic read-only open
            SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY)
        } catch (e2: Exception) {
            Log.e(TAG, "Failed to open database at $path: ${e2.message}")
            null
        }
    }
}

