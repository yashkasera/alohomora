package io.github.yashkasera.alohomora.desktop.data.config

import io.github.yashkasera.alohomora.desktop.data.devtools.DesktopConfigRepoPrefs
import io.github.yashkasera.alohomora.desktop.domain.config.SyncStatus
import java.io.File
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.RemoteRefUpdate
import org.eclipse.jgit.transport.URIish

/** Connection state the UI renders, git vocabulary hidden. */
sealed interface ConfigRepoStatus {
    data object Disconnected : ConfigRepoStatus
    data class Connected(val url: String, val clonePath: String) : ConfigRepoStatus
    data class Error(val message: String) : ConfigRepoStatus
}

/**
 * Owns the lifecycle of the team config clone: connect (clone/open), initialize (scaffold an empty
 * repo), sync, disconnect, and the developer-mode flag. Holds the current [GitConfigStore] so
 * [ConfigStoreFacade] can read it through a provider and pick up a connection made mid-session.
 *
 * Auth comes from [ConfigRepoCredentials]: HTTPS remotes use a personal access token (the one just
 * typed, else the OS keychain entry for the host), SSH remotes the user's agent through the registered
 * sshd transport, and file remotes need nothing.
 *
 * There is one clone path, so a clone belongs to exactly one remote. Connecting a different URL moves
 * the old clone aside rather than reusing it — reusing it reported `Connected(newUrl)` while sharing
 * pushed to the old origin — and [initialize] never runs `git init` over an existing clone, which
 * would commit the scaffold onto the previous team's history and push all of it to the new remote.
 */
class ConfigRepoManager(
    private val clonePath: File = File(DesktopConfigRepoPrefs.loadClonePath()),
    private val author: PersonIdent = defaultAuthor(),
) {
    private val _status = MutableStateFlow<ConfigRepoStatus>(ConfigRepoStatus.Disconnected)
    val status: StateFlow<ConfigRepoStatus> = _status.asStateFlow()

    private val _team = MutableStateFlow<GitConfigStore?>(null)
    val team: StateFlow<GitConfigStore?> = _team.asStateFlow()

    private val _developerMode = MutableStateFlow(DesktopConfigRepoPrefs.loadDeveloperMode())
    val developerMode: StateFlow<Boolean> = _developerMode.asStateFlow()

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.NotConnected)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    /** The current team store, read by the facade's provider. */
    val teamStore: GitConfigStore? get() = _team.value

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        // Reconnect silently to a previously connected clone on the next launch — but off the main
        // thread: this is constructed inside a Compose remember{}, and Git.open() is blocking disk I/O.
        val url = DesktopConfigRepoPrefs.loadRepoUrl()
        scope.launch {
            ConfigRepoCredentials.ensureSshRegistered()
            // Only when the clone's origin is the saved URL: a mismatch (left by a build that reused
            // clones across URLs) would otherwise relabel one team's repo as another's.
            if (url != null && isCloneOf(url)) {
                runCatching {
                    GitConfigStore.open(clonePath, url, author, ConfigRepoCredentials.providerFor(url))
                }.onSuccess {
                    _team.value = it
                    _status.value = ConfigRepoStatus.Connected(url, clonePath.path)
                }
            }
        }
    }

    /**
     * Clones [url] into the clone path (or opens it if it is already a clone of [url]) and makes it the
     * team store. A clone of any other remote is moved aside first. A non-blank [token] is stored in
     * the OS keychain for the URL's host (HTTPS auth).
     */
    suspend fun connect(url: String, token: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            closeTeam()
            saveToken(url, token)
            val credentials = credentialsFor(url, token)
            val reuse = isCloneOf(url)
            if (!reuse) moveCloneAside()
            val store = if (reuse) {
                GitConfigStore.open(clonePath, url, author, credentials)
            } else {
                clonePath.parentFile?.mkdirs()
                GitConfigStore.clone(url, clonePath, author, credentials)
            }
            _team.value = store
            DesktopConfigRepoPrefs.saveRepoUrl(url)
            _status.value = ConfigRepoStatus.Connected(url, clonePath.path)
        }.onFailure { _status.value = ConfigRepoStatus.Error(it.message ?: "Connect failed") }
            .map {}
    }

    /**
     * Scaffolds the config structure and connects to it. With a [url] it also wires the remote and
     * pushes `main`; blank creates a **local-only** repo (start now, add a remote and share later).
     *
     * Always starts from an empty directory: an existing clone is moved aside first. Refusing outright
     * would strand the user, since Disconnect keeps the clone and nothing in the app removes it.
     */
    suspend fun initialize(url: String, token: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            closeTeam()
            saveToken(url, token)
            val credentials = credentialsFor(url, token)
            moveCloneAside()
            clonePath.mkdirs()
            try {
                scaffold(url, credentials)
            } catch (e: Exception) {
                // Everything here was created by this call a moment ago (the old clone, if any, was
                // moved aside), so removing it loses nothing — and leaving it would let the next
                // Connect reopen a scaffold whose history is unrelated to the remote's.
                clonePath.deleteRecursively()
                throw e
            }
            _team.value = GitConfigStore.open(clonePath, url, author, credentials)
            if (url.isNotBlank()) DesktopConfigRepoPrefs.saveRepoUrl(url)
            _status.value = ConfigRepoStatus.Connected(url.ifBlank { "(local only)" }, clonePath.path)
        }.onFailure { _status.value = ConfigRepoStatus.Error(it.message ?: "Initialize failed") }
            .map {}
    }

    private fun scaffold(url: String, credentials: org.eclipse.jgit.transport.CredentialsProvider?) {
        Git.init().setInitialBranch(MAIN).setDirectory(clonePath).call().use { git ->
            File(clonePath, "config.json").writeText(SCAFFOLD_CONFIG)
            KIND_DIRS.forEach { dir ->
                File(clonePath, dir).mkdirs()
                File(clonePath, "$dir/.gitkeep").writeText("")
            }
            File(clonePath, "README.md").writeText(SCAFFOLD_README)
            git.add().addFilepattern(".").call()
            git.commit().setMessage("Initialize Alohomora config").setAuthor(author).call()
            if (url.isNotBlank()) {
                git.remoteAdd().setName(ORIGIN).setUri(URIish(url)).call()
                val results = git.push()
                    .setRemote(ORIGIN)
                    .setRefSpecs(RefSpec("refs/heads/$MAIN:refs/heads/$MAIN"))
                    .also { if (credentials != null) it.setCredentialsProvider(credentials) }
                    .call()
                // JGit reports a rejected ref in the result instead of throwing, so a remote that
                // already has a main line would otherwise "initialize" successfully against it.
                val rejected = results.flatMap { it.remoteUpdates }.filter { it.status !in PUSH_OK }
                check(rejected.isEmpty()) {
                    val detail = rejected.joinToString { "${it.remoteName}: ${it.status}" }
                    "That repository already has content ($detail). Join it instead of starting a new one."
                }
            }
        }
    }

    private fun saveToken(url: String, token: String?) {
        if (token.isNullOrBlank()) return
        val host = ConfigRepoCredentials.hostOf(url) ?: return
        ConfigRepoCredentials.savePat(host, token)
    }

    /**
     * Credentials for [url], preferring the just-typed [token] over the keychain so a working connect
     * never depends on the keychain round-trip (which is best-effort and may be unavailable). SSH URLs
     * return null and authenticate through the registered ssh session factory / agent.
     */
    private fun credentialsFor(
        url: String,
        token: String?,
    ): org.eclipse.jgit.transport.CredentialsProvider? {
        if (!url.startsWith("http", ignoreCase = true)) return null
        val effective = token?.takeIf { it.isNotBlank() }
            ?: ConfigRepoCredentials.hostOf(url)?.let { ConfigRepoCredentials.loadPat(it) }
            ?: return null
        return ConfigRepoCredentials.providerForToken(effective)
    }

    suspend fun sync(): SyncStatus {
        val team = _team.value ?: run {
            _syncStatus.value = SyncStatus.NotConnected
            return SyncStatus.NotConnected
        }
        // A fetch can fail (transport/auth); surface it as an Error status rather than throwing.
        return runCatching { team.sync() }
            .onSuccess { _syncStatus.value = it }
            .onFailure { _status.value = ConfigRepoStatus.Error(it.message ?: "Sync failed") }
            .getOrDefault(_syncStatus.value)
    }

    fun disconnect() {
        closeTeam()
        _team.value = null
        _syncStatus.value = SyncStatus.NotConnected
        _status.value = ConfigRepoStatus.Disconnected
        DesktopConfigRepoPrefs.saveRepoUrl(null)
    }

    fun setDeveloperMode(enabled: Boolean) {
        _developerMode.value = enabled
        DesktopConfigRepoPrefs.saveDeveloperMode(enabled)
    }

    /** The working-tree directory, for "reveal in terminal" / open-in-file-manager. */
    fun repoDir(): File = clonePath

    fun shutdown() {
        scope.cancel()
        closeTeam()
    }

    private fun closeTeam() {
        _team.value?.close()
        // A closed store must not stay reachable through the facade, least of all once its directory
        // has been moved aside.
        _team.value = null
    }

    private fun isCloneOf(url: String): Boolean =
        File(clonePath, DOT_GIT).exists() && originOf(clonePath)?.sameRemote(url) == true

    /**
     * Renames a non-empty clone path to a timestamped sibling, never deletes it: it can hold a
     * local-only repo's unshared work or another team's config. Not `.git`-gated, because `git init`
     * followed by `add .` would just as happily commit stray files from a half-finished clone.
     */
    private fun moveCloneAside() {
        if (!clonePath.exists() || clonePath.list().isNullOrEmpty()) return
        val parent = clonePath.absoluteFile.parentFile
        val stamp = LocalDateTime.now().format(ASIDE_STAMP)
        var target = File(parent, "${clonePath.name}.previous-$stamp")
        var n = 1
        while (target.exists()) target = File(parent, "${clonePath.name}.previous-$stamp-${n++}")
        Files.move(clonePath.toPath(), target.toPath())
    }

    companion object {
        private const val DOT_GIT = ".git"
        private val ASIDE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
        private val PUSH_OK = setOf(RemoteRefUpdate.Status.OK, RemoteRefUpdate.Status.UP_TO_DATE)
        private const val MAIN = "main"
        private const val ORIGIN = "origin"
        private val KIND_DIRS = listOf("journeys", "mocks", "deeplinks")

        private val SCAFFOLD_CONFIG = """
            {
              "schemaVersion": 1,
              "marker": "alohomora-config"
            }
        """.trimIndent() + "\n"

        private val SCAFFOLD_README = """
            # Alohomora config

            Version-controlled journeys, mock sets, and deep links for the team.
            Managed by the Alohomora desktop app. Protect the `main` branch in your forge so changes
            arrive through review.
        """.trimIndent() + "\n"

        /** The clone's `origin` URL, or null for a local-only repo or an unreadable one. */
        private fun originOf(dir: File): String? = runCatching {
            Git.open(dir).use { it.repository.config.getString("remote", ORIGIN, "url") }
        }.getOrNull()

        /**
         * Same remote modulo the spellings a user pastes interchangeably (trailing `/`, `.git`).
         * Deliberately no SSH/HTTPS equivalence: a false "different" only costs a fresh clone, a false
         * "same" is the bug this guards against.
         */
        private fun String.sameRemote(other: String): Boolean = normalizeRemote(this) == normalizeRemote(other)

        private fun normalizeRemote(url: String) = url.trim().trimEnd('/').removeSuffix(".git").trimEnd('/')

        private fun defaultAuthor(): PersonIdent {
            val user = System.getProperty("user.name")?.ifBlank { null } ?: "alohomora"
            return PersonIdent(user, "$user@localhost")
        }
    }
}
