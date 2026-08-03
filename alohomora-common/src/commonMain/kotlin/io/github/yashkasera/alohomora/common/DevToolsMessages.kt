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
    val event: Event,
) : DevToolsMessage()

@Serializable
@SerialName("STREAM_TRAFFIC")
data class StreamTrafficMessage(
    override val sequence: Long,
    val traffic: TrafficEntry,
) : DevToolsMessage()

/**
 * A recorded [Error] — a crash or a reported non-fatal — pushed as it happens.
 *
 * Distinct from [DeviceErrorMessage], which reports that a *desktop command* failed on the device.
 * The names are close enough to be worth stating outright: nothing here is a transport problem,
 * this is the app's own failure.
 */
@Serializable
@SerialName("STREAM_ERROR")
data class StreamErrorMessage(
    override val sequence: Long,
    val error: Error,
) : DevToolsMessage()

@Serializable
@SerialName("SNAPSHOT_DATABASE")
data class DatabaseSnapshotMessage(
    override val sequence: Long,
    val payload: DatabaseSnapshotPayload,
) : DevToolsMessage()

@Serializable
@SerialName("SNAPSHOT_CACHE")
data class CacheSnapshotMessage(
    override val sequence: Long,
    val payload: CacheSnapshotPayload,
) : DevToolsMessage()

/**
 * Liveness probe. The client must answer with a [PongMessage].
 *
 * Sent by the device, not the client, because the device is the side that can be wedged by a
 * peer that dies without a FIN: it allows one connection at a time and rejects every later
 * client while the dead one is still attached, so the slot is unrecoverable short of restarting
 * the app. Through an `adb forward` this is not hypothetical — the device's socket is to the
 * on-device adb daemon and stays healthy after the host-side process is gone, so nothing at the
 * TCP layer reveals the loss.
 *
 * Sent only to a client that set [AuthResponseMessage.heartbeatSupported]. A client that
 * predates PING would decode this as [UnknownMessage] and correctly ignore it, which reads
 * identically to a dead peer.
 */
@Serializable
@SerialName("PING")
data class PingMessage(override val sequence: Long) : DevToolsMessage()

/**
 * Answers a [PingMessage], echoing its sequence.
 *
 * Carries no payload and needs no handler beyond being received: the reply's arrival *is* the
 * information. The sequence is echoed for log correlation, not matching — the device tracks how
 * long its peer has been silent, not which individual ping went unanswered.
 */
@Serializable
@SerialName("PONG")
data class PongMessage(override val sequence: Long = 0) : DevToolsMessage()

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
    /**
     * Whether this client answers [PingMessage] with [PongMessage].
     *
     * Defaults to false so the device never pings a client that predates the heartbeat: such a
     * client ignores the ping as an unknown type, which the device cannot tell from a dead peer
     * and would reap on the session's first idle stretch. Declared here rather than in a
     * post-auth message because the slot is held from the moment the socket is accepted — a
     * client that goes silent while its OTP is still on screen wedges the device just as
     * effectively as an authenticated one.
     */
    val heartbeatSupported: Boolean = false,
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
    val errors: Boolean = false,
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
@SerialName("REQUEST_CACHE_VALUE")
data class RequestCacheValueMessage(
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
 * snapshot or is unobservable, but a replay can fail before any traffic entry exists — no handler
 * registered, a hand-edited URL that will not parse, a refused connection — and with no reply the
 * desktop would sit waiting for traffic that is never coming.
 *
 * [traceId] is set only when the handler could identify the resulting traffic; the replay is still
 * successful without it, since the traffic arrives over `STREAM_TRAFFIC` either way.
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

/**
 * Reports that the device failed to service a desktop command.
 *
 * Exists because the alternative is silence. A handler that threw used to escape the device's
 * reader loop and close the socket, so a database the app could not read surfaced on the desktop
 * as an unexplained disconnect/reconnect cycle with the real cause only in `logcat`/Xcode. The
 * reader now survives a failed handler, which makes this the only way the desktop learns that the
 * answer it is waiting for is never coming.
 *
 * Advisory, not terminal: the connection stays up and other commands keep working.
 *
 * @param request the [SerialName] of the command that failed, so the console can say what broke.
 */
@Serializable
@SerialName("DEVICE_ERROR")
data class DeviceErrorMessage(
    override val sequence: Long,
    val request: String,
    val message: String,
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
data class BuildMetadataPayload(
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
data class GitHistoryPayload(
    val sha: String,
    val author: String,
    val message: String,
    val timestamp: Long,
)

@Serializable
data class CacheSnapshotPayload(
    val keys: List<String> = emptyList(),
    val values: Map<String, String?> = emptyMap(),
)

@Serializable
data class InitialStatePayload(
    val events: List<Event>,
    val traffic: List<TrafficEntry>,
    /**
     * Defaults to empty so a newer desktop talking to an app that predates error capture renders an
     * empty Errors panel instead of failing to decode the entire snapshot.
     */
    val errors: List<Error> = emptyList(),
    val databaseSchema: DatabaseSchemaSnapshot,
    val databases: List<AppDatabaseInfo> = emptyList(),
    val selectedDatabase: String? = null,
    val cacheKeys: List<String>,
    val buildMetadata: BuildMetadataPayload? = null,
    val gitHistory: List<GitHistoryPayload> = emptyList(),
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
