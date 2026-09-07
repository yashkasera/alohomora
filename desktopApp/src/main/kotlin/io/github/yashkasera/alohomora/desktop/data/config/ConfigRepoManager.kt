package io.github.yashkasera.alohomora.desktop.data.config

import io.github.yashkasera.alohomora.desktop.data.devtools.DesktopConfigRepoPrefs
import io.github.yashkasera.alohomora.desktop.domain.config.SyncStatus
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.transport.RefSpec
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
 * Transport-agnostic: credentials are null, so file and cached-HTTPS remotes work out of the box.
 * SSH-agent and HTTPS-PAT auth layer on here (the latter needs the OS keychain); neither is wired yet.
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

    init {
        ConfigRepoCredentials.ensureSshRegistered()
        // Reconnect silently to a previously connected clone on the next launch.
        val url = DesktopConfigRepoPrefs.loadRepoUrl()
        if (url != null && File(clonePath, DOT_GIT).exists()) {
            runCatching { GitConfigStore.open(clonePath, url, author, ConfigRepoCredentials.providerFor(url)) }
                .onSuccess {
                    _team.value = it
                    _status.value = ConfigRepoStatus.Connected(url, clonePath.path)
                }
        }
    }

    /**
     * Clones [url] into the clone path (or opens it if already cloned) and makes it the team store.
     * A non-blank [token] is stored in the OS keychain for the URL's host (HTTPS auth).
     */
    suspend fun connect(url: String, token: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            closeTeam()
            saveToken(url, token)
            val credentials = credentialsFor(url, token)
            val store = if (File(clonePath, DOT_GIT).exists()) {
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
     */
    suspend fun initialize(url: String, token: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            closeTeam()
            saveToken(url, token)
            val credentials = credentialsFor(url, token)
            clonePath.mkdirs()
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
                    git.push()
                        .setRemote(ORIGIN)
                        .setRefSpecs(RefSpec("refs/heads/$MAIN:refs/heads/$MAIN"))
                        .also { if (credentials != null) it.setCredentialsProvider(credentials) }
                        .call()
                }
            }
            _team.value = GitConfigStore.open(clonePath, url, author, credentials)
            if (url.isNotBlank()) DesktopConfigRepoPrefs.saveRepoUrl(url)
            _status.value = ConfigRepoStatus.Connected(url.ifBlank { "(local only)" }, clonePath.path)
        }.onFailure { _status.value = ConfigRepoStatus.Error(it.message ?: "Initialize failed") }
            .map {}
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
        val result = _team.value?.sync() ?: SyncStatus.NotConnected
        _syncStatus.value = result
        return result
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

    fun shutdown() = closeTeam()

    private fun closeTeam() {
        _team.value?.close()
    }

    companion object {
        private const val DOT_GIT = ".git"
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

        private fun defaultAuthor(): PersonIdent {
            val user = System.getProperty("user.name")?.ifBlank { null } ?: "alohomora"
            return PersonIdent(user, "$user@localhost")
        }
    }
}
