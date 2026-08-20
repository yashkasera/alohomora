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

/**
 * One completed [Span], pushed as it is recorded.
 *
 * Per-span rather than per-completed-trace, because a trace has no observable end — it is finished
 * when nobody starts another child, which is only knowable by timeout. Batching would mean either
 * holding spans for seconds or guessing wrong on a long-running root, and "wait for the root" is the
 * same lag by another name (`TraceSummary.rootSpanName`).
 *
 * The consequence the desktop must handle either way: a child can arrive before its parent, so it
 * renders as a provisional root and re-parents itself later. `buildTraceTree` does that regardless,
 * so batching by trace would buy nothing.
 */
@Serializable
@SerialName("STREAM_SPAN")
data class StreamSpanMessage(
    override val sequence: Long,
    val span: Span,
) : DevToolsMessage()

/**
 * Answers [RequestTraceSpansMessage] with every span of one trace, ignoring the snapshot limit.
 */
@Serializable
@SerialName("SNAPSHOT_TRACE_SPANS")
data class TraceSpansSnapshotMessage(
    override val sequence: Long,
    val traceId: String,
    val spans: List<Span>,
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

@Serializable
@SerialName("SNAPSHOT_FEATURE_FLAGS")
data class FeatureFlagsSnapshotMessage(
    override val sequence: Long,
    val flags: List<FeatureFlag>,
) : DevToolsMessage()

/**
 * Reports the current state of device-wide VPN throttling back to the desktop.
 *
 * Pushed whenever the state changes (consent requested, VPN starting, active, error, off).
 * The desktop reflects this in the toolbar so the developer sees what the device is doing.
 */
@Serializable
@SerialName("VPN_STATE")
data class VpnStateMessage(
    override val sequence: Long = 0,
    val state: VpnThrottleState,
    val activeProfile: ThrottleProfile? = null,
    val error: String? = null,
) : DevToolsMessage()

@Serializable
enum class VpnThrottleState { OFF, AWAITING_CONSENT, STARTING, ACTIVE, ERROR }

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

@Serializable
@SerialName("SET_THROTTLE_PROFILE")
data class SetThrottleProfileMessage(
    override val sequence: Long = 0,
    val profile: ThrottleProfile,
) : DevToolsMessage()

@Serializable
@SerialName("SET_MOCK_RULES")
data class SetMockRulesMessage(
    override val sequence: Long = 0,
    val rules: List<MockRule>,
) : DevToolsMessage()

/**
 * Asks the device to enable or disable VPN-based device-wide throttling.
 *
 * Reuses [ThrottleProfile] for the rate parameters. When [enabled] is false the VPN is torn down
 * regardless of the profile. When true, [profile] with name "none" means the VPN is active but
 * passes traffic unthrottled — useful for verifying the tunnel works before introducing latency.
 */
@Serializable
@SerialName("SET_VPN_THROTTLE")
data class SetVpnThrottleMessage(
    override val sequence: Long = 0,
    val profile: ThrottleProfile,
    val enabled: Boolean,
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
    /**
     * Clears captured spans.
     *
     * Named `spans`, **not** `traces` — [traces] above already means *traffic* on this wire (see
     * `DevToolsRuntime.handleClear`, which routes it to the traffic table), a misnomer that predates
     * the vocabulary rule and is kept for interop. Reusing it for OpenTelemetry-style traces would
     * silently wipe the wrong table.
     */
    val spans: Boolean = false,
) : DevToolsMessage()

/**
 * Asks the device for every span of one trace.
 *
 * Needed because the snapshot truncates by rowid descending while a trace's spans are spread across
 * rowids by *end* order: the root survives truncation — it ends last, so it has the highest rowid —
 * while the earliest-finishing leaves are cut. A large trace would therefore render as a waterfall
 * that looks complete and is not, which is worse than one that admits it is partial.
 *
 * Gate on [InitialStatePayload.spanCaptureSupported]: an app predating span capture decodes this as
 * [UnknownMessage] and never replies, so an ungated request waits forever.
 */
@Serializable
@SerialName("REQUEST_TRACE_SPANS")
data class RequestTraceSpansMessage(
    override val sequence: Long = 0,
    val traceId: String,
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
@SerialName("REQUEST_DATABASE_UPDATE")
data class RequestDatabaseUpdateMessage(
    override val sequence: Long = 0,
    val databaseName: String,
    val tableName: String,
    val primaryKeys: Map<String, String>,
    val columnName: String,
    val newValue: String?,
) : DevToolsMessage()

@Serializable
@SerialName("REQUEST_CACHE_VALUE")
data class RequestCacheValueMessage(
    override val sequence: Long = 0,
    val key: String,
) : DevToolsMessage()

@Serializable
@SerialName("REQUEST_CACHE_UPDATE")
data class RequestCacheUpdateMessage(
    override val sequence: Long = 0,
    val storeName: String,
    val key: String,
    val newValue: String?,
    val type: String,
) : DevToolsMessage()

@Serializable
@SerialName("REQUEST_CACHE_DELETE")
data class RequestCacheDeleteMessage(
    override val sequence: Long = 0,
    val storeName: String,
    val key: String,
) : DevToolsMessage()

@Serializable
@SerialName("REQUEST_CACHE_REFRESH")
data class RequestCacheRefreshMessage(
    override val sequence: Long = 0,
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

// ── Custom actions ───────────────────────────────────────────────────────────

@Serializable
data class ActionDescriptor(
    val id: String,
    val label: String,
    val description: String? = null,
    val parameters: List<ActionParameter> = emptyList(),
)

@Serializable
data class ActionParameter(
    val key: String,
    val label: String,
    val type: String = "string",
    val defaultValue: String? = null,
    val options: List<String>? = null,
    val required: Boolean = true,
)

/**
 * Asks the device to execute a consumer-registered action.
 *
 * Actions are registered at startup via `Alohomora.registerAction` and advertised to the desktop
 * in [InitialStatePayload.actions]. An unrecognised [actionId] returns a [CustomActionResultMessage]
 * with `success = false` rather than a [DeviceErrorMessage], because it is a user error (stale UI)
 * not a device fault.
 */
@Serializable
@SerialName("REQUEST_CUSTOM_ACTION")
data class RequestCustomActionMessage(
    override val sequence: Long = 0,
    val actionId: String,
    val params: Map<String, String> = emptyMap(),
) : DevToolsMessage()

/**
 * Reports the outcome of a [RequestCustomActionMessage].
 *
 * Modelled after [ReplayResultMessage]: an explicit reply because the desktop needs to know whether
 * the action succeeded, and without it a failed action is indistinguishable from one that is still
 * running.
 */
@Serializable
@SerialName("CUSTOM_ACTION_RESULT")
data class CustomActionResultMessage(
    override val sequence: Long,
    val actionId: String,
    val success: Boolean,
    val result: Map<String, String> = emptyMap(),
    val error: String? = null,
) : DevToolsMessage()

// ── Plugin data ──────────────────────────────────────────────────────────────

@Serializable
data class PluginDataFieldDescriptor(
    val pluginId: String,
    val key: String,
    val label: String,
    val type: String = "string",
    val value: String,
    val options: List<String>? = null,
    val readOnly: Boolean = false,
)

@Serializable
data class PluginDataSnapshot(
    val pluginId: String,
    val fields: List<PluginDataFieldDescriptor>,
)

@Serializable
@SerialName("REQUEST_PLUGIN_DATA_UPDATE")
data class RequestPluginDataUpdateMessage(
    override val sequence: Long = 0,
    val pluginId: String,
    val key: String,
    val value: String,
) : DevToolsMessage()

@Serializable
@SerialName("PLUGIN_DATA_UPDATE_RESULT")
data class PluginDataUpdateResultMessage(
    override val sequence: Long,
    val pluginId: String,
    val key: String,
    val success: Boolean,
    val error: String? = null,
) : DevToolsMessage()

@Serializable
@SerialName("STREAM_PLUGIN_DATA")
data class StreamPluginDataMessage(
    override val sequence: Long,
    val snapshot: PluginDataSnapshot,
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
    val appName: String? = null,
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
data class CacheEntrySnapshot(
    val key: String,
    val value: String?,
    val type: String,
)

@Serializable
data class CacheStoreSnapshot(
    val name: String,
    val isEncrypted: Boolean = false,
    val entries: List<CacheEntrySnapshot> = emptyList(),
)

@Serializable
data class CacheSnapshotPayload(
    val keys: List<String> = emptyList(),
    val values: Map<String, String?> = emptyMap(),
    val stores: List<CacheStoreSnapshot> = emptyList(),
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
    /**
     * Defaults to empty, as [errors] does, so a newer desktop talking to an app that predates span
     * capture renders an empty Traces panel instead of failing to decode the entire snapshot.
     */
    val spans: List<Span> = emptyList(),
    val databaseSchema: DatabaseSchemaSnapshot,
    val databases: List<AppDatabaseInfo> = emptyList(),
    val selectedDatabase: String? = null,
    val cacheKeys: List<String>,
    val cacheStores: List<CacheStoreSnapshot> = emptyList(),
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
    /**
     * Whether the app on the other end has span capture wired up — something has called
     * `Alohomora.recordSpan` at least once.
     *
     * A session capability rather than build metadata, exactly like [replaySupported]: capture
     * depends on the host app having registered a tracer adapter, which an app may simply not have
     * done. Without it an empty Traces panel is indistinguishable from "no spans yet", and the
     * desktop cannot tell whether [RequestTraceSpansMessage] will ever be answered. Defaults to
     * false so a newer desktop degrades rather than sending into a void.
     */
    val spanCaptureSupported: Boolean = false,
    val networkRulesSupported: Boolean = false,
    /**
     * Whether the app supports VPN-based device-wide throttling.
     *
     * Android-only. Defaults false so a desktop talking to an iOS app or an older Android build
     * hides the toggle rather than sending [SetVpnThrottleMessage] into a void.
     */
    val vpnThrottleSupported: Boolean = false,
    val vpnThrottleState: VpnThrottleState = VpnThrottleState.OFF,
    val vpnThrottleActiveProfile: ThrottleProfile? = null,
    val featureFlags: List<FeatureFlag> = emptyList(),
    val actions: List<ActionDescriptor> = emptyList(),
    val pluginData: List<PluginDataSnapshot> = emptyList(),
)
