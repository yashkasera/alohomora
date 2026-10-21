package io.github.yashkasera.alohomora.desktop.presentation.model

import io.github.yashkasera.alohomora.desktop.domain.config.SyncStatus

/**
 * Plain-language copy for the Team-config surface, kept out of the composables so it is unit-testable
 * and so the "hide git vocabulary" promise of the domain layer is not undone at the last mile.
 *
 * The raw inputs are a [SyncStatus] (behind/ahead counts) and the message of a git/transport failure.
 * A product manager never needs to read either in its native form, so both are translated here.
 */
object TeamConfigCopy {

    /**
     * A one-line summary of how the local copy compares to the team's shared config, or null when
     * there is nothing to say (not connected). Replaces the raw "N behind, N ahead" drift counts.
     */
    fun syncSummary(status: SyncStatus): String? = when (status) {
        SyncStatus.NotConnected -> null
        is SyncStatus.UpToDate -> "You're in sync with your team."
        is SyncStatus.Drift -> buildString {
            if (status.behind > 0) {
                val it = if (status.behind == 1) "it" else "them"
                append("Your team shared ${status.behind} ${plural(status.behind, "update")} — Sync to get $it.")
            }
            if (status.ahead > 0) {
                if (isNotEmpty()) append(" ")
                append("You have ${status.ahead} ${plural(status.ahead, "change")} waiting to share.")
            }
            if (isEmpty()) append("You're in sync with your team.")
        }
    }

    /**
     * Maps raw git/transport error text to something a non-engineer can act on. Falls back to a
     * generic line that points at Developer mode for the raw detail, so nothing is lost — it is just
     * not the first thing a PM reads.
     */
    fun friendlyError(raw: String?): String {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return GENERIC
        val lower = text.lowercase()
        return when {
            "not connected" in lower ->
                "Team config isn't set up yet. Open Preferences → Team config to connect, then share."

            "auth" in lower || "not authorized" in lower || "unauthorized" in lower ||
                "401" in lower || "403" in lower || "permission" in lower || "credential" in lower ->
                "Couldn't sign in to the repository. Check your access token and that you have access."

            "not found" in lower || "404" in lower ->
                "Couldn't find that repository. Double-check the link your team shared."

            "unknownhost" in lower || "unable to resolve" in lower || "no route" in lower ||
                "network" in lower || "timed out" in lower || "timeout" in lower ->
                "Couldn't reach the repository. Check your internet connection and the link."

            "protected" in lower || ("branch" in lower && "protect" in lower) ->
                "The team's main line is protected, so your change goes in as a review for a teammate " +
                    "to approve — which is exactly what sharing does."

            else -> GENERIC
        }
    }

    private const val GENERIC =
        "Something went wrong talking to the repository. Turn on Developer mode to see the details."

    private fun plural(n: Int, singular: String) = if (n == 1) singular else "${singular}s"
}
