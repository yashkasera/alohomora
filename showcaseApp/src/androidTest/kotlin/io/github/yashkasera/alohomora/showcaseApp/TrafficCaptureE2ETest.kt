package io.github.yashkasera.alohomora.showcaseApp

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.Traffic
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A real request, captured by the Ktor plugin the app installs, read back from the console.
 *
 * Needs network: the showcase app calls `https://jsonplaceholder.typicode.com` and nothing here
 * stubs it. Stubbing would remove the only thing this test proves.
 */
@RunWith(AndroidJUnit4::class)
class TrafficCaptureE2ETest : ShowcaseE2ETest() {

    @Test
    fun refreshedPostsRequestIsCapturedOnTheTrafficScreen() {
        compose.launchShowcaseApp()
        compose.refreshPosts()

        compose.openConsole()
        compose.openModule("Traffic")

        compose.awaitNode(capturedGet(path = "/posts", status = "200"), NETWORK_TIMEOUT_MILLIS)
    }

    /**
     * `PostsApi.fetchAuthorStatus` hits a path jsonplaceholder does not implement, so every refresh
     * produces exactly one 404 alongside the successful fetch. Deliberate fixture data, not a flake
     * — see the KDoc on that method.
     */
    @Test
    fun theAuthorLookupIsCapturedAsAFailedRequest() {
        compose.launchShowcaseApp()
        compose.refreshPosts()

        compose.openConsole()
        compose.openModule("Traffic")

        compose.awaitNode(capturedGet(path = "/author", status = "404"), NETWORK_TIMEOUT_MILLIS)
    }

    /**
     * Matched by content, not by `Traffic.item(id)`: the entry's id is minted by the inspector and
     * never surfaces in the UI. The row is a clickable `Card`, so its semantics subtree is merged
     * and host, method, path and status are all readable off the one node.
     *
     * Method and status are matched exactly rather than as substrings. The row also renders the
     * duration as `"${millis}ms"`, so a request that happened to take 200 ms would satisfy a
     * substring match for status `200`. [path] stays a substring because it carries the query.
     */
    private fun capturedGet(path: String, status: String) =
        hasText("jsonplaceholder.typicode.com") and
            hasText("GET") and
            hasText(status) and
            hasText(path, substring = true) and
            hasAnyAncestor(hasTestTag(Traffic.LIST))
}
