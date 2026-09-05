package io.github.yashkasera.alohomora.desktop.data.config

import io.github.yashkasera.alohomora.desktop.domain.config.ConfigKind
import io.github.yashkasera.alohomora.desktop.domain.config.Forge
import io.github.yashkasera.alohomora.desktop.domain.config.ForgeSelector
import io.github.yashkasera.alohomora.desktop.domain.config.Proposal
import io.github.yashkasera.alohomora.desktop.domain.config.SyncStatus
import java.io.Closeable
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.BranchTrackingStatus
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.RefSpec

/**
 * Team scope, backed by a local clone of the config repo.
 *
 * The load-bearing cadence: file writes reuse [LocalConfigStore] pointed at the clone working tree, so
 * output stays byte-identical to LOCAL and Save is never a commit. Only [shareWithTeam] commits — it
 * branches, commits, pushes, and returns the tree to [mainBranch], so the user is never "on a branch".
 *
 * Proposal creation is the generic git floor plus optional [Forge] adapters: the push always works;
 * an adapter only adds push options or a review URL. An unrecognised host degrades to the floor.
 */
class GitConfigStore(
    private val repoDir: File,
    private val git: Git,
    private val remoteUrl: String,
    private val author: PersonIdent,
    private val credentials: CredentialsProvider? = null,
    private val selectForge: (String) -> Forge = ForgeSelector::select,
    private val mainBranch: String = "main",
) : Closeable {

    private val files = LocalConfigStore(repoDir)

    suspend fun <T> list(kind: ConfigKind<T>): List<T> = files.list(kind)

    /**
     * Packages [item] into a branch and pushes it as a proposal, returning the working tree to
     * [mainBranch]. Direct-to-main is never generated here; protection is server-side.
     */
    suspend fun <T> shareWithTeam(kind: ConfigKind<T>, item: T): Proposal = withContext(Dispatchers.IO) {
        files.save(kind, item)
        val branch = branchName(kind.nameOf(item))
        git.checkout().setCreateBranch(true).setName(branch).call()
        try {
            git.add().addFilepattern(kind.dir).call()
            git.commit()
                .setMessage(commitMessage(kind.nameOf(item), kind.descriptionOf(item)))
                .setAuthor(author)
                .call()

            val forge = selectForge(remoteUrl)
            val options = forge.pushOptions(branch, mainBranch)
            val push = git.push()
                .setRemote(ORIGIN)
                .setRefSpecs(RefSpec("$branch:$branch"))
                .also { cmd ->
                    if (options.isNotEmpty()) cmd.setPushOptions(options)
                    if (credentials != null) cmd.setCredentialsProvider(credentials)
                }
                .call()

            val messages = push.joinToString("\n") { it.messages.orEmpty() }
            val review = forge.parseReview(messages)
            Proposal(
                branch = branch,
                reviewUrl = review.url,
                reviewNoun = forge.reviewNoun,
                autoCreated = review.autoCreated || options.isNotEmpty(),
            )
        } finally {
            // Always land back on main, even if the push failed, so the tree never gets stuck on a
            // proposal branch.
            git.checkout().setName(mainBranch).call()
        }
    }

    /** Fetches the remote and reports drift of the local main against its upstream. */
    suspend fun sync(): SyncStatus = withContext(Dispatchers.IO) {
        git.fetch().also { if (credentials != null) it.setCredentialsProvider(credentials) }.call()
        val tracking = BranchTrackingStatus.of(git.repository, mainBranch)
            ?: return@withContext SyncStatus.UpToDate(System.currentTimeMillis())
        if (tracking.aheadCount == 0 && tracking.behindCount == 0) {
            SyncStatus.UpToDate(System.currentTimeMillis())
        } else {
            SyncStatus.Drift(behind = tracking.behindCount, ahead = tracking.aheadCount)
        }
    }

    override fun close() = git.close()

    private fun branchName(name: String): String {
        val user = System.getProperty("user.name")?.let { slug(it) }?.ifBlank { null } ?: "user"
        return "proposal/${slug(name)}-$user-${LocalDate.now()}"
    }

    private fun commitMessage(name: String, description: String): String =
        if (description.isBlank()) name else "$name\n\n$description"

    companion object {
        private const val ORIGIN = "origin"

        /** Opens an existing clone. */
        fun open(
            repoDir: File,
            remoteUrl: String,
            author: PersonIdent,
            credentials: CredentialsProvider? = null,
        ): GitConfigStore = GitConfigStore(repoDir, Git.open(repoDir), remoteUrl, author, credentials)

        /** Clones [remoteUrl] into [repoDir] and opens it. */
        fun clone(
            remoteUrl: String,
            repoDir: File,
            author: PersonIdent,
            credentials: CredentialsProvider? = null,
        ): GitConfigStore {
            val git = Git.cloneRepository()
                .setURI(remoteUrl)
                .setDirectory(repoDir)
                .also { if (credentials != null) it.setCredentialsProvider(credentials) }
                .call()
            return GitConfigStore(repoDir, git, remoteUrl, author, credentials)
        }
    }
}
