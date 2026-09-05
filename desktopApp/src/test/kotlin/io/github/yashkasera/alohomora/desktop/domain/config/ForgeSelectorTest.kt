package io.github.yashkasera.alohomora.desktop.domain.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ForgeSelectorTest {

    @Test
    fun `selects github for a github remote`() {
        assertEquals(GitHubForge, ForgeSelector.select("https://github.com/acme/config.git"))
    }

    @Test
    fun `selects gitlab for a gitlab remote`() {
        assertEquals(GitLabForge, ForgeSelector.select("git@gitlab.com:acme/config.git"))
    }

    @Test
    fun `an unrecognised remote degrades to the generic floor`() {
        assertEquals(GenericGitForge, ForgeSelector.select("https://git.internal.example/acme/config"))
        assertEquals(GenericGitForge, ForgeSelector.select("/tmp/local/bare"))
    }

    @Test
    fun `the generic floor sets no push options and returns no review url`() {
        assertTrue(GenericGitForge.pushOptions("b", "main").isEmpty())
        val review = GenericGitForge.parseReview("anything")
        assertEquals(null, review.url)
        assertEquals(false, review.autoCreated)
    }

    @Test
    fun `gitlab sets merge request push options targeting the main branch`() {
        val options = GitLabForge.pushOptions("proposal/x", "main")
        assertTrue(options.contains("merge_request.create"))
        assertTrue(options.contains("merge_request.target=main"))
    }

    @Test
    fun `github parses the pull request url out of push messages`() {
        val messages = """
            remote: Create a pull request for 'proposal/x' on GitHub by visiting:
            remote:   https://github.com/acme/config/pull/new/proposal/x
        """.trimIndent()
        assertEquals(
            "https://github.com/acme/config/pull/new/proposal/x",
            GitHubForge.parseReview(messages).url,
        )
    }

    @Test
    fun `review nouns drive the label per forge`() {
        assertEquals("pull request", GitHubForge.reviewNoun)
        assertEquals("merge request", GitLabForge.reviewNoun)
        assertEquals("review", GenericGitForge.reviewNoun)
    }
}
