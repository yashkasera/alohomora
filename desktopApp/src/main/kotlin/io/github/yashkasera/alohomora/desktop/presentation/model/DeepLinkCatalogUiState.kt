package io.github.yashkasera.alohomora.desktop.presentation.model

import io.github.yashkasera.alohomora.common.deeplink.DeepLinkDef
import io.github.yashkasera.alohomora.desktop.domain.config.ConfigItem
import io.github.yashkasera.alohomora.desktop.domain.config.ConfigScope
import io.github.yashkasera.alohomora.desktop.domain.config.Proposal

/** Single immutable state for the deep-link catalog viewer + typed editor/builder. */
data class DeepLinkCatalogUiState(
    val scope: ConfigScope = ConfigScope.LOCAL,
    val defs: List<ConfigItem<DeepLinkDef>> = emptyList(),
    val query: String = "",
    val selectedId: String? = null,
    /** The definition open in the editor, or null when closed. */
    val editorDraft: DeepLinkDef? = null,
    /** Problems from validating the draft's examples against its template + params. */
    val validationErrors: List<String> = emptyList(),
    val lastProposal: Proposal? = null,
    val message: String? = null,
    val isLoading: Boolean = false,
) {
    /**
     * Definitions after the search filter. A non-empty query flattens the module grouping into a
     * cross-module list; the composable groups by module when the query is blank. Indexed fields, in
     * rank order: name, module/flow, param name/allowedValues, uriTemplate, description.
     */
    val visibleDefs: List<ConfigItem<DeepLinkDef>>
        get() {
            val q = query.trim()
            if (q.isEmpty()) return defs.sortedWith(compareBy({ it.value.module }, { it.value.name }))
            return defs.filter { matches(it.value, q) }
                .sortedWith(compareByDescending<ConfigItem<DeepLinkDef>> { rank(it.value, q) }.thenBy { it.value.name })
        }

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
