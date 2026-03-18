package io.github.yashkasera.alohomora.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class DevToolsMessage {
    abstract val sequence: Long
}

// ── Server → Client ──────────────────────────────────────────────────────────

@Serializable
@SerialName("AUTH_CHALLENGE")
data class AuthChallengeMessage(override val sequence: Long) : DevToolsMessage()

@Serializable
@SerialName("AUTH_SUCCESS")
data class AuthSuccessMessage(override val sequence: Long) : DevToolsMessage()

@Serializable
@SerialName("AUTH_FAILURE")
data class AuthFailureMessage(
    override val sequence: Long,
    val reason: String,
) : DevToolsMessage()

@Serializable
@SerialName("INITIAL_STATE")
data class InitialStateMessage(
    override val sequence: Long,
    val payload: InitialStatePayload,
) : DevToolsMessage()

@Serializable
@SerialName("STREAM_EVENT")
data class StreamEventMessage(
    override val sequence: Long,
    val event: TelemetryEvent,
) : DevToolsMessage()

@Serializable
@SerialName("STREAM_API_LOG")
data class StreamApiLogMessage(
    override val sequence: Long,
    val log: TraceEntry,
) : DevToolsMessage()

@Serializable
@SerialName("SNAPSHOT_DATABASE")
data class DatabaseSnapshotMessage(
    override val sequence: Long,
    val payload: DatabaseSnapshotPayload,
) : DevToolsMessage()

@Serializable
@SerialName("SNAPSHOT_PREFS")
data class PrefsSnapshotMessage(
    override val sequence: Long,
    val payload: PrefsSnapshotPayload,
) : DevToolsMessage()

// ── Client → Server ──────────────────────────────────────────────────────────

@Serializable
@SerialName("AUTH_RESPONSE")
data class AuthResponseMessage(
    override val sequence: Long = 0,
    val otp: String,
) : DevToolsMessage()

@Serializable
@SerialName("REQUEST_INITIAL_STATE")
data class RequestInitialStateMessage(
    override val sequence: Long = 0,
) : DevToolsMessage()

@Serializable
@SerialName("REQUEST_DATABASE_SCHEMA")
data class RequestDatabaseSchemaMessage(
    override val sequence: Long = 0,
    val databaseName: String,
) : DevToolsMessage()

@Serializable
@SerialName("REQUEST_DATABASE_TABLE")
data class RequestDatabaseTableMessage(
    override val sequence: Long = 0,
    val databaseName: String? = null,
    val tableName: String,
    val limit: Int = 200,
) : DevToolsMessage()

@Serializable
@SerialName("REQUEST_PREF_VALUE")
data class RequestPrefValueMessage(
    override val sequence: Long = 0,
    val key: String,
) : DevToolsMessage()

// ── Payload types (kept as named wrappers for complex payloads) ───────────────

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
data class BuildInfoPayload(
    val projectName: String,
    val packageName: String? = null,
    val versionName: String,
    val versionCode: Int,
    val variantName: String,
    val flavorName: String? = null,
    val buildType: String? = null,
    val branch: String,
    val commitSha: String,
    val isDirty: Boolean,
    val buildTimestampUtc: Long,
    val slackWebhookUrl: String? = null,
)

@Serializable
data class ChronicleCommitPayload(
    val sha: String,
    val author: String,
    val message: String,
    val timestamp: Long,
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
    val buildInfo: BuildInfoPayload? = null,
    val chronicle: List<ChronicleCommitPayload> = emptyList(),
)
