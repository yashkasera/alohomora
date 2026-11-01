package io.github.yashkasera.alohomora.desktop.data.config

import io.github.yashkasera.alohomora.desktop.domain.config.ConfigKind
import io.github.yashkasera.alohomora.desktop.domain.config.Forge
import io.github.yashkasera.alohomora.desktop.domain.config.ForgeSelector
import io.github.yashkasera.alohomora.desktop.domain.config.Proposal
import io.github.yashkasera.alohomora.desktop.domain.config.SyncStatus
import java.io.Closeable
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeCommand
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.revwalk.RevWalkUtils
import org.eclipse.jgit.revwalk.filter.RevFilter
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
 *
 * Every operation holds [gitLock]. One instance serves every device window and the MCP `share_journey`
 * tool, and [shareWithTeam] moves the working tree onto a proposal branch and back: a concurrent
 * [list] would read the unshared file as live, and a concurrent [sync] would fast-forward whichever
 * branch happened to be checked out.
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

    /** Not reentrant: nothing that holds it may call another locked member. */
    private val gitLock = Mutex()

    suspend fun <T> list(kind: ConfigKind<T>): List<T> = gitLock.withLock { files.list(kind) }

    /**
     * Packages [item] into a branch and pushes it as a proposal, returning the working tree to
     * [mainBranch]. Direct-to-main is never generated here; protection is server-side.
     */
    suspend fun <T> shareWithTeam(kind: ConfigKind<T>, item: T): Proposal = gitLock.withLock {
        withContext(Dispatchers.IO) { shareLocked(kind, item) }
    }

    private suspend fun <T> shareLocked(kind: ConfigKind<T>, item: T): Proposal {
        val branch = branchName(kind.nameOf(item))
        try {
            // Check out the branch BEFORE writing files, so a failed checkout (e.g. the branch already
            // exists) never strands dirty files on main, and the finally below always returns to main.
            git.checkout().setCreateBranch(true).setName(branch).call()
            files.save(kind, item)
            // The committed catalog is the browsable, reviewable artifact, regenerated (not hand-edited)
            // so it only diffs when the definitions change.
            if (kind == ConfigKind.DeepLinks) regenerateDeepLinkReadme()
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
            return Proposal(
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

    /**
     * Fetches, then fast-forwards the local [mainBranch] onto the team's when it is strictly behind.
     *
     * Fetching alone moved only `origin/main`: TEAM lists kept reading the old files, the UI kept
     * offering "Sync to get them", and every proposal branched from an ever-older main. Fast-forward
     * only, never a merge or reset — when both sides have moved the result is [SyncStatus.Diverged]
     * and the local work is left exactly as it was.
     *
     * Compares against `refs/remotes/origin/<main>` directly rather than the branch's upstream config,
     * which a repo created by `initialize` never gets — that clone would otherwise report up to date
     * forever.
     */
    suspend fun sync(): SyncStatus = gitLock.withLock {
        withContext(Dispatchers.IO) {
            git.fetch().also { if (credentials != null) it.setCredentialsProvider(credentials) }.call()
            var drift = compareWithRemote()
                ?: return@withContext SyncStatus.UpToDate(System.currentTimeMillis())
            if (drift.behind > 0 && drift.ahead == 0) {
                fastForward(drift.remote)
                drift = compareWithRemote()
                    ?: return@withContext SyncStatus.UpToDate(System.currentTimeMillis())
            }
            when {
                drift.behind > 0 && drift.ahead > 0 -> SyncStatus.Diverged(drift.behind, drift.ahead)
                drift.behind == 0 && drift.ahead == 0 -> SyncStatus.UpToDate(System.currentTimeMillis())
                else -> SyncStatus.Drift(behind = drift.behind, ahead = drift.ahead)
            }
        }
    }

    override fun close() = git.close()

    private class Divergence(val ahead: Int, val behind: Int, val remote: ObjectId)

    /** Commits on each side since the merge base, or null when either ref is missing (empty remote). */
    private fun compareWithRemote(): Divergence? {
        val repo = git.repository
        val localId = repo.resolve("refs/heads/$mainBranch") ?: return null
        val remoteId = repo.resolve("refs/remotes/$ORIGIN/$mainBranch") ?: return null
        RevWalk(repo).use { walk ->
            val local = walk.parseCommit(localId)
            val remote = walk.parseCommit(remoteId)
            walk.revFilter = RevFilter.MERGE_BASE
            walk.markStart(local)
            walk.markStart(remote)
            // Null for unrelated histories, which RevWalkUtils.count then counts in full: diverged.
            val base = walk.next()
            walk.reset()
            walk.revFilter = RevFilter.ALL
            return Divergence(
                ahead = RevWalkUtils.count(walk, local, base),
                behind = RevWalkUtils.count(walk, remote, base),
                remote = remoteId,
            )
        }
    }

    /**
     * FF_ONLY on the checked-out main, so the working tree moves with the ref. A hand edit in the
     * clone that the incoming change would overwrite makes JGit throw rather than clobber it, which
     * surfaces as a sync error instead of lost work.
     */
    private fun fastForward(remote: ObjectId) {
        if (git.repository.branch != mainBranch) git.checkout().setName(mainBranch).call()
        val result = git.merge()
            .include(remote)
            .setFastForward(MergeCommand.FastForwardMode.FF_ONLY)
            .call()
        check(result.mergeStatus.isSuccessful) { "Couldn't bring in team updates: ${result.mergeStatus}" }
    }

    private suspend fun regenerateDeepLinkReadme() {
        val defs = files.list(ConfigKind.DeepLinks)
        File(repoDir, "deeplinks/README.md").apply { parentFile?.mkdirs() }
            .writeText(DeepLinkCatalogRenderer.render(defs))
    }

    private fun branchName(name: String): String {
        val user = System.getProperty("user.name")?.let { slug(it) }?.ifBlank { null } ?: "user"
        // Include the time so re-sharing the same artifact the same day gets a fresh branch rather than
        // failing with RefAlreadyExistsException.
        val stamp = LocalDateTime.now().format(BRANCH_STAMP)
        return "proposal/${slug(name)}-$user-$stamp"
    }

    private fun commitMessage(name: String, description: String): String =
        if (description.isBlank()) name else "$name\n\n$description"

    companion object {
        private const val ORIGIN = "origin"
        private val BRANCH_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

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
