package io.github.yashkasera.alohomora.desktop.mcp

import io.github.yashkasera.alohomora.common.deeplink.DeepLinkDef
import io.github.yashkasera.alohomora.common.deeplink.buildUri
import io.github.yashkasera.alohomora.common.journey.JourneyDefinition
import io.github.yashkasera.alohomora.desktop.data.adb.DefaultAdbCommandRunner
import io.github.yashkasera.alohomora.desktop.domain.config.ConfigKind
import io.github.yashkasera.alohomora.desktop.domain.config.ConfigScope
import io.github.yashkasera.alohomora.desktop.domain.config.ConfigStore
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Journey/config write tools, registered only when write tools are on and a config store is present.
 *
 * `save_journey` is a local, reversible write (no confirmation). `share_journey` pushes to a remote, so
 * it gates on [McpConfirmationBroker] like the destructive device tools. `fire_deeplink` validates its
 * args against the definition's typed params before firing on the device via `adb am start`.
 */
fun registerAlohomoraJourneyWriteTools(
    server: Server,
    registry: DeviceSessionRegistry,
    configStore: ConfigStore,
    confirmation: McpConfirmationBroker,
) {
    server.addTool(
        "save_journey",
        "Create or update a LOCAL-scope journey. Pass the full definition object " +
            "{id, name, description?, ordered?, allowUnexpected?, steps:[{id, eventName, ...}]}. " +
            "An existing id updates in place. Use share_journey to propose it to the team.",
        buildSchema {
            put(
                "definition",
                buildJsonObject {
                    put("type", "object")
                    put("description", "The full JourneyDefinition to save.")
                },
            )
        },
    ) { request ->
        val element = request.arguments?.get("definition")
            ?: return@addTool errorResult("Missing required argument: definition")
        AlohomoraJourneyMcpWriteData.saveJourney(configStore, element).toResult()
    }

    server.addTool(
        "share_journey",
        "Propose a journey to the team: push a branch and open (or link to) a review. Remote-affecting, " +
            "so it requires the developer to confirm in the desktop app. Requires a connected config repo.",
        buildSchema {
            put("journeyId", stringProp("Id of the journey to share (from list_journeys)."))
            put("description", stringProp("Optional description; becomes the proposal body."))
        },
    ) { request ->
        val journeyId = request.str("journeyId")
            ?: return@addTool errorResult("Missing required argument: journeyId")
        AlohomoraJourneyMcpWriteData.shareJourney(
            configStore = configStore,
            confirmation = confirmation,
            journeyId = journeyId,
            description = request.str("description"),
        ).toResult()
    }

    server.addTool(
        "fire_deeplink",
        "Fire a catalogued deep link on a device. Validates args against the definition's typed params " +
            "(required/enum/int/uuid) and refuses malformed input, then opens it via 'am start'.",
        buildSchema {
            put("deviceId", stringProp("Target device; optional when only one is connected."))
            put("id", stringProp("The deep-link definition id, from list_deeplinks."))
            put(
                "args",
                buildJsonObject {
                    put("type", "object")
                    put("description", "Parameter name -> value, matching the definition's params.")
                },
            )
        },
    ) { request ->
        val handle = registry.resolve(request.str("deviceId"))
            ?: return@addTool errorResult(noDeviceMessage(registry))
        val id = request.str("id") ?: return@addTool errorResult("Missing required argument: id")
        AlohomoraJourneyMcpWriteData.fireDeepLink(
            configStore = configStore,
            deviceId = handle.deviceId,
            id = id,
            args = stringMap(request.arguments?.get("args")),
        ).toResult()
    }
}

/** Pure write actions over the config store, returning [WriteResult] so the adapter renders errors. */
internal object AlohomoraJourneyMcpWriteData {

    private val adbRunner = DefaultAdbCommandRunner()

    suspend fun saveJourney(configStore: ConfigStore, definition: JsonElement): WriteResult {
        val journey = runCatching {
            json.decodeFromJsonElement(JourneyDefinition.serializer(), definition)
        }.getOrElse { return WriteResult.Error("Invalid journey definition: ${it.message}") }
        if (journey.id.isBlank()) return WriteResult.Error("The definition needs a non-empty id.")
        configStore.saveLocal(ConfigKind.Journeys, journey)
        return WriteResult.Ok(
            buildJsonObject {
                put("saved", true)
                put("id", journey.id)
                put("scope", ConfigScope.LOCAL.name)
            },
        )
    }

    suspend fun shareJourney(
        configStore: ConfigStore,
        confirmation: McpConfirmationBroker,
        journeyId: String,
        description: String?,
    ): WriteResult {
        val journey = findJourney(configStore, journeyId)
            ?: return WriteResult.Error("No journey with id '$journeyId'.")
        val approved = confirmation.confirm(
            title = "Share journey with the team?",
            message = "The agent wants to push '${journey.name}' as a proposal.",
        )
        if (!approved) return WriteResult.Error("Denied by the developer.")
        val toShare = if (description != null) journey.copy(description = description) else journey
        val proposal = runCatching { configStore.shareWithTeam(ConfigKind.Journeys, toShare) }
            .getOrElse { return WriteResult.Error(it.message ?: "Share failed.") }
        return WriteResult.Ok(
            buildJsonObject {
                put("shared", true)
                put("branch", proposal.branch)
                proposal.reviewUrl?.let { put("reviewUrl", it) }
                put("reviewNoun", proposal.reviewNoun)
                put("autoCreated", proposal.autoCreated)
            },
        )
    }

    suspend fun fireDeepLink(
        configStore: ConfigStore,
        deviceId: String,
        id: String,
        args: Map<String, String>,
    ): WriteResult {
        val def = findDeepLink(configStore, id)
            ?: return WriteResult.Error("No deep link with id '$id'.")
        val built = def.buildUri(args)
        val url = built.url
            ?: return WriteResult.Error("Invalid arguments: ${built.errors.joinToString("; ")}")
        val result = withContext(Dispatchers.IO) {
            adbRunner.run(
                listOf("-s", deviceId, "shell", "am", "start", "-a", "android.intent.action.VIEW", "-d", url),
            )
        }
        return WriteResult.Ok(
            buildJsonObject {
                put("fired", true)
                put("url", url)
                put("exitCode", result.exitCode)
            },
        )
    }

    private suspend fun findJourney(configStore: ConfigStore, id: String): JourneyDefinition? =
        (
            configStore.list(ConfigKind.Journeys, ConfigScope.LOCAL) +
                configStore.list(ConfigKind.Journeys, ConfigScope.TEAM)
            ).map { it.value }.firstOrNull { it.id == id }

    private suspend fun findDeepLink(configStore: ConfigStore, id: String): DeepLinkDef? =
        (
            configStore.list(ConfigKind.DeepLinks, ConfigScope.LOCAL) +
                configStore.list(ConfigKind.DeepLinks, ConfigScope.TEAM)
            ).map { it.value }.firstOrNull { it.id == id }
}

private fun WriteResult.toResult(): CallToolResult = when (this) {
    is WriteResult.Ok -> result(json)
    is WriteResult.Error -> errorResult(message)
}

private fun stringMap(element: JsonElement?): Map<String, String> {
    val obj = element as? JsonObject ?: return emptyMap()
    return obj.mapNotNull { (key, value) ->
        (value as? JsonPrimitive)?.let { key to it.content }
    }.toMap()
}
