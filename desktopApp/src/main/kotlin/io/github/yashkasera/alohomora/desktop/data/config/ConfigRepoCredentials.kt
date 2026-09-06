package io.github.yashkasera.alohomora.desktop.data.config

import com.github.javakeyring.Keyring
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.transport.sshd.SshdSessionFactory

/**
 * Git auth for the config repo: SSH-agent transport (registered once) and an HTTPS personal access
 * token stored in the OS keychain — never in `java.util.prefs`, which is effectively plaintext.
 *
 * The PAT is keyed by host, so one token serves every repo on a forge. SSH URLs use the user's agent
 * key and need no stored secret.
 */
object ConfigRepoCredentials {
    private const val SERVICE = "alohomora-config"

    @Volatile
    private var sshRegistered = false

    /** Backed by the OS keychain (macOS Keychain / Windows Credential Store / libsecret). */
    private val keyring: Keyring? by lazy { runCatching { Keyring.create() }.getOrNull() }

    /** Registers the Apache MINA sshd transport once, so `git@host:...` remotes resolve. */
    @Synchronized
    fun ensureSshRegistered() {
        if (sshRegistered) return
        sshRegistered = true
        runCatching { SshSessionFactory.setInstance(SshdSessionFactory()) }
    }

    fun savePat(host: String, token: String) {
        runCatching { keyring?.setPassword(SERVICE, host, token) }
    }

    fun loadPat(host: String): String? =
        runCatching { keyring?.getPassword(SERVICE, host) }.getOrNull()?.ifBlank { null }

    fun clearPat(host: String) {
        runCatching { keyring?.deletePassword(SERVICE, host) }
    }

    fun hostOf(url: String): String? =
        runCatching { URIish(url).host }.getOrNull()?.ifBlank { null }
            ?: runCatching { java.net.URI(url).host }.getOrNull()?.ifBlank { null }

    /**
     * A credentials provider for an HTTPS URL with a stored PAT, else null — SSH and anonymous/cached
     * HTTPS need none.
     */
    fun providerFor(url: String): CredentialsProvider? {
        if (!url.startsWith("http", ignoreCase = true)) return null
        val host = hostOf(url) ?: return null
        val token = loadPat(host) ?: return null
        return UsernamePasswordCredentialsProvider(token, "")
    }
}
