package io.github.yashkasera.alohomora.desktop.mcp

import io.github.yashkasera.alohomora.desktop.domain.config.ConfigScope
import io.github.yashkasera.alohomora.desktop.domain.config.ConfigStore
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.serialization.json.put

/**
 * Read-only journey/config tools, registered alongside the device tools. Journeys and the deep-link
 * catalog are app-scoped ([ConfigStore]); verification also needs a device's events, resolved through
 * the [DeviceSessionRegistry] like every other device tool. Thin adapters over
 * [AlohomoraJourneyMcpToolData]. Write tools (save/share/fire) are a later opt-in behind writeEnabled.
 */
fun registerAlohomoraJourneyTools(
    server: Server,
    registry: DeviceSessionRegistry,
    configStore: ConfigStore,
) {
    server.addTool(
        "list_journeys",
        "List saved event journeys (the expected flows to grade a session against). App-scoped, no " +
            "device needed. Optional scope: LOCAL (personal) or TEAM (shared); omit for both.",
        buildSchema { put("scope", stringProp("LOCAL or TEAM; omit for both.")) },
    ) { request ->
        val scope = request.str("scope")?.let { raw ->
            runCatching { ConfigScope.valueOf(raw.uppercase()) }.getOrNull()
        }
        result(AlohomoraJourneyMcpToolData.listJourneys(configStore, scope))
    }

    server.addTool(
        "verify_journey",
        "Grade a journey against a device's captured events and return the report (PASSED / FAILED / " +
            "NOT_EXERCISED, per-step outcomes, tagged timeline). Non-mutating.",
        buildSchema {
            put("journeyId", stringProp("Id of the journey to verify (from list_journeys)."))
            put("deviceId", stringProp("Target device; optional when only one is connected."))
        },
    ) { request ->
        withRepo(registry, request) { repo ->
            val journeyId = request.str("journeyId")
                ?: return@withRepo errorResult("Missing required argument: journeyId")
            result(AlohomoraJourneyMcpToolData.verifyJourney(configStore, repo, journeyId))
        }
    }

    server.addTool(
        "list_deeplinks",
        "List the typed deep-link catalog (definitions with typed parameters), optionally filtered by " +
            "module. App-scoped, no device needed.",
        buildSchema { put("module", stringProp("Only deep links in this module; omit for all.")) },
    ) { request ->
        result(AlohomoraJourneyMcpToolData.listDeepLinks(configStore, request.str("module")))
    }
}
