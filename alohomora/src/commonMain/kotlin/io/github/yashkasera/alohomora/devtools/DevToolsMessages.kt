package io.github.yashkasera.alohomora.devtools

import io.github.yashkasera.alohomora.data.entity.Analytics
import io.github.yashkasera.alohomora.data.entity.ApiRequest
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
data class EventPayload(
    val id: Long,
    val name: String,
    val properties: JsonElement?,
    val time: Long,
) {
    companion object {
        fun from(entity: Analytics): EventPayload {
            return EventPayload(
                id = entity.id,
                name = entity.name,
                properties = entity.properties,
                time = entity.time,
            )
        }
    }
}

@Serializable
data class ApiLogPayload(
    val id: String,
    val status: Int?,
    val url: String?,
    val message: String?,
    val method: String?,
    val scheme: String?,
    val host: String?,
    val path: String?,
    val query: String?,
    val request: String?,
    val response: String?,
    val time: Long?,
    val duration: Long?,
    val requestHeaders: Map<String, List<String>>?,
    val responseHeaders: Map<String, List<String>>?,
    val curl: String?,
    val size: Long?,
    val isViewed: Boolean,
) {
    companion object {
        fun from(entity: ApiRequest): ApiLogPayload {
            return ApiLogPayload(
                id = entity.id,
                status = entity.status,
                url = entity.url,
                message = entity.message,
                method = entity.method,
                scheme = entity.scheme,
                host = entity.host,
                path = entity.path,
                query = entity.query,
                request = entity.request,
                response = entity.response,
                time = entity.time,
                duration = entity.duration,
                requestHeaders = entity.requestHeaders,
                responseHeaders = entity.responseHeaders,
                curl = entity.curl,
                size = entity.size,
                isViewed = entity.isViewed,
            )
        }
    }
}

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
    val events: List<EventPayload>,
    val apiLogs: List<ApiLogPayload>,
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
