package io.github.yashkasera.alohomora.desktop.domain.config

/** A review request created from a pushed branch. [url] is null when the forge offers no auto-link. */
data class ReviewRef(val url: String?, val autoCreated: Boolean)

/**
 * Maps a pushed branch to a code review, forge by forge. The design is a generic git floor plus
 * optional adapters: a plain branch push works on any remote (the floor), and an adapter only removes
 * clicks. If an adapter ever became *required*, that would be lock-in and is wrong.
 *
 * The store owns the actual git push; a [Forge] only contributes the push options to set beforehand and
 * parses any review URL the server printed back. It names no JGit type, so it stays pure and testable.
 */
interface Forge {
    /** "merge request" | "pull request" | "change" | "review" — drives the UI label. */
    val reviewNoun: String

    /** Push options to set before pushing [branch] targeting [target]; empty for the generic floor. */
    fun pushOptions(branch: String, target: String): List<String>

    /** Parses a review URL out of the server's push messages, or none. */
    fun parseReview(pushMessages: String): ReviewRef
}

/**
 * The floor: works on any git remote. A plain push, no host knowledge, no review URL — the user opens
 * the review in their tool. This is the whole feature working with no adapter matched.
 */
object GenericGitForge : Forge {
    override val reviewNoun = "review"
    override fun pushOptions(branch: String, target: String): List<String> = emptyList()
    override fun parseReview(pushMessages: String): ReviewRef = ReviewRef(url = null, autoCreated = false)
}

/** GitHub prints a "Create a pull request" URL in the push output; parse it. No push options. */
object GitHubForge : Forge {
    override val reviewNoun = "pull request"
    override fun pushOptions(branch: String, target: String): List<String> = emptyList()
    override fun parseReview(pushMessages: String): ReviewRef {
        val url = URL_REGEX.find(pushMessages)?.value
        return ReviewRef(url = url, autoCreated = false)
    }

    private val URL_REGEX = Regex("""https://github\.com/\S+/pull/\S+""")
}

/** GitLab creates the MR via push options and echoes its URL; set the options and grab the URL. */
object GitLabForge : Forge {
    override val reviewNoun = "merge request"
    override fun pushOptions(branch: String, target: String): List<String> = listOf(
        "merge_request.create",
        "merge_request.target=$target",
    )

    override fun parseReview(pushMessages: String): ReviewRef {
        val url = URL_REGEX.find(pushMessages)?.value
        // The options above ask GitLab to create the MR, so it is auto-created even if the URL echo
        // format changes and we fail to parse it.
        return ReviewRef(url = url, autoCreated = true)
    }

    private val URL_REGEX = Regex("""https://\S+/-/merge_requests/\S+""")
}

/**
 * Picks a [Forge] from the remote URL, degrading to [GenericGitForge] for anything unrecognised — an
 * unknown host gets the floor, it never errors.
 */
object ForgeSelector {
    fun select(remoteUrl: String): Forge = when {
        remoteUrl.contains("github.com", ignoreCase = true) -> GitHubForge
        remoteUrl.contains("gitlab", ignoreCase = true) -> GitLabForge
        else -> GenericGitForge
    }
}
