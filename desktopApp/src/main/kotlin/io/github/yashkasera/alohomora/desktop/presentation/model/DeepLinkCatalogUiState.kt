package io.github.yashkasera.alohomora.desktop.presentation.model

import io.github.yashkasera.alohomora.common.deeplink.DeepLinkDef
import io.github.yashkasera.alohomora.common.deeplink.DeepLinkFieldErrors
import io.github.yashkasera.alohomora.desktop.domain.config.ConfigItem
import io.github.yashkasera.alohomora.desktop.domain.config.ConfigScope
import io.github.yashkasera.alohomora.desktop.domain.config.Proposal

/** One module's definitions, for the grouped (collapsible) catalog view. */
data class CatalogModuleGroup(
    val module: String,
    val defs: List<ConfigItem<DeepLinkDef>>,
)

/** Single immutable state for the deep-link catalog viewer + typed editor/builder. */
data class DeepLinkCatalogUiState(
    val scope: ConfigScope = ConfigScope.LOCAL,
    val defs: List<ConfigItem<DeepLinkDef>> = emptyList(),
    val query: String = "",
    /** A module jump-chip selection. Mutually exclusive with [query] in the UI. */
    val moduleFilter: String? = null,
    val selectedId: String? = null,
    /** The definition open in the editor, or null when closed. */
    val editorDraft: DeepLinkDef? = null,
    /** Structural problems with the draft — blocks saving; drives inline field errors. */
    val fieldErrors: DeepLinkFieldErrors = DeepLinkFieldErrors(),
    /** Advisory warnings (undeclared placeholders, example mismatches) — never block a save. */
    val validationErrors: List<String> = emptyList(),
    val lastProposal: Proposal? = null,
    val message: String? = null,
    val isLoading: Boolean = false,
) {
    /**
     * Definitions after the search/module filter. A non-empty query flattens grouping into a
     * cross-module ranked list; a [moduleFilter] narrows to one module (name-sorted); otherwise the
     * full list is ordered by module → flow → name for the grouped view. Query ranks: name,
     * module/flow, param name/allowedValues, uriTemplate, description.
     */
    val visibleDefs: List<ConfigItem<DeepLinkDef>>
        get() {
            val q = query.trim()
            if (q.isNotEmpty()) {
                return defs.filter { matches(it.value, q) }
                    .sortedWith(compareByDescending<ConfigItem<DeepLinkDef>> { rank(it.value, q) }.thenBy { it.value.name })
            }
            val scoped = moduleFilter?.let { m -> defs.filter { it.value.module == m } } ?: defs
            return scoped.sortedWith(
                compareBy({ it.value.module }, { it.value.flow ?: "" }, { it.value.name }),
            )
        }

    /** Distinct module names across all defs, sorted — the jump-chip axis. */
    val moduleNames: List<String>
        get() = defs.map { it.value.module }.distinct().sorted()

    /** Count of defs per module, for the chip badges. */
    val moduleCounts: Map<String, Int>
        get() = defs.groupingBy { it.value.module }.eachCount()

    /** [visibleDefs] grouped by module for the collapsible view (used when [query] is blank). */
    val moduleGroups: List<CatalogModuleGroup>
        get() = visibleDefs.groupBy { it.value.module }
            .map { (module, items) -> CatalogModuleGroup(module, items) }

    private fun matches(def: DeepLinkDef, q: String): Boolean =
        def.name.contains(q, true) ||
            def.module.contains(q, true) ||
            (def.flow?.contains(q, true) == true) ||
            def.params.any { p -> p.name.contains(q, true) || p.allowedValues.any { it.contains(q, true) } } ||
            def.uriTemplate.contains(q, true) ||
            def.description.contains(q, true)

    /** Higher is a better match; ties broken by name in [visibleDefs]. */
    private fun rank(def: DeepLinkDef, q: String): Int = when {
        def.name.contains(q, true) -> 5
        def.module.contains(q, true) || def.flow?.contains(q, true) == true -> 4
        def.params.any { p -> p.name.contains(q, true) || p.allowedValues.any { it.contains(q, true) } } -> 3
        def.uriTemplate.contains(q, true) -> 2
        else -> 1
    }

    val selected: ConfigItem<DeepLinkDef>?
        get() = defs.firstOrNull { it.value.id == selectedId }
}
