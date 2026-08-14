package io.github.yashkasera.alohomora.devtools

import androidx.sqlite.SQLITE_DATA_BLOB
import androidx.sqlite.SQLITE_DATA_FLOAT
import androidx.sqlite.SQLITE_DATA_INTEGER
import androidx.sqlite.SQLITE_DATA_NULL
import androidx.sqlite.SQLITE_DATA_TEXT
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.yashkasera.alohomora.common.AppDatabaseInfo
import io.github.yashkasera.alohomora.common.DatabaseSchemaSnapshot
import io.github.yashkasera.alohomora.common.DatabaseTableColumnPayload
import io.github.yashkasera.alohomora.common.DatabaseTableSchemaPayload
import io.github.yashkasera.alohomora.common.DatabaseTableSnapshot
import io.github.yashkasera.alohomora.data.db.AlohomoraDb

/**
 * iOS database inspection.
 *
 * Previously every method returned an empty list, so the Vault was silently blank on iOS and the
 * desktop client could not distinguish "unimplemented" from "no databases found". Implemented
 * with [BundledSQLiteDriver], which this module already depends on for Room — no cinterop and no
 * new dependency.
 *
 * Mirrors the Android implementation's safety rules exactly: a table name arriving off the wire
 * is checked against the tables we ourselves enumerated (SQLite has no bind parameter for
 * identifiers, and backtick quoting is not escaping), and the row limit is clamped because a
 * negative limit becomes `LIMIT -1`, i.e. an unbounded dump into memory.
 */
internal actual class DevToolsDatabaseInspector actual constructor(
    database: AlohomoraDb,
    private val appDatabaseProvider: DevToolsAppDatabaseProvider,
) {
    actual fun listDatabases(): List<AppDatabaseInfo> = appDatabaseProvider.listDatabases()

    actual fun loadSchema(databaseName: String): DatabaseSchemaSnapshot {
        val path = appDatabaseProvider.resolvePath(databaseName) ?: return emptySchema(databaseName)
        return withConnection(path, { emptySchema(databaseName) }) { connection ->
            val tables = queryTables(connection)
            DatabaseSchemaSnapshot(
                databaseName = databaseName,
                tables = tables,
                schemas = tables.map { table -> queryTableSchema(connection, table) },
            )
        }
    }

    actual fun loadTable(
        databaseName: String,
        tableName: String,
        limit: Int,
    ): DatabaseTableSnapshot {
        val path = appDatabaseProvider.resolvePath(databaseName)
            ?: return emptyTable(databaseName, tableName)

        return withConnection(path, { emptyTable(databaseName, tableName) }) { connection ->
            if (tableName !in queryTables(connection)) {
                println("[Alohomora] Rejecting DevTools request for unknown table '$tableName'")
                return@withConnection emptyTable(databaseName, tableName)
            }
            val safeLimit = limit.coerceIn(1, MAX_ROW_LIMIT)

            connection.prepare("SELECT * FROM `$tableName` LIMIT $safeLimit").use { statement ->
                val columns =
                    (0 until statement.getColumnCount()).map { statement.getColumnName(it) }
                val rows = mutableListOf<Map<String, String?>>()
                while (statement.step()) {
                    val row = LinkedHashMap<String, String?>(columns.size)
                    columns.forEachIndexed { index, column ->
                        row[column] = when (statement.getColumnType(index)) {
                            SQLITE_DATA_NULL -> null
                            SQLITE_DATA_INTEGER -> statement.getLong(index).toString()
                            SQLITE_DATA_FLOAT -> statement.getDouble(index).toString()
                            SQLITE_DATA_TEXT -> statement.getText(index)
                            // Reported as a size, matching Android. Streaming raw blob bytes
                            // would inflate the frame for no diagnostic value.
                            SQLITE_DATA_BLOB -> "BLOB(${statement.getBlob(index).size})"
                            else -> null
                        }
                    }
                    rows.add(row)
                }
                DatabaseTableSnapshot(
                    databaseName = databaseName,
                    name = tableName,
                    columns = columns,
                    rows = rows,
                )
            }
        }
    }

    actual fun updateCell(
        databaseName: String,
        tableName: String,
        primaryKeys: Map<String, String>,
        columnName: String,
        newValue: String?,
    ): Boolean {
        if (primaryKeys.isEmpty()) return false
        val path = appDatabaseProvider.resolvePath(databaseName) ?: return false
        return withConnection(path, { false }) { connection ->
            if (tableName !in queryTables(connection)) return@withConnection false
            val setCols = "`$columnName` = ?"
            val whereClause = primaryKeys.keys.joinToString(" AND ") { "`$it` = ?" }
            val sql = "UPDATE `$tableName` SET $setCols WHERE $whereClause"
            connection.prepare(sql).use { statement ->
                if (newValue == null) {
                    statement.bindNull(1)
                } else {
                    statement.bindText(1, newValue)
                }
                primaryKeys.values.forEachIndexed { index, value ->
                    statement.bindText(index + 2, value)
                }
                statement.step()
            }
            true
        }
    }

    /**
     * Opens [path], runs [block], and always closes the connection.
     *
     * Returns [onFailure] rather than throwing: this runs on the DevTools reader coroutine, so a
     * corrupt or locked database must degrade to an empty view instead of killing the session.
     */
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
            println("[Alohomora] Failed to inspect database at $path: ${e.message}")
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
            buildList {
                while (statement.step()) add(statement.getText(0))
            }
        }

    private fun queryTableSchema(
        connection: SQLiteConnection,
        tableName: String,
    ): DatabaseTableSchemaPayload {
        val columns = mutableListOf<DatabaseTableColumnPayload>()
        var primaryKey: String? = null

        // PRAGMA table_info columns: cid, name, type, notnull, dflt_value, pk
        connection.prepare("PRAGMA table_info(`$tableName`)").use { statement ->
            while (statement.step()) {
                val name = statement.getText(1)
                val isPrimaryKey = statement.getLong(5) > 0
                if (isPrimaryKey) primaryKey = name
                columns += DatabaseTableColumnPayload(
                    name = name,
                    type = statement.getText(2),
                    notNull = statement.getLong(3) == 1L,
                    primaryKey = isPrimaryKey,
                    defaultValue = if (statement.isNull(4)) null else statement.getText(4),
                )
            }
        }

        // PRAGMA index_list columns: seq, name, unique, origin, partial
        val indexes = connection.prepare("PRAGMA index_list(`$tableName`)").use { statement ->
            buildList {
                while (statement.step()) add(statement.getText(1))
            }
        }

        return DatabaseTableSchemaPayload(
            name = tableName,
            columns = columns,
            primaryKey = primaryKey,
            indexes = indexes,
        )
    }

    private fun emptySchema(databaseName: String) = DatabaseSchemaSnapshot(
        databaseName = databaseName,
        tables = emptyList(),
        schemas = emptyList(),
    )

    private fun emptyTable(databaseName: String, tableName: String) = DatabaseTableSnapshot(
        databaseName = databaseName,
        name = tableName,
        columns = emptyList(),
        rows = emptyList(),
    )

    private companion object {
        const val MAX_ROW_LIMIT = 1000
    }
}
