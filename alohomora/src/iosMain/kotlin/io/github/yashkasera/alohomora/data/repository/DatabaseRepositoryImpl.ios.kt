package io.github.yashkasera.alohomora.data.repository

import androidx.sqlite.SQLITE_DATA_BLOB
import androidx.sqlite.SQLITE_DATA_FLOAT
import androidx.sqlite.SQLITE_DATA_INTEGER
import androidx.sqlite.SQLITE_DATA_NULL
import androidx.sqlite.SQLITE_DATA_TEXT
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.yashkasera.alohomora.common.DatabaseInfo
import io.github.yashkasera.alohomora.common.TableColumn
import io.github.yashkasera.alohomora.common.TableData
import io.github.yashkasera.alohomora.common.TableSchema
import io.github.yashkasera.alohomora.devtools.DevToolsDatabaseOverrides
import io.github.yashkasera.alohomora.domain.repository.QueryResult
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSLibraryDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import kotlin.time.measureTime

private const val MAX_ROW_LIMIT = 1000

private val AUXILIARY_SUFFIXES = listOf("-wal", "-shm", "-journal", ".corrupt")
private val DATABASE_SUFFIXES = listOf(".db", ".sqlite", ".sqlite3")
private val DEFAULT_EXCLUDES = setOf("alohomora.db")

internal actual class PlatformDatabaseAccessor actual constructor()

internal actual suspend fun PlatformDatabaseAccessor.listDatabases(): List<DatabaseInfo> {
    val overrides = DevToolsDatabaseOverrides.snapshot()
    val includeNames = overrides.includes.keys
    val excluded = (DEFAULT_EXCLUDES + overrides.excludes) - includeNames

    val discovered = LinkedHashMap<String, DatabaseInfo>()
    searchRoots().forEach { root ->
        filesIn(root).forEach { path ->
            val name = path.substringAfterLast('/')
            if (name.isBlank()) return@forEach
            if (!looksLikeDatabase(name)) return@forEach
            if (name in excluded) return@forEach
            discovered.getOrPut(name) { DatabaseInfo(name = name, path = path) }
        }
    }

    overrides.includes.forEach { (name, path) ->
        val resolved = path ?: discovered[name]?.path ?: return@forEach
        discovered[name] = DatabaseInfo(name = name, path = resolved)
    }

    return discovered.values.toList()
}

internal actual suspend fun PlatformDatabaseAccessor.listTables(databasePath: String): List<String> =
    withConnection(databasePath, { emptyList() }) { connection ->
        queryTables(connection)
    }

internal actual suspend fun PlatformDatabaseAccessor.getTableData(
    databasePath: String,
    tableName: String,
    limit: Int,
): TableData = withConnection(databasePath, { TableData(emptyList(), emptyList()) }) { connection ->
    if (tableName !in queryTables(connection)) return@withConnection TableData(emptyList(), emptyList())
    val safeLimit = limit.coerceIn(1, MAX_ROW_LIMIT)

    val columnTypes = mutableMapOf<String, String>()
    connection.prepare("PRAGMA table_info(`$tableName`)").use { statement ->
        while (statement.step()) {
            columnTypes[statement.getText(1)] = statement.getText(2)
        }
    }

    connection.prepare("SELECT * FROM `$tableName` LIMIT $safeLimit").use { statement ->
        val columns = (0 until statement.getColumnCount()).map { index ->
            val name = statement.getColumnName(index)
            TableColumn(name = name, type = columnTypes[name] ?: "TEXT")
        }
        val rows = mutableListOf<Map<String, String>>()
        while (statement.step()) {
            val row = LinkedHashMap<String, String>(columns.size)
            columns.forEachIndexed { index, col ->
                row[col.name] = readColumnValue(statement, index)
            }
            rows.add(row)
        }
        TableData(columns = columns, rows = rows)
    }
}

internal actual suspend fun PlatformDatabaseAccessor.getTableSchema(
    databasePath: String,
    tableName: String,
): TableSchema = withConnection(databasePath, {
    TableSchema(name = tableName, columns = emptyList(), primaryKey = null, indexes = emptyList())
}) { connection ->
    val columns = mutableListOf<TableColumn>()
    var primaryKey: String? = null

    connection.prepare("PRAGMA table_info(`$tableName`)").use { statement ->
        while (statement.step()) {
            val name = statement.getText(1)
            val type = statement.getText(2)
            if (statement.getLong(5) > 0) primaryKey = name
            columns += TableColumn(name = name, type = type)
        }
    }

    val indexes = connection.prepare("PRAGMA index_list(`$tableName`)").use { statement ->
        buildList { while (statement.step()) add(statement.getText(1)) }
    }

    TableSchema(name = tableName, columns = columns, primaryKey = primaryKey, indexes = indexes)
}

internal actual suspend fun PlatformDatabaseAccessor.executeQuery(
    databasePath: String,
    query: String,
): QueryResult {
    return withConnection(databasePath, {
        QueryResult(
            data = TableData(emptyList(), emptyList()),
            executionTimeMs = 0,
            rowsAffected = 0,
            success = false,
            errorMessage = "Could not open database",
        )
    }) { connection ->
        var resultData = TableData(emptyList(), emptyList())
        var rowsAffected = 0

        val elapsed = measureTime {
            connection.prepare(query).use { statement ->
                val columnNames = (0 until statement.getColumnCount()).map { statement.getColumnName(it) }
                val rows = mutableListOf<Map<String, String>>()
                val inferredTypes = mutableMapOf<String, String>()

                while (statement.step()) {
                    val row = LinkedHashMap<String, String>(columnNames.size)
                    columnNames.forEachIndexed { index, name ->
                        row[name] = readColumnValue(statement, index)
                        if (name !in inferredTypes) {
                            inferredTypes[name] = when (statement.getColumnType(index)) {
                                SQLITE_DATA_INTEGER -> "INTEGER"
                                SQLITE_DATA_FLOAT -> "REAL"
                                SQLITE_DATA_TEXT -> "TEXT"
                                SQLITE_DATA_BLOB -> "BLOB"
                                else -> "TEXT"
                            }
                        }
                    }
                    rows.add(row)
                }

                rowsAffected = rows.size
                resultData = TableData(
                    columns = columnNames.map { TableColumn(name = it, type = inferredTypes[it] ?: "TEXT") },
                    rows = rows,
                )
            }
        }

        QueryResult(
            data = resultData,
            executionTimeMs = elapsed.inWholeMilliseconds,
            rowsAffected = rowsAffected,
            success = true,
        )
    }
}

private fun readColumnValue(statement: androidx.sqlite.SQLiteStatement, index: Int): String =
    when (statement.getColumnType(index)) {
        SQLITE_DATA_NULL -> "NULL"
        SQLITE_DATA_INTEGER -> statement.getLong(index).toString()
        SQLITE_DATA_FLOAT -> statement.getDouble(index).toString()
        SQLITE_DATA_TEXT -> statement.getText(index)
        SQLITE_DATA_BLOB -> "BLOB(${statement.getBlob(index).size})"
        else -> ""
    }

private inline fun <T> withConnection(
    path: String,
    onFailure: () -> T,
    block: (SQLiteConnection) -> T,
): T {
    var connection: SQLiteConnection? = null
    return try {
        connection = BundledSQLiteDriver().open(path)
        block(connection)
    } catch (e: Exception) {
        println("[Alohomora] Failed to access database at $path: ${e.message}")
        onFailure()
    } finally {
        runCatching { connection?.close() }
    }
}

private fun queryTables(connection: SQLiteConnection): List<String> =
    connection.prepare(
        "SELECT name FROM sqlite_master WHERE type='table' " +
            "AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%'",
    ).use { statement ->
        buildList { while (statement.step()) add(statement.getText(0)) }
    }

@OptIn(ExperimentalForeignApi::class)
@Suppress("UNCHECKED_CAST")
private fun searchRoots(): List<String> =
    listOf(NSDocumentDirectory, NSLibraryDirectory, NSApplicationSupportDirectory)
        .flatMap { directory ->
            NSSearchPathForDirectoriesInDomains(directory, NSUserDomainMask, true)
                as? List<String> ?: emptyList()
        }
        .distinct()

@OptIn(ExperimentalForeignApi::class)
private fun filesIn(root: String): List<String> {
    @Suppress("UNCHECKED_CAST")
    val names = NSFileManager.defaultManager.contentsOfDirectoryAtPath(root, null)
        as? List<String> ?: return emptyList()
    return names.map { "$root/$it" }
}

private fun looksLikeDatabase(name: String): Boolean {
    if (AUXILIARY_SUFFIXES.any { name.contains(it, ignoreCase = true) }) return false
    return DATABASE_SUFFIXES.any { name.endsWith(it, ignoreCase = true) }
}
