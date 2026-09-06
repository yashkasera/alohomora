package io.github.yashkasera.alohomora.desktop.mcp

import io.github.yashkasera.alohomora.common.deeplink.DeepLinkDef
import io.github.yashkasera.alohomora.common.journey.JourneyDefinition
import io.github.yashkasera.alohomora.common.journey.JourneyMatcher
import io.github.yashkasera.alohomora.common.journey.JourneyReport
import io.github.yashkasera.alohomora.desktop.domain.config.ConfigKind
import io.github.yashkasera.alohomora.desktop.domain.config.ConfigScope
import io.github.yashkasera.alohomora.desktop.domain.config.ConfigStore
import io.github.yashkasera.alohomora.desktop.domain.repository.DevToolsRepository
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Pure projections for the journey/config MCP tools, over the app-scoped [ConfigStore] (and, for
 * verification, a device's [DevToolsRepository]). Returns [JsonElement] so the `addTool` handlers stay
 * thin adapters and this stays unit-testable. Reuses [JourneyMatcher]/[JourneyReport] directly — one
 * definition of a journey result, shared with the UI.
 *
 * Journeys and deep links carry no secrets, so no projection here needs the `slackWebhookUrl` guard the
 * device tools apply.
 */
object AlohomoraJourneyMcpToolData {

    suspend fun listJourneys(configStore: ConfigStore, scope: ConfigScope?): JsonElement {
        val items = when (scope) {
            null -> configStore.list(ConfigKind.Journeys, ConfigScope.LOCAL) +
                configStore.list(ConfigKind.Journeys, ConfigScope.TEAM)
            else -> configStore.list(ConfigKind.Journeys, scope)
        }
        return buildJsonArray {
            items.forEach { item ->
                val j = item.value
                add(
                    buildJsonObject {
                        put("id", j.id)
                        put("name", j.name)
                        put("description", j.description)
                        put("scope", item.scope.name)
                        put("state", item.state.name)
                        put("ordered", j.ordered)
                        put("stepCount", j.steps.size)
                    },
                )
            }
        }
    }

    /** Grades [journeyId] against the device's captured events, returning the report or an error. */
    suspend fun verifyJourney(
        configStore: ConfigStore,
        repo: DevToolsRepository,
        journeyId: String,
    ): JsonElement {
        val journey = findJourney(configStore, journeyId)
            ?: return buildJsonObject { put("error", "No journey with id '$journeyId'.") }
        val report = JourneyMatcher.evaluate(journey, repo.events.value)
        return json.encodeToJsonElement(JourneyReport.serializer(), report)
    }

    suspend fun listDeepLinks(configStore: ConfigStore, module: String?): JsonElement {
        val items = configStore.list(ConfigKind.DeepLinks, ConfigScope.LOCAL) +
            configStore.list(ConfigKind.DeepLinks, ConfigScope.TEAM)
        val defs = items.map { it.value }.filter { module == null || it.module.equals(module, true) }
        return buildJsonArray {
            defs.forEach { def -> add(def.toJson()) }
        }
    }

    private suspend fun findJourney(configStore: ConfigStore, id: String): JourneyDefinition? =
        (
            configStore.list(ConfigKind.Journeys, ConfigScope.LOCAL) +
                configStore.list(ConfigKind.Journeys, ConfigScope.TEAM)
            ).map { it.value }.firstOrNull { it.id == id }

    private fun DeepLinkDef.toJson(): JsonElement = buildJsonObject {
        put("id", id)
        put("name", name)
        put("module", module)
        flow?.let { put("flow", it) }
        put("description", description)
        put("uriTemplate", uriTemplate)
        put(
            "params",
            buildJsonArray {
                params.forEach { p ->
                    add(
                        buildJsonObject {
                            put("name", p.name)
                            put("type", p.type.name)
                            put("required", p.required)
                            if (p.allowedValues.isNotEmpty()) {
                                put("allowedValues", buildJsonArray { p.allowedValues.forEach { add(it) } })
                            }
                        },
                    )
                }
            },
        )
        put("examples", buildJsonArray { examples.forEach { add(it) } })
    }
}
