package io.github.yashkasera.alohomora.common

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
enum class DevToolsMessageType {
    STREAM_EVENT,
    STREAM_API_LOG,
    SNAPSHOT_DATABASE,
    SNAPSHOT_PREFS,
    REQUEST_INITIAL_STATE,
    REQUEST_DATABASE_SCHEMA,
    REQUEST_DATABASE_TABLE,
    REQUEST_PREF_VALUE,
}

@Serializable
data class DevToolsEnvelope(
    val type: DevToolsMessageType,
    val sequence: Long,
    val payload: JsonElement? = null,
)

@Serializable
data class AppDatabaseInfo(
    val name: String,
    val path: String,
)

@Serializable
data class DatabaseTableColumnPayload(
    val name: String,
    val type: String,
    val notNull: Boolean,
    val primaryKey: Boolean,
    val defaultValue: String? = null,
)

@Serializable
data class DatabaseTableSchemaPayload(
    val name: String,
    val columns: List<DatabaseTableColumnPayload>,
    val primaryKey: String?,
    val indexes: List<String>,
)

@Serializable
data class DatabaseSchemaSnapshot(
    val databaseName: String? = null,
    val tables: List<String>,
    val schemas: List<DatabaseTableSchemaPayload>,
)

@Serializable
data class DatabaseTableSnapshot(
    val databaseName: String? = null,
    val name: String,
    val columns: List<String>,
    val rows: List<Map<String, String?>>,
)

@Serializable
data class DatabaseSnapshotPayload(
    val databaseName: String? = null,
    val schema: DatabaseSchemaSnapshot? = null,
    val table: DatabaseTableSnapshot? = null,
)

@Serializable
data class PrefsSnapshotPayload(
    val keys: List<String> = emptyList(),
    val values: Map<String, String?> = emptyMap(),
)

@Serializable
data class InitialStatePayload(
    val events: List<TelemetryEvent>,
    val apiLogs: List<TraceEntry>,
    val databaseSchema: DatabaseSchemaSnapshot,
    val databases: List<AppDatabaseInfo> = emptyList(),
    val selectedDatabase: String? = null,
    val preferenceKeys: List<String>,
)

@Serializable
data class RequestDatabaseSchemaPayload(
    val databaseName: String,
)

@Serializable
data class RequestDatabaseTablePayload(
    val databaseName: String? = null,
    val tableName: String,
    val limit: Int = 200,
)

@Serializable
data class RequestPrefValuePayload(
    val key: String,
)
