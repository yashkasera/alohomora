package io.github.yashkasera.alohomora.desktop.presentation.model

import io.github.yashkasera.alohomora.common.journey.JourneyDefinition
import io.github.yashkasera.alohomora.common.journey.JourneyReport
import io.github.yashkasera.alohomora.desktop.domain.config.ConfigItem
import io.github.yashkasera.alohomora.desktop.domain.config.ConfigScope
import io.github.yashkasera.alohomora.desktop.domain.config.Proposal

/**
 * The single immutable state for the journeys surfaces. The view model owns all mutation; composables
 * read this and emit intents. Derived values are pure getters so nothing recomputes off-screen.
 */
data class JourneyUiState(
    val scope: ConfigScope = ConfigScope.LOCAL,
    val journeys: List<ConfigItem<JourneyDefinition>> = emptyList(),
    val query: String = "",
    val selectedId: String? = null,
    /** The journey open in the editor, or null when the editor is closed. Not yet persisted. */
    val editorDraft: JourneyDefinition? = null,
    /** True when the open editor is a freshly created journey (drives the "New" vs "Edit" title). */
    val editorIsNew: Boolean = false,
    /** The most recent validation result for [selectedId], if any. */
    val lastReport: JourneyReport? = null,
    /** Non-null while the full-width live validation panel is open, grading this journey live. */
    val liveJourneyId: String? = null,
    /** Display name of the journey being validated live, for the panel header. */
    val liveJourneyName: String = "",
    /** The most recent successful "Share with team" result, for the In-review link. */
    val lastProposal: Proposal? = null,
    /** A transient message to surface (e.g. a share failure). */
    val message: String? = null,
    val isLoading: Boolean = false,
) {
    /** Journeys after the search filter, ranked name-first. Case-insensitive substring. */
    val visibleJourneys: List<ConfigItem<JourneyDefinition>>
        get() {
            val q = query.trim()
            if (q.isEmpty()) return journeys
            return journeys.filter { item ->
                val j = item.value
                j.name.contains(q, ignoreCase = true) ||
                    j.description.contains(q, ignoreCase = true) ||
                    j.steps.any { it.eventName.contains(q, ignoreCase = true) }
            }
        }

    val selected: ConfigItem<JourneyDefinition>?
        get() = journeys.firstOrNull { it.value.id == selectedId }
}
