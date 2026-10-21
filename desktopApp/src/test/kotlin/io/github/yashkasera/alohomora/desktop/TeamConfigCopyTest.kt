package io.github.yashkasera.alohomora.desktop

import io.github.yashkasera.alohomora.desktop.domain.config.SyncStatus
import io.github.yashkasera.alohomora.desktop.presentation.model.TeamConfigCopy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The Team-config copy is the last line that undoes the domain's "hide git" promise if it leaks raw
 * drift counts or JGit text. These assert the translation, not the exact wording — only the facts a
 * non-engineer must get (which direction to act, what went wrong) and that no git jargon survives.
 */
class TeamConfigCopyTest {

    @Test
    fun `sync summary is absent when not connected`() {
        assertNull(TeamConfigCopy.syncSummary(SyncStatus.NotConnected))
    }

    @Test
    fun `sync summary reads as in sync when up to date`() {
        assertEquals("You're in sync with your team.", TeamConfigCopy.syncSummary(SyncStatus.UpToDate(0L)))
    }

    @Test
    fun `sync summary explains behind count as updates to pull`() {
        val text = TeamConfigCopy.syncSummary(SyncStatus.Drift(behind = 2, ahead = 0))!!
        assertTrue("2 updates" in text, text)
        assertTrue("Sync" in text, text)
        assertNoGitJargon(text)
    }

    @Test
    fun `sync summary singularises a single update`() {
        val text = TeamConfigCopy.syncSummary(SyncStatus.Drift(behind = 1, ahead = 0))!!
        assertTrue("1 update" in text, text)
        assertTrue("1 updates" !in text, text)
    }

    @Test
    fun `sync summary explains ahead count as changes waiting to share`() {
        val text = TeamConfigCopy.syncSummary(SyncStatus.Drift(behind = 0, ahead = 3))!!
        assertTrue("3 changes" in text, text)
        assertTrue("share" in text, text)
    }

    @Test
    fun `sync summary covers both directions`() {
        val text = TeamConfigCopy.syncSummary(SyncStatus.Drift(behind = 2, ahead = 1))!!
        assertTrue("2 updates" in text, text)
        assertTrue("1 change" in text, text)
    }

    @Test
    fun `sync summary falls back to in sync when both counts are zero`() {
        assertEquals(
            "You're in sync with your team.",
            TeamConfigCopy.syncSummary(SyncStatus.Drift(behind = 0, ahead = 0)),
        )
    }

    @Test
    fun `friendly error falls back to generic for null or blank`() {
        val generic = TeamConfigCopy.friendlyError(null)
        assertEquals(generic, TeamConfigCopy.friendlyError("   "))
        assertTrue("Developer mode" in generic, generic)
    }

    @Test
    fun `friendly error maps an auth failure to an access hint`() {
        val text = TeamConfigCopy.friendlyError("org.eclipse.jgit: not authorized (401)")
        assertTrue("access token" in text, text)
        assertNoGitJargon(text)
    }

    @Test
    fun `friendly error maps a missing repo`() {
        val text = TeamConfigCopy.friendlyError("Repository not found (404)")
        assertTrue("link" in text, text)
    }

    @Test
    fun `friendly error maps a network failure`() {
        val text = TeamConfigCopy.friendlyError("java.net.UnknownHostException: could not resolve host")
        assertTrue("internet connection" in text, text)
    }

    @Test
    fun `friendly error points an unset repo at preferences`() {
        val text = TeamConfigCopy.friendlyError("Team config is not connected.")
        assertTrue("Team config" in text, text)
        assertTrue("Preferences" in text, text)
    }

    @Test
    fun `friendly error falls back to generic for an unrecognised message`() {
        val text = TeamConfigCopy.friendlyError("some totally opaque internal failure")
        assertTrue("Developer mode" in text, text)
    }

    private fun assertNoGitJargon(text: String) {
        val lower = text.lowercase()
        listOf("jgit", "clone", "git@", "branch", "commit", "remote", "push").forEach {
            assertTrue(it !in lower, "leaked git term '$it' in: $text")
        }
    }
}
