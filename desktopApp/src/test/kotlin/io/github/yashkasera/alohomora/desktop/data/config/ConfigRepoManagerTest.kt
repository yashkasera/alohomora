package io.github.yashkasera.alohomora.desktop.data.config

import io.github.yashkasera.alohomora.common.journey.JourneyDefinition
import io.github.yashkasera.alohomora.common.journey.JourneyStep
import io.github.yashkasera.alohomora.desktop.data.devtools.DesktopConfigRepoPrefs
import io.github.yashkasera.alohomora.desktop.domain.config.ConfigKind
import io.github.yashkasera.alohomora.desktop.domain.config.SyncStatus
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.URIish

class ConfigRepoManagerTest {

    private lateinit var root: File
    private lateinit var bare: File

    @BeforeTest
    fun setUp() {
        root = Files.createTempDirectory("alohomora-repomgr-test").toFile()
        bare = File(root, "remote.git")
        Git.init().setBare(true).setInitialBranch("main").setDirectory(bare).call().close()
    }

    @AfterTest
    fun tearDown() {
        // Do not leave the test's repo URL in the developer's real user prefs.
        DesktopConfigRepoPrefs.saveRepoUrl(null)
        root.deleteRecursively()
    }

    private fun journey() = JourneyDefinition(
        id = "j1",
        name = "Checkout",
        steps = listOf(JourneyStep(id = "s1", eventName = "login")),
    )

    @Test
    fun `initialize scaffolds an empty remote and connects`() = runBlocking {
        val manager = ConfigRepoManager(clonePath = File(root, "init-clone"))
        val result = manager.initialize(bare.absolutePath)

        assertTrue(result.isSuccess)
        assertTrue(manager.status.value is ConfigRepoStatus.Connected)
        assertNotNull(manager.teamStore)

        Git.open(bare).use { g ->
            assertTrue(g.branchList().call().any { it.name.endsWith("main") })
        }
        manager.shutdown()
    }

    @Test
    fun `initialize with a blank url creates a local-only repo`() = runBlocking {
        val manager = ConfigRepoManager(clonePath = File(root, "local-only"))
        val result = manager.initialize("")

        assertTrue(result.isSuccess)
        assertTrue(manager.status.value is ConfigRepoStatus.Connected)
        assertNotNull(manager.teamStore)
        assertTrue(File(root, "local-only/.git").exists())
        assertTrue(File(root, "local-only/config.json").exists())
        manager.shutdown()
    }

    @Test
    fun `connect clones an initialized repo and share pushes a proposal`() = runBlocking {
        ConfigRepoManager(clonePath = File(root, "init-clone")).apply {
            initialize(bare.absolutePath)
            shutdown()
        }

        val manager = ConfigRepoManager(clonePath = File(root, "connect-clone"))
        assertTrue(manager.connect(bare.absolutePath).isSuccess)
        assertTrue(manager.status.value is ConfigRepoStatus.Connected)
        assertTrue(manager.list(ConfigKind.Journeys).isEmpty())

        manager.teamStore!!.shareWithTeam(ConfigKind.Journeys, journey())
        Git.open(bare).use { g ->
            assertTrue(g.branchList().call().any { it.name.contains("proposal/") })
        }

        assertEquals(SyncStatus.UpToDate::class, manager.sync()::class)
        manager.shutdown()
    }

    @Test
    fun `connecting a different repo clones it fresh and moves the old clone aside`() = runBlocking {
        val teamA = seededRemote("team-a", "a.txt")
        val teamB = seededRemote("team-b", "b.txt")
        val clone = File(root, "clone")

        val manager = ConfigRepoManager(clonePath = clone)
        assertTrue(manager.connect(teamA.absolutePath).isSuccess)
        manager.disconnect()
        assertTrue(manager.connect(teamB.absolutePath).isSuccess)

        assertEquals(teamB.absolutePath, originOf(clone))
        assertTrue(File(clone, "b.txt").exists())
        assertFalse(File(clone, "a.txt").exists(), "team A's files must not be in team B's clone")

        // Team A's clone is kept, not deleted.
        val aside = asideDirs()
        assertEquals(1, aside.size, "aside: $aside")
        assertEquals(teamA.absolutePath, originOf(aside.single()))

        // And sharing now reaches team B, not team A.
        val proposal = manager.teamStore!!.shareWithTeam(ConfigKind.Journeys, journey())
        assertTrue(branchesOf(teamB).any { it.endsWith(proposal.branch) })
        assertFalse(branchesOf(teamA).any { it.contains("proposal/") })
        manager.shutdown()
    }

    @Test
    fun `reconnecting the same repo reuses the clone`() = runBlocking {
        val clone = File(root, "clone")
        val manager = ConfigRepoManager(clonePath = clone)
        assertTrue(manager.initialize(bare.absolutePath).isSuccess)
        manager.disconnect()

        assertTrue(manager.connect(bare.absolutePath + "/").isSuccess)
        assertTrue(asideDirs().isEmpty(), "a trailing slash is the same remote")
        assertTrue(manager.status.value is ConfigRepoStatus.Connected)
        manager.shutdown()
    }

    @Test
    fun `initialize never carries an existing clone's history to the new remote`() = runBlocking {
        val teamA = seededRemote("team-a", "a.txt")
        val clone = File(root, "clone")
        val manager = ConfigRepoManager(clonePath = clone)
        assertTrue(manager.connect(teamA.absolutePath).isSuccess)
        manager.disconnect()

        assertTrue(manager.initialize(bare.absolutePath).isSuccess)

        Git.open(bare).use { g ->
            val log = g.log().call().map { it.shortMessage }
            assertEquals(listOf("Initialize Alohomora config"), log, "only the scaffold may reach the new remote")
        }
        assertFalse(File(clone, "a.txt").exists())
        assertEquals(teamA.absolutePath, originOf(asideDirs().single()))
        manager.shutdown()
    }

    @Test
    fun `initialize refuses a remote that already has content and leaves no clone behind`() = runBlocking {
        val teamA = seededRemote("team-a", "a.txt")
        val clone = File(root, "clone")
        val manager = ConfigRepoManager(clonePath = clone)

        val result = manager.initialize(teamA.absolutePath)

        assertTrue(result.isFailure)
        assertTrue(manager.status.value is ConfigRepoStatus.Error)
        assertEquals(null, manager.teamStore)
        assertFalse(clone.exists(), "the half-made scaffold must not be reopened by a later Connect")
        Git.open(teamA).use { g -> assertEquals(listOf("seed"), g.log().call().map { it.shortMessage }) }
        manager.shutdown()
    }

    /** A bare remote whose main holds one commit adding [file]. */
    private fun seededRemote(name: String, file: String): File {
        val remote = File(root, "$name.git")
        Git.init().setBare(true).setInitialBranch("main").setDirectory(remote).call().close()
        val work = File(root, "$name-seed")
        Git.init().setInitialBranch("main").setDirectory(work).call().use { g ->
            File(work, file).writeText(name)
            g.add().addFilepattern(".").call()
            g.commit().setMessage("seed").setAuthor(PersonIdent("Test", "test@example.com")).call()
            g.remoteAdd().setName("origin").setUri(URIish(remote.absolutePath)).call()
            g.push().setRemote("origin").setRefSpecs(RefSpec("refs/heads/main:refs/heads/main")).call()
        }
        return remote
    }

    private fun asideDirs(): List<File> =
        root.listFiles().orEmpty().filter { it.name.startsWith("clone.previous-") }

    private fun originOf(dir: File): String? =
        Git.open(dir).use { it.repository.config.getString("remote", "origin", "url") }

    private fun branchesOf(remote: File): List<String> =
        Git.open(remote).use { g -> g.branchList().call().map { it.name } }
}

private suspend fun ConfigRepoManager.list(kind: ConfigKind<*>) =
    teamStore!!.list(kind)
