package io.github.yashkasera.alohomora.showcaseApp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.yashkasera.alohomora.showcaseApp.data.db.PostDao
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.Database
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

/**
 * The showcase app's own Room database, inspected from the console.
 *
 * `PlatformDatabaseAccessor.listDatabases()` filters `alohomora.db` out of the list on purpose — the
 * inspector exists to show the *host app's* data — so `android_sample.db` is what the selector
 * offers. `:alohomora`'s own device test has to create a throwaway SQLite file to have anything to
 * select at all; here the fixture is the real database the app writes to.
 *
 * Needs network: the `posts` table is empty until a refresh fills it, and the database file itself
 * does not exist until Room first opens it.
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseE2ETest : ShowcaseE2ETest() {

    @Before
    fun refreshPostsIntoTheDatabase() {
        compose.launchShowcaseApp()
        compose.refreshPosts()
        awaitPostsWritten()
    }

    @Test
    fun theAppsOwnDatabaseIsListedInTheSelector() {
        compose.openConsole()
        compose.openModule("Database")
        compose.awaitTag(Database.SELECTOR)
        compose.onNodeWithTag(Database.SELECTOR).performClick()
        compose.awaitTag(Database.SELECTOR_SHEET)

        compose.onNodeWithTag(Database.database(DATABASE)).assertIsDisplayed()
    }

    @Test
    fun thePostsTableIsListedForTheSelectedDatabase() {
        openPostsDatabase()

        compose.onNodeWithTag(Database.table(TABLE)).assertIsDisplayed()
    }

    @Test
    fun schemaShowsTheColumnsOfThePostsTable() {
        openPostsDatabase()
        compose.onNodeWithTag(Database.tab("SCHEMA")).performClick()
        compose.awaitTag(Database.SCHEMA)

        // `PostEntity`'s columns, read back out of SQLite rather than out of Room's annotations.
        compose.awaitText("COLUMNS")
        compose.awaitText("updatedAtEpochMillis")
    }

    @Test
    fun browseRendersThePostsTable() {
        openPostsDatabase()
        compose.onNodeWithTag(Database.tab("BROWSE")).performClick()

        compose.awaitTag(Database.BROWSE)
        compose.onNodeWithTag(Database.BROWSE).assertIsDisplayed()
    }

    /**
     * Proves the table has rows without asserting on anything jsonplaceholder returns.
     *
     * The projection is a literal this test owns, so the row's *content* is fixed while its
     * *existence* still depends on the refresh having written posts. One column also keeps the value
     * on screen, which a `title` five columns wide would not be.
     *
     * Scoped to `QUERY_RESULT` because the query text is rendered in the editor directly above — an
     * unscoped text match would find the query itself and pass against an empty table.
     */
    @Test
    fun aQueryConfirmsTheRefreshWrotePostRows() {
        openPostsDatabase()

        // BROWSE is the landing tab, so the query editor only composes once QUERY is opened.
        compose.onNodeWithTag(Database.tab("QUERY")).performClick()
        compose.awaitTag(Database.QUERY_EDITOR)
        compose.onTextFieldIn(Database.QUERY_EDITOR)
            .performTextInput("SELECT '$MARKER' AS marker FROM $TABLE LIMIT 1")
        compose.waitForIdle()
        compose.onNodeWithTag(Database.QUERY_RUN).performClick()

        compose.awaitNode(
            hasText(MARKER, substring = true) and
                hasAnyAncestor(hasTestTag(Database.QUERY_RESULT)),
            useUnmergedTree = true,
        )
    }

    /**
     * Waits on the app's own DAO rather than on a row in the posts list.
     *
     * The list is lazy and the preferences and WebView cards occupy the first screen, so post 1 is
     * routinely below the fold and absent from the semantics tree even after the write has landed.
     * The DAO is the thing the assertion actually depends on, and `GlobalContext` reaches it because
     * `AndroidApp` starts its own Koin container in the process these tests share.
     */
    private fun awaitPostsWritten() {
        val dao = GlobalContext.get().get<PostDao>()
        runBlocking {
            withTimeout(NETWORK_TIMEOUT_MILLIS.milliseconds) {
                dao.observePosts().first { it.isNotEmpty() }
            }
        }
    }

    /**
     * Selects the fixture explicitly rather than relying on the view model's auto-select.
     *
     * It picks the first database it finds, which would be enough if `android_sample.db` were the
     * only one — but the databases directory is process-wide and other suites in the same run can
     * create files there.
     */
    private fun openPostsDatabase() {
        compose.openConsole()
        compose.openModule("Database")
        compose.awaitTag(Database.SELECTOR)
        compose.onNodeWithTag(Database.SELECTOR).performClick()
        compose.awaitTag(Database.database(DATABASE))
        compose.onNodeWithTag(Database.database(DATABASE)).performClick()
        compose.awaitTag(Database.table(TABLE))
    }

    private companion object {
        const val DATABASE = "android_sample.db"
        const val TABLE = "posts"

        const val MARKER = "alohomora-e2e-row"
    }
}
