package io.github.yashkasera.alohomora.common

import io.github.yashkasera.alohomora.replay.ReplayRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class DevToolsMessage {
    abstract val sequence: Long
}

/**
 * Sentinel for a message type this build does not recognise.
 *
 * The sealed hierarchy is closed, so kotlinx.serialization would otherwise throw on any
 * unseen `@SerialName` — meaning a newer desktop client talking to an older app killed the
 * read loop instead of being politely ignored. Registered as the polymorphic default in
 * [DevToolsProtocol.json]; handlers should skip it.
 */
@Serializable
@SerialName("UNKNOWN")
data class UnknownMessage(override val sequence: Long = 0) : DevToolsMessage()

// ── Server → Client ──────────────────────────────────────────────────────────

@Serializable
@SerialName("AUTH_CHALLENGE")
data class AuthChallengeMessage(override val sequence: Long) : DevToolsMessage()

@Serializable
@SerialName("AUTH_SUCCESS")
data class AuthSuccessMessage(
    override val sequence: Long,
    /**
     * A trust token the client should persist and present on reconnect, letting a desktop that
     * has already been approved skip the OTP prompt entirely.
     *
     * Null when the client authenticated *with* a token (there is nothing new to hand out) and
     * on older devices that predate trust-on-first-use — hence nullable with a default, so an
     * old client deserialising a new message and vice versa both keep working.
     */
    val token: String? = null,
) : DevToolsMessage()

/**
 * The device has displayed a code and is waiting for the user to type it.
 *
 * Sent only when a client's probe carried no usable trust token. Without it the client cannot
 * tell "the device is still validating my token" from "the device wants a code from the user" —
 * both look like AWAITING_AUTH — so it either prompts for a code that is never needed, or sits
 * silently waiting for one it never asks for.
 */
@Serializable
@SerialName("AUTH_OTP_REQUIRED")
data class AuthOtpRequiredMessage(override val sequence: Long) : DevToolsMessage()

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
    /**
     * The code the user read off the device screen. Empty when authenticating with [token].
     */
    val otp: String = "",
    /**
     * A token issued by this device on a previous successful pairing.
     *
     * Sent instead of an OTP so an already-approved desktop reconnects silently. Null on a first
     * pairing, and from clients that predate trust-on-first-use.
     */
    val token: String? = null,
) : DevToolsMessage()

/**
 * Asks the device to delete its captured traces and/or telemetry.
 *
 * Clearing only the desktop's local copy would look like it worked and then silently repopulate
 * from the device snapshot on the next reconnect, so the delete has to happen at the source.
 */
@Serializable
@SerialName("REQUEST_CLEAR")
data class RequestClearMessage(
    override val sequence: Long = 0,
    val traces: Boolean = false,
    val events: Boolean = false,
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

/**
 * Asks the device to re-send a captured request through the host app's own HTTP client.
 *
 * Carries the whole request rather than just a trace id, because the desktop lets the user edit the
 * URL, headers and payload before sending — the point of the feature is to change a payload and
 * have the app re-sign it, so "replay trace X as captured" would not cover the use case.
 */
@Serializable
@SerialName("REQUEST_REPLAY_TRACE")
data class RequestReplayTraceMessage(
    override val sequence: Long = 0,
    val request: ReplayRequest,
) : DevToolsMessage()

/**
 * Reports the fate of a [RequestReplayTraceMessage].
 *
 * The only desktop→device command with an explicit reply. Every other one either answers with a
 * snapshot or is unobservable, but a replay can fail before any trace exists — no handler
 * registered, a hand-edited URL that will not parse, a refused connection — and with no reply the
 * desktop would sit waiting for a trace that is never coming.
 *
 * [traceId] is set only when the handler could identify the resulting trace; the replay is still
 * successful without it, since the trace arrives over `STREAM_API_LOG` either way.
 */
@Serializable
@SerialName("REPLAY_RESULT")
data class ReplayResultMessage(
    override val sequence: Long,
    val sourceTraceId: String,
    val sent: Boolean,
    val traceId: String? = null,
    val error: String? = null,
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
    /**
     * Whether the app on the other end registered a replay handler.
     *
     * A session capability, not build metadata: replay depends on the host app having handed
     * Alohomora its HTTP client at startup, which a device may simply not have done. Defaults to
     * false so a desktop talking to an older app hides the action rather than offering one whose
     * request would go nowhere.
     */
    val replaySupported: Boolean = false,
)
