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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.eclipse.jgit.api.Git

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
}

private suspend fun ConfigRepoManager.list(kind: ConfigKind<*>) =
    teamStore!!.list(kind)
