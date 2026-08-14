package io.github.yashkasera.alohomora.desktop.mcp

import io.github.yashkasera.alohomora.common.MockRule
import io.github.yashkasera.alohomora.common.ThrottleProfile
import io.github.yashkasera.alohomora.common.ThrottleProfiles
import io.github.yashkasera.alohomora.desktop.data.adb.DefaultAdbCommandRunner
import io.github.yashkasera.alohomora.desktop.data.local.toMockRule
import io.github.yashkasera.alohomora.desktop.domain.repository.DevToolsRepository
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.NetworkRulesViewModel
import io.github.yashkasera.alohomora.replay.ReplayHeaderText
import io.github.yashkasera.alohomora.replay.replayBlockedReason
import io.github.yashkasera.alohomora.replay.toReplayRequest
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * The MCP write/command tools — registered only when the developer has flipped "Allow write tools" on.
 *
 * Every tool routes through the *same* code path the desktop UI uses, so an agent's change shows up in
 * the console: replay and clear go through [DevToolsRepository]; mocks and throttle go through
 * [NetworkRulesViewModel] (its `_mockRules`/`_throttleProfile` are the UI's source of truth — talking to
 * the repo directly would desync them). Each write gates on the device capability at call time.
 *
 * `clear_captured` is the only irreversible one, so it is confirmed through [McpConfirmationBroker]
 * before it runs. The rest are reversible (a mock/throttle can be cleared, a replay is just the app
 * re-sending a request through its own client).
 */

/** Long enough for the device to run a replay and answer, short enough to fail rather than hang. */
private const val REPLAY_AWAIT_TIMEOUT_MILLIS = 10_000L

internal sealed interface WriteResult {
    data class Ok(val json: JsonElement) : WriteResult
    data class Error(val message: String) : WriteResult
}

fun registerAlohomoraWriteTools(
    server: Server,
    registry: DeviceSessionRegistry,
    confirmation: McpConfirmationBroker,
) {
    server.addTool(
        "replay_traffic",
        "Re-send a captured request through the app's own HTTP client (so its interceptors re-sign an " +
            "edited payload). Pass the traffic id from list_traffic; optionally override method/url/headers/" +
            "body/contentType. The replayed request is captured like any other, tagged replayOf.",
        buildSchema {
            put("deviceId", stringProp("Target device; optional when only one is connected."))
            put("id", stringProp("The traffic entry id to replay, from list_traffic."))
            put("method", stringProp("Override HTTP method (optional)."))
            put("url", stringProp("Override full URL (optional)."))
            put(
                "headers",
                buildJsonObject {
                    put("type", "object")
                    put(
                        "description",
                        "Override request headers (optional). Object of name -> string or array of strings.",
                    )
                },
            )
            put("body", stringProp("Override request body (optional)."))
            put("contentType", stringProp("Override content type (optional)."))
        },
    ) { request ->
        withSession(registry, request) { handle ->
            val id =
                request.str("id") ?: return@withSession errorResult("Missing required argument: id")
            AlohomoraMcpWriteData.replay(
                repo = handle.devToolsRepository,
                id = id,
                method = request.str("method"),
                url = request.str("url"),
                headers = parseHeaders(request.arguments?.get("headers")),
                body = request.str("body"),
                contentType = request.str("contentType"),
            ).toCallResult()
        }
    }

    server.addTool(
        "list_mock_rules",
        "List the mock rules currently active for this device (the same set the Mock Rules sheet shows). " +
            "Read this before set_mock_rules, which replaces the whole set.",
        deviceOnlySchema(),
    ) { request ->
        withSession(registry, request) { handle ->
            AlohomoraMcpWriteData.listMockRules(handle.networkRulesViewModel).toCallResult()
        }
    }

    server.addTool(
        "set_mock_rules",
        "Replace ALL mock rules for this device. Each rule needs at least urlPattern; statusCode defaults " +
            "200, contentType application/json. Read list_mock_rules first and send the full desired set.",
        buildSchema {
            put("deviceId", stringProp("Target device; optional when only one is connected."))
            put(
                "rules",
                buildJsonObject {
                    put("type", "array")
                    put(
                        "description",
                        "The full desired rule set. Objects: {urlPattern, name?, enabled?, isRegex?, method?, statusCode?, responseBody?, contentType?}.",
                    )
                },
            )
        },
    ) { request ->
        withSession(registry, request) { handle ->
            val rules = runCatching { parseMockRules(request.arguments?.get("rules")) }
                .getOrElse { return@withSession errorResult(it.message ?: "Invalid rules") }
            AlohomoraMcpWriteData.setMockRules(handle.networkRulesViewModel, rules).toCallResult()
        }
    }

    server.addTool(
        "clear_mock_rules",
        "Remove all mock rules for this device.",
        deviceOnlySchema(),
    ) { request ->
        withSession(registry, request) { handle ->
            AlohomoraMcpWriteData.clearMockRules(handle.networkRulesViewModel).toCallResult()
        }
    }

    server.addTool(
        "get_throttle",
        "The device's current network throttle profile (latency + download cap).",
        deviceOnlySchema(),
    ) { request ->
        withSession(registry, request) { handle ->
            AlohomoraMcpWriteData.getThrottle(handle.networkRulesViewModel).toCallResult()
        }
    }

    server.addTool(
        "set_throttle",
        "Set the device's network throttle. Pass a preset (none, edge, 3g, fast_3g, slow_wifi) or a custom " +
            "{latencyMs, downloadBytesPerSec}. Use preset 'none' to turn throttling off.",
        buildSchema {
            put("deviceId", stringProp("Target device; optional when only one is connected."))
            put(
                "preset",
                stringProp("One of: none, edge, 3g, fast_3g, slow_wifi. Omit for a custom profile."),
            )
            put("latencyMs", intProp("Custom added latency in ms (used when preset is omitted)."))
            put(
                "downloadBytesPerSec",
                intProp("Custom download cap in bytes/sec (used when preset is omitted)."),
            )
        },
    ) { request ->
        withSession(registry, request) { handle ->
            val profile = runCatching { parseThrottle(request) }
                .getOrElse {
                    return@withSession errorResult(
                        it.message ?: "Invalid throttle profile",
                    )
                }
            AlohomoraMcpWriteData.setThrottle(handle.networkRulesViewModel, profile).toCallResult()
        }
    }

    server.addTool(
        "clear_captured",
        "Delete captured data on the device AND locally. Irreversible — requires the developer to confirm " +
            "in the desktop app. Select any of traffic/events/errors/spans (traffic = network requests, " +
            "spans = distributed-trace spans).",
        buildSchema {
            put("deviceId", stringProp("Target device; optional when only one is connected."))
            put("traffic", boolProp("Clear captured network requests."))
            put("events", boolProp("Clear captured events."))
            put("errors", boolProp("Clear captured errors."))
            put("spans", boolProp("Clear captured distributed-trace spans."))
        },
    ) { request ->
        withSession(registry, request) { handle ->
            AlohomoraMcpWriteData.clearCaptured(
                repo = handle.devToolsRepository,
                confirmation = confirmation,
                deviceId = handle.deviceId,
                traffic = request.bool("traffic") ?: false,
                events = request.bool("events") ?: false,
                errors = request.bool("errors") ?: false,
                spans = request.bool("spans") ?: false,
            ).toCallResult()
        }
    }

    server.addTool(
        "create_mock_from_traffic",
        "Create a mock rule from a captured traffic entry's response. Matches the request's path and " +
            "method, returns its status code and body. The rule is added to the current set and sent " +
            "to the device immediately.",
        buildSchema {
            put("deviceId", stringProp("Target device; optional when only one is connected."))
            put("id", stringProp("The traffic entry id, from list_traffic or search_traffic."))
            put("name", stringProp("Optional human-readable name for the rule."))
        },
    ) { request ->
        withSession(registry, request) { handle ->
            val id =
                request.str("id") ?: return@withSession errorResult("Missing required argument: id")
            AlohomoraMcpWriteData.createMockFromTraffic(
                repo = handle.devToolsRepository,
                vm = handle.networkRulesViewModel,
                id = id,
                name = request.str("name"),
            ).toCallResult()
        }
    }

    server.addTool(
        "run_adb_command",
        "Run an ADB shell command on the connected device. Allowed: shell am/pm/dumpsys/settings/" +
            "getprop/svc/input/screencap/monkey/wm/cmd, logcat. Not allowed: install, uninstall, " +
            "push, pull, reboot, root, shell rm/su. Pass the command without the 'adb' or " +
            "'-s <serial>' prefix.",
        buildSchema {
            put("deviceId", stringProp("Target device; optional when only one is connected."))
            put(
                "command",
                stringProp("The ADB command without 'adb' prefix. Example: 'shell am force-stop com.example.app'."),
            )
        },
    ) { request ->
        withSession(registry, request) { handle ->
            val command = request.str("command")
                ?: return@withSession errorResult("Missing required argument: command")
            AlohomoraMcpWriteData.runAdbCommand(
                deviceId = handle.deviceId,
                command = command,
            ).toCallResult()
        }
    }
}

/**
 * Pure write actions over a repository / view model, so they are unit-testable with a fake repo and a
 * real (or fake) view model. Errors are values, not thrown, so the adapter can render them.
 */
internal object AlohomoraMcpWriteData {

    suspend fun replay(
        repo: DevToolsRepository,
        id: String,
        method: String?,
        url: String?,
        headers: Map<String, List<String>>?,
        body: String?,
        contentType: String?,
    ): WriteResult {
        if (!repo.replayState.value.supported) {
            return WriteResult.Error("Replay is not available: the app has not registered a replay handler.")
        }
        val entry = repo.traffic.value.firstOrNull { it.id == id }
            ?: return WriteResult.Error("No traffic entry with id $id")
        entry.replayBlockedReason()?.let {
            return WriteResult.Error("This request cannot be replayed: ${it.name}")
        }
        val base = entry.toReplayRequest()
            ?: return WriteResult.Error("This request cannot be replayed.")
        val request = base.copy(
            method = method?.trim()?.uppercase() ?: base.method,
            url = url?.trim() ?: base.url,
            headers = headers ?: base.headers,
            body = body ?: base.body,
            contentType = contentType ?: base.contentType,
        )

        repo.replayTraffic(request)

        // replayTraffic marks the source in-flight synchronously; wait for the device's result.
        val settled = withTimeoutOrNull(REPLAY_AWAIT_TIMEOUT_MILLIS) {
            repo.replayState.first { !it.isInFlight(id) }
        }
            ?: return WriteResult.Error("Replay sent, but the device did not report a result in time.")

        settled.errorFor(id)?.let { return WriteResult.Error("Replay failed: $it") }

        val replayed = repo.traffic.value.filter { it.replayOf == id }.maxByOrNull { it.time ?: 0L }
        return WriteResult.Ok(
            buildJsonObject {
                put("replayed", true)
                put("sourceId", id)
                if (replayed != null) {
                    put("resultId", replayed.id)
                    put("status", replayed.status)
                    put("summary", replayed.summary())
                } else {
                    put("note", "Sent; the replayed entry has not appeared in traffic yet.")
                }
            },
        )
    }

    fun listMockRules(vm: NetworkRulesViewModel): WriteResult {
        if (!vm.networkRulesSupported.value) return unsupportedMocks()
        return WriteResult.Ok(
            json.encodeToJsonElement(
                ListSerializer(MockRule.serializer()),
                vm.mockRules.value,
            ),
        )
    }

    fun setMockRules(vm: NetworkRulesViewModel, rules: List<MockRule>): WriteResult {
        if (!vm.networkRulesSupported.value) return unsupportedMocks()
        vm.replaceRules(rules)
        return WriteResult.Ok(
            buildJsonObject {
                put("applied", true)
                put("count", vm.mockRules.value.size)
            },
        )
    }

    fun clearMockRules(vm: NetworkRulesViewModel): WriteResult {
        if (!vm.networkRulesSupported.value) return unsupportedMocks()
        vm.replaceRules(emptyList())
        return WriteResult.Ok(buildJsonObject { put("cleared", true) })
    }

    fun getThrottle(vm: NetworkRulesViewModel): WriteResult =
        WriteResult.Ok(
            json.encodeToJsonElement(
                ThrottleProfile.serializer(),
                vm.throttleProfile.value,
            ),
        )

    fun setThrottle(vm: NetworkRulesViewModel, profile: ThrottleProfile): WriteResult {
        if (!vm.networkRulesSupported.value) {
            return WriteResult.Error("Network throttling is not available for this device.")
        }
        vm.selectProfile(profile)
        return WriteResult.Ok(json.encodeToJsonElement(ThrottleProfile.serializer(), profile))
    }

    suspend fun clearCaptured(
        repo: DevToolsRepository,
        confirmation: McpConfirmationBroker,
        deviceId: String,
        traffic: Boolean,
        events: Boolean,
        errors: Boolean,
        spans: Boolean,
    ): WriteResult {
        val selected = buildList {
            if (traffic) add("traffic")
            if (events) add("events")
            if (errors) add("errors")
            if (spans) add("spans")
        }
        if (selected.isEmpty()) {
            return WriteResult.Error("Select at least one of traffic/events/errors/spans to clear.")
        }
        val approved = confirmation.confirm(
            title = "Clear captured data?",
            message = "The agent wants to permanently delete ${selected.joinToString(", ")} on $deviceId.",
        )
        if (!approved) return WriteResult.Error("Denied by the developer.")

        // NOTE the wire naming trap: clearCaptured(traces = …) clears TRAFFIC; spans clears trace spans.
        repo.clearCaptured(traces = traffic, events = events, errors = errors, spans = spans)
        return WriteResult.Ok(
            buildJsonObject {
                put("cleared", true)
                put("streams", buildJsonArray { selected.forEach { add(it) } })
            },
        )
    }

    @OptIn(ExperimentalUuidApi::class)
    fun createMockFromTraffic(
        repo: DevToolsRepository,
        vm: NetworkRulesViewModel,
        id: String,
        name: String?,
    ): WriteResult {
        if (!vm.networkRulesSupported.value) return unsupportedMocks()
        val entry = repo.traffic.value.firstOrNull { it.id == id }
            ?: return WriteResult.Error("No traffic entry with id $id")
        val rule = entry.toMockRule()
            .copy(id = Uuid.random().toString())
            .let { if (name != null) it.copy(name = name) else it }
        vm.addRule(rule)
        return WriteResult.Ok(json.encodeToJsonElement(MockRule.serializer(), rule))
    }

    suspend fun runAdbCommand(
        deviceId: String,
        command: String,
    ): WriteResult {
        val args = command.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (args.isEmpty()) return WriteResult.Error("Empty command.")
        if (!isAllowedAdbCommand(args)) {
            return WriteResult.Error(
                "Command not allowed. Permitted prefixes: ${ALLOWED_ADB_PREFIXES.joinToString(", ")}.",
            )
        }
        val result = withContext(Dispatchers.IO) {
            adbRunner.run(listOf("-s", deviceId) + args)
        }
        return WriteResult.Ok(
            buildJsonObject {
                put("exitCode", result.exitCode)
                put("stdout", result.stdout.take(MAX_ADB_OUTPUT_CHARS))
                put("stderr", result.stderr.take(MAX_ADB_OUTPUT_CHARS))
                if (result.stdout.length > MAX_ADB_OUTPUT_CHARS) put("stdoutTruncated", true)
                if (result.stderr.length > MAX_ADB_OUTPUT_CHARS) put("stderrTruncated", true)
            },
        )
    }

    private fun unsupportedMocks() =
        WriteResult.Error("Mock rules are not available for this device.")
}

// --- adb allowlist -----------------------------------------------------------------------------

private val adbRunner = DefaultAdbCommandRunner()
private const val MAX_ADB_OUTPUT_CHARS = 50_000

private val ALLOWED_ADB_PREFIXES = listOf(
    "shell am",
    "shell pm",
    "shell dumpsys",
    "shell settings",
    "shell getprop",
    "shell svc wifi",
    "shell svc data",
    "shell input",
    "shell screencap",
    "shell monkey",
    "shell wm",
    "shell cmd",
    "logcat",
)

internal fun isAllowedAdbCommand(args: List<String>): Boolean {
    val joined = args.joinToString(" ")
    return ALLOWED_ADB_PREFIXES.any { joined.startsWith(it, ignoreCase = true) }
}

// --- adapters + parsing ------------------------------------------------------------------------

private inline fun withSession(
    registry: DeviceSessionRegistry,
    request: CallToolRequest,
    block: (DeviceSessionHandle) -> CallToolResult,
): CallToolResult {
    val handle =
        registry.resolve(request.str("deviceId")) ?: return errorResult(noDeviceMessage(registry))
    return block(handle)
}

private fun WriteResult.toCallResult(): CallToolResult = when (this) {
    is WriteResult.Ok -> result(json)
    is WriteResult.Error -> errorResult(message)
}

private fun parseHeaders(element: JsonElement?): Map<String, List<String>>? = when (element) {
    null -> null
    is JsonObject -> element.mapValues { (_, value) ->
        when (value) {
            is JsonArray -> value.map { it.jsonPrimitive.content }
            is JsonPrimitive -> listOf(value.content)
            else -> emptyList()
        }
    }.filterValues { it.isNotEmpty() }

    is JsonPrimitive -> if (element.isString) ReplayHeaderText.parse(element.content) else null
    else -> null
}

private fun parseMockRules(element: JsonElement?): List<MockRule> {
    val array = element as? JsonArray
        ?: throw IllegalArgumentException("'rules' must be an array of rule objects.")
    return array.map { parseMockRule(it.jsonObject) }
}

private fun parseMockRule(obj: JsonObject): MockRule {
    val urlPattern = obj["urlPattern"]?.jsonPrimitive?.content
        ?: throw IllegalArgumentException("Each rule needs a urlPattern.")
    return MockRule(
        id = obj["id"]?.jsonPrimitive?.content ?: "",
        name = obj["name"]?.jsonPrimitive?.content,
        enabled = obj["enabled"]?.jsonPrimitive?.booleanOrNull ?: true,
        urlPattern = urlPattern,
        isRegex = obj["isRegex"]?.jsonPrimitive?.booleanOrNull ?: false,
        method = obj["method"]?.jsonPrimitive?.content,
        statusCode = obj["statusCode"]?.jsonPrimitive?.intOrNull ?: 200,
        responseBody = obj["responseBody"]?.jsonPrimitive?.content ?: "",
        contentType = obj["contentType"]?.jsonPrimitive?.content ?: "application/json",
    )
}

private fun parseThrottle(request: CallToolRequest): ThrottleProfile {
    val preset = request.str("preset")
    if (preset != null) {
        val key = preset.trim().lowercase()
        return ThrottleProfiles.PRESETS.firstOrNull { it.name == key }
            ?: when (key) {
                "slow_3g", "3g_slow" -> ThrottleProfiles.SLOW_3G
                "off", "none" -> ThrottleProfiles.NONE
                else -> throw IllegalArgumentException(
                    "Unknown preset '$preset'. Valid: ${ThrottleProfiles.PRESETS.joinToString(", ") { it.name }}.",
                )
            }
    }
    val latency = request.int("latencyMs")?.toLong() ?: 0L
    val download = request.int("downloadBytesPerSec")?.toLong() ?: 0L
    return ThrottleProfile(name = "custom", latencyMs = latency, downloadBytesPerSec = download)
}
