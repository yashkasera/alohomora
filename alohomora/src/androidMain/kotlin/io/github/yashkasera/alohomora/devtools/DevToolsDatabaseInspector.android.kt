package io.github.yashkasera.alohomora.devtools

import android.database.sqlite.SQLiteDatabase
import io.github.yashkasera.alohomora.common.AppDatabaseInfo
import io.github.yashkasera.alohomora.common.DatabaseSchemaSnapshot
import io.github.yashkasera.alohomora.common.DatabaseTableColumnPayload
import io.github.yashkasera.alohomora.common.DatabaseTableSchemaPayload
import io.github.yashkasera.alohomora.common.DatabaseTableSnapshot
import io.github.yashkasera.alohomora.data.db.AlohomoraDb

internal actual class DevToolsDatabaseInspector actual constructor(
    database: AlohomoraDb,
    private val appDatabaseProvider: DevToolsAppDatabaseProvider,
) {
    actual fun listDatabases(): List<AppDatabaseInfo> {
        return appDatabaseProvider.listDatabases()
    }

    actual fun loadSchema(databaseName: String): DatabaseSchemaSnapshot {
        val path = appDatabaseProvider.resolvePath(databaseName) ?: return emptySchema(databaseName)
        val db = openDatabase(path) ?: return emptySchema(databaseName)
        return try {
            val tables = queryTables(db)
            val schemas = tables.map { table -> queryTableSchema(db, table) }
            DatabaseSchemaSnapshot(
                databaseName = databaseName,
                tables = tables,
                schemas = schemas,
            )
        } catch (e: Exception) {
            emptySchema(databaseName)
        } finally {
            db.close()
        }
    }

    actual fun loadTable(databaseName: String, tableName: String, limit: Int): DatabaseTableSnapshot {
        val path = appDatabaseProvider.resolvePath(databaseName) ?: return emptyTable(databaseName, tableName)
        val db = openDatabase(path) ?: return emptyTable(databaseName, tableName)
        return try {
            // tableName arrives straight off the socket. SQLite has no bind parameter for
            // identifiers and backtick-quoting is not escaping, so the only safe check is an
            // allowlist against the tables we ourselves enumerated. This also blocks reading
            // sqlite_master / android_* directly, which queryTables deliberately filters out.
            if (tableName !in queryTables(db)) {
                println("[Alohomora] Rejecting DevTools request for unknown table '$tableName'")
                return emptyTable(databaseName, tableName)
            }
            // Clamped because a negative limit becomes `LIMIT -1`, i.e. unbounded — a
            // peer-triggered full-table dump into memory.
            val safeLimit = limit.coerceIn(1, MAX_ROW_LIMIT)
            val cursor = db.rawQuery("SELECT * FROM `$tableName` LIMIT $safeLimit", null)
            val columns = (0 until cursor.columnCount).map { index -> cursor.getColumnName(index) }
            val rows = mutableListOf<Map<String, String?>>()
            while (cursor.moveToNext()) {
                val row = mutableMapOf<String, String?>()
                for (index in columns.indices) {
                    val columnName = columns[index]
                    val value = when (cursor.getType(index)) {
                        0 -> null
                        1 -> cursor.getLong(index).toString()
                        2 -> cursor.getDouble(index).toString()
                        3 -> cursor.getString(index)
                        4 -> {
                            val blob = cursor.getBlob(index)
                            if (blob == null) null else "BLOB(${blob.size})"
                        }
                        else -> null
                    }
                    row[columnName] = value
                }
                rows.add(row)
            }
            cursor.close()
            DatabaseTableSnapshot(
                databaseName = databaseName,
                name = tableName,
                columns = columns,
                rows = rows,
            )
        } catch (e: Exception) {
            emptyTable(databaseName, tableName)
        } finally {
            db.close()
        }
    }

    private fun openDatabase(path: String): SQLiteDatabase? {
        return try {
            SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY)
        } catch (e: Exception) {
            null
        }
    }

    private fun queryTables(db: SQLiteDatabase): List<String> {
        val cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' " +
                "AND name NOT LIKE 'android_%' " +
                "AND name NOT LIKE 'sqlite_%' " +
                "AND name != 'room_master_table'",
            null
        )
        val tables = mutableListOf<String>()
        while (cursor.moveToNext()) {
            val name = cursor.getString(0)
            tables.add(name)
        }
        cursor.close()
        return tables
    }

    private fun queryTableSchema(db: SQLiteDatabase, tableName: String): DatabaseTableSchemaPayload {
        val columnsCursor = db.rawQuery("PRAGMA table_info(`$tableName`)", null)
        val columns = mutableListOf<DatabaseTableColumnPayload>()
        var primaryKey: String? = null
        val nameIndex = columnsCursor.getColumnIndex("name")
        val typeIndex = columnsCursor.getColumnIndex("type")
        val notNullIndex = columnsCursor.getColumnIndex("notnull")
        val pkIndex = columnsCursor.getColumnIndex("pk")
        val defaultIndex = columnsCursor.getColumnIndex("dflt_value")
        while (columnsCursor.moveToNext()) {
            if (nameIndex == -1 || typeIndex == -1 || notNullIndex == -1 || pkIndex == -1) continue
            val name = columnsCursor.getString(nameIndex)
            val type = columnsCursor.getString(typeIndex)
            val notNull = columnsCursor.getInt(notNullIndex) == 1
            val pk = columnsCursor.getInt(pkIndex) == 1
            val defaultValue = if (defaultIndex == -1) null else columnsCursor.getString(defaultIndex)
            if (pk) primaryKey = name
            columns.add(
                DatabaseTableColumnPayload(
                    name = name,
                    type = type,
                    notNull = notNull,
                    primaryKey = pk,
                    defaultValue = defaultValue,
                )
            )
        }
        columnsCursor.close()

        val indexCursor = db.rawQuery("PRAGMA index_list(`$tableName`)", null)
        val indexes = mutableListOf<String>()
        val indexNameIndex = indexCursor.getColumnIndex("name")
        while (indexCursor.moveToNext()) {
            if (indexNameIndex == -1) continue
            val name = indexCursor.getString(indexNameIndex)
            indexes.add(name)
        }
        indexCursor.close()

        return DatabaseTableSchemaPayload(
            name = tableName,
            columns = columns,
            primaryKey = primaryKey,
            indexes = indexes,
        )
    }

    private fun emptySchema(databaseName: String): DatabaseSchemaSnapshot {
        return DatabaseSchemaSnapshot(
            databaseName = databaseName,
            tables = emptyList(),
            schemas = emptyList(),
        )
    }

    private fun emptyTable(databaseName: String, tableName: String): DatabaseTableSnapshot {
        return DatabaseTableSnapshot(
            databaseName = databaseName,
            name = tableName,
            columns = emptyList(),
            rows = emptyList(),
        )
    }

    private companion object {
        const val MAX_ROW_LIMIT = 1000
    }
}
