package io.github.yashkasera.alohomora.desktop.data.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ConfigRepoCredentialsTest {

    @Test
    fun `hostOf parses https and scp-style urls`() {
        assertEquals("github.com", ConfigRepoCredentials.hostOf("https://github.com/acme/config.git"))
        assertEquals("gitlab.com", ConfigRepoCredentials.hostOf("git@gitlab.com:acme/config.git"))
    }

    @Test
    fun `providerFor returns null for an ssh url`() {
        // SSH uses the agent key, never a stored PAT.
        assertNull(ConfigRepoCredentials.providerFor("git@github.com:acme/config.git"))
    }

    @Test
    fun `providerFor returns null for https without a stored token`() {
        // A host that will not have a keychain entry.
        assertNull(ConfigRepoCredentials.providerFor("https://no-such-host.invalid/acme/config.git"))
    }
}
