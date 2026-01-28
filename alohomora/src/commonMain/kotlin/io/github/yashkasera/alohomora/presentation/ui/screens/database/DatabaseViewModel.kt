package io.github.yashkasera.alohomora.presentation.ui.screens.database

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DatabaseInfo(
    val name: String,
    val path: String
)

data class TableColumn(
    val name: String,
    val type: String
)

data class TableData(
    val columns: List<TableColumn>,
    val rows: List<Map<String, String>>
)

data class TableSchema(
    val name: String,
    val columns: List<TableColumn>,
    val primaryKey: String?,
    val indexes: List<String>
)

data class DatabaseState(
    val databases: List<DatabaseInfo> = emptyList(),
    val selectedDatabase: DatabaseInfo? = null,
    val tables: List<String> = emptyList(),
    val selectedTable: String? = null,
    val currentTab: Int = 1, // 0: Browse, 1: Query, 2: Schema
    val queryText: String = "",
    val queryResults: TableData? = null,
    val queryStatus: QueryStatus? = null,
    val tableData: TableData? = null,
    val tableSchema: TableSchema? = null,
    val showDatabaseSelector: Boolean = false
)

data class QueryStatus(
    val success: Boolean,
    val executionTimeMs: Long,
    val rowsAffected: Int = 0
)

internal class DatabaseViewModel : ViewModel() {
    private val _state = MutableStateFlow(DatabaseState())
    val state: StateFlow<DatabaseState> = _state.asStateFlow()

    init {
        loadMockData()
    }

    private fun loadMockData() {
        val mockDatabases = listOf(
            DatabaseInfo("app_data.db", "/data/data/com.example.app/databases/app_data.db"),
            DatabaseInfo("cache.db", "/data/data/com.example.app/databases/cache.db"),
            DatabaseInfo("user_prefs.db", "/data/data/com.example.app/databases/user_prefs.db")
        )

        val mockTables = listOf("users", "orders", "products", "analytics", "logs")

        val mockColumns = listOf(
            TableColumn("id", "INTEGER"),
            TableColumn("email", "TEXT"),
            TableColumn("status", "TEXT"),
            TableColumn("last_login", "TEXT")
        )

        val mockRows = listOf(
            mapOf(
                "id" to "8492",
                "email" to "alex.morgan@dev.co",
                "status" to "active",
                "last_login" to "2023-11-02"
            ),
            mapOf(
                "id" to "8491",
                "email" to "sarah.j@studio.io",
                "status" to "active",
                "last_login" to "2023-11-02"
            ),
            mapOf(
                "id" to "8488",
                "email" to "m.ross@pearson.com",
                "status" to "active",
                "last_login" to "2023-11-01"
            ),
            mapOf(
                "id" to "8485",
                "email" to "donna.p@legal.ny",
                "status" to "active",
                "last_login" to "2023-11-01"
            ),
            mapOf(
                "id" to "8482",
                "email" to "louis.l@litt.com",
                "status" to "active",
                "last_login" to "2023-10-31"
            )
        )

        val mockQueryData = TableData(columns = mockColumns, rows = mockRows)

        val mockSchema = TableSchema(
            name = "users",
            columns = mockColumns + listOf(
                TableColumn("created_at", "TIMESTAMP"),
                TableColumn("updated_at", "TIMESTAMP")
            ),
            primaryKey = "id",
            indexes = listOf("idx_email", "idx_status", "idx_last_login")
        )

        _state.value = _state.value.copy(
            databases = mockDatabases,
            selectedDatabase = mockDatabases[0],
            tables = mockTables,
            selectedTable = "users",
            queryText = "SELECT id, email, status, last_login\nFROM users\nWHERE status = 'active'\nORDER BY last_login DESC\nLIMIT 5;",
            queryResults = mockQueryData,
            queryStatus = QueryStatus(success = true, executionTimeMs = 12, rowsAffected = 5),
            tableData = mockQueryData,
            tableSchema = mockSchema
        )
    }

    fun selectDatabase(database: DatabaseInfo) {
        _state.value = _state.value.copy(
            selectedDatabase = database,
            showDatabaseSelector = false
        )
    }

    fun selectTable(table: String) {
        _state.value = _state.value.copy(selectedTable = table)
    }

    fun selectTab(tabIndex: Int) {
        _state.value = _state.value.copy(currentTab = tabIndex)
    }

    fun updateQueryText(text: String) {
        _state.value = _state.value.copy(queryText = text)
    }

    fun executeQuery() {
        // Mock query execution
        _state.value = _state.value.copy(
            queryStatus = QueryStatus(success = true, executionTimeMs = 12, rowsAffected = 5)
        )
    }

    fun toggleDatabaseSelector(show: Boolean) {
        _state.value = _state.value.copy(showDatabaseSelector = show)
    }
}
