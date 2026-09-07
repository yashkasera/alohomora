package io.github.yashkasera.alohomora.desktop.data.config

import io.github.yashkasera.alohomora.common.deeplink.DeepLinkDef
import io.github.yashkasera.alohomora.common.journey.JourneyDefinition
import io.github.yashkasera.alohomora.common.journey.JourneyStep
import io.github.yashkasera.alohomora.desktop.domain.config.ConfigKind
import io.github.yashkasera.alohomora.desktop.domain.config.SyncStatus
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.URIish

class GitConfigStoreTest {

    private lateinit var root: File
    private lateinit var bare: File
    private lateinit var clone: File
    private lateinit var store: GitConfigStore
    private val author = PersonIdent("Test", "test@example.com")

    @BeforeTest
    fun setUp() {
        root = Files.createTempDirectory("alohomora-git-test").toFile()
        bare = File(root, "remote.git")
        clone = File(root, "clone")

        // A bare remote with a single main commit, so the store's clone has a real upstream. Prime by
        // initialising a normal repo and pushing main — cloning an empty bare throws in JGit.
        Git.init().setBare(true).setInitialBranch("main").setDirectory(bare).call().close()
        val prime = File(root, "prime")
        Git.init().setInitialBranch("main").setDirectory(prime).call().use { g ->
            File(prime, "config.json").writeText("""{ "marker": "alohomora-config" }""")
            g.add().addFilepattern(".").call()
            g.commit().setMessage("init").setAuthor(author).call()
            g.remoteAdd().setName("origin").setUri(URIish(bare.absolutePath)).call()
            g.push().setRemote("origin").setRefSpecs(RefSpec("refs/heads/main:refs/heads/main")).call()
        }

        store = GitConfigStore.clone(bare.absolutePath, clone, author)
    }

    @AfterTest
    fun tearDown() {
        store.close()
        root.deleteRecursively()
    }

    private fun journey() = JourneyDefinition(
        id = "j1",
        name = "Checkout flow",
        steps = listOf(JourneyStep(id = "s1", eventName = "login")),
    )

    @Test
    fun `share pushes a proposal branch and returns the generic floor result`() = runTest {
        val proposal = store.shareWithTeam(ConfigKind.Journeys, journey())

        assertTrue(proposal.branch.startsWith("proposal/"), "branch was ${proposal.branch}")
        // A local file remote matches no forge adapter, so the floor applies.
        assertEquals("review", proposal.reviewNoun)
        assertEquals(null, proposal.reviewUrl)
        assertEquals(false, proposal.autoCreated)

        // The branch reached the remote.
        Git.open(bare).use { g ->
            val branches = g.branchList().call().map { it.name }
            assertTrue(branches.any { it.endsWith(proposal.branch) }, "remote branches: $branches")
        }
    }

    @Test
    fun `share leaves the working tree back on main without the proposed file`() = runTest {
        store.shareWithTeam(ConfigKind.Journeys, journey())

        Git.open(clone).use { g ->
            assertEquals("main", g.repository.branch)
        }
        // The artifact lives only on the proposal branch, not on main.
        assertTrue(store.list(ConfigKind.Journeys).isEmpty())
    }

    @Test
    fun `sync reports up to date on a fresh clone`() = runTest {
        assertEquals(SyncStatus.UpToDate::class, store.sync()::class)
    }

    @Test
    fun `share returns to main and leaves it clean when the push fails`() = runTest {
        // Break the remote so the push inside share fails after the branch/commit steps.
        bare.deleteRecursively()

        val failed = runCatching { store.shareWithTeam(ConfigKind.Journeys, journey()) }.isFailure
        assertTrue(failed, "expected the push to fail")

        Git.open(clone).use { g -> assertEquals("main", g.repository.branch) }
        // The proposed file was written on the branch, never stranded on main.
        assertTrue(store.list(ConfigKind.Journeys).isEmpty())
    }

    @Test
    fun `sharing a deep link commits a regenerated catalog readme`() = runTest {
        val def = DeepLinkDef(id = "d1", name = "KYC verify", module = "kyc", uriTemplate = "app://kyc/verify")
        val proposal = store.shareWithTeam(ConfigKind.DeepLinks, def)

        // The README rides on the proposal branch; check it out to inspect.
        Git.open(clone).use { g -> g.checkout().setName(proposal.branch).call() }
        val readme = File(clone, "deeplinks/README.md")
        assertTrue(readme.exists())
        assertTrue(readme.readText().contains("KYC verify"))
    }
}
