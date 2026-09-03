package io.github.yashkasera.alohomora.device

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import io.github.yashkasera.alohomora.presentation.navigation.Routes
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.Chrome
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.Database
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * The Database inspector, driven against a real on-device SQLite file.
 *
 * The fixture is a database this test creates, **not** Alohomora's own `alohomora.db`. The Android
 * accessor filters that name out of `listDatabases()` on purpose — the inspector exists to show the
 * *host app's* data, and listing the debugger's own tables alongside it is noise the console
 * deliberately hides (`IosDatabaseInspectorTest` asserts the same exclusion on the other platform).
 * So there is no path by which seeded traffic reaches this screen; a separate database is the only
 * honest fixture.
 *
 * Created and deleted in `@Before` rather than `@After`, for the reason [ConsoleTestRule] gives:
 * a test that fails mid-way skips its own cleanup, and the file outlives the process.
 */
class DatabaseScreenTest {

    @get:Rule(order = 0)
    val console = ConsoleTestRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    @Before
    fun createFixtureDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(FIXTURE_DB)
        context.openOrCreateDatabase(FIXTURE_DB, Context.MODE_PRIVATE, null).use { db ->
            db.execSQL("CREATE TABLE $FIXTURE_TABLE (id INTEGER PRIMARY KEY, title TEXT NOT NULL)")
            db.execSQL("INSERT INTO $FIXTURE_TABLE (id, title) VALUES (1, '$FIRST_ROW')")
            db.execSQL("INSERT INTO $FIXTURE_TABLE (id, title) VALUES (2, 'second-note')")
        }
    }

    @Test
    fun theSelectorListsTheAppsDatabases() {
        compose.launchConsole(Routes.Database)
        compose.awaitTag(Database.SELECTOR)
        compose.onNodeWithTag(Database.SELECTOR).performClick()
        compose.awaitTag(Database.SELECTOR_SHEET)

        compose.onNodeWithTag(Database.SELECTOR_SHEET).assertIsDisplayed()
        compose.onNodeWithTag(Database.database(FIXTURE_DB)).assertIsDisplayed()
    }

    @Test
    fun tableChipsAppearForTheSelectedDatabase() {
        openFixtureDatabase()

        compose.onNodeWithTag(Database.table(FIXTURE_TABLE)).assertIsDisplayed()
    }

    @Test
    fun browseShowsTheRowsOfTheSelectedTable() {
        openFixtureDatabase()
        compose.onNodeWithTag(Database.tab("BROWSE")).performClick()
        compose.awaitTag(Database.BROWSE)

        compose.awaitText(FIRST_ROW)
    }

    @Test
    fun schemaShowsTheColumnsOfTheSelectedTable() {
        openFixtureDatabase()
        compose.onNodeWithTag(Database.tab("SCHEMA")).performClick()
        compose.awaitTag(Database.SCHEMA)

        compose.awaitText("COLUMNS")
        compose.awaitText("title")
    }

    @Test
    fun tabsSwitchTheContentBelowThem() {
        openFixtureDatabase()

        // BROWSE is the landing tab — the inspector opens on the data itself.
        compose.onNodeWithTag(Database.BROWSE).assertIsDisplayed()

        compose.onNodeWithTag(Database.tab("QUERY")).performClick()
        compose.awaitTag(Database.QUERY_RESULT)
        compose.onNodeWithTag(Database.QUERY_RESULT).assertIsDisplayed()

        compose.onNodeWithTag(Database.tab("SCHEMA")).performClick()
        compose.awaitTag(Database.SCHEMA)
        compose.onNodeWithTag(Database.SCHEMA).assertIsDisplayed()
    }

    @Test
    fun aValidQueryReportsAStatusAndReturnsRows() {
        openFixtureDatabase()
        runQuery("SELECT title FROM $FIXTURE_TABLE")

        compose.awaitTag(Database.QUERY_STATUS)
        // The status chip carries only an elapsed time and an untitled icon, so success is read
        // off the result body: a failed query publishes an empty `TableData`, which renders as
        // "No columns" instead of the row.
        compose.awaitText(FIRST_ROW)
    }

    @Test
    fun anInvalidQueryReportsAFailure() {
        openFixtureDatabase()
        runQuery("SELECT * FROM no_such_table")

        compose.awaitTag(Database.QUERY_STATUS)
        compose.awaitText("No columns")
    }

    @Test
    fun theTopBarNamesTheInspector() {
        compose.launchConsole(Routes.Database)

        compose.onNodeWithTag(Chrome.TOP_BAR_TITLE).assertTextEquals("Database")
    }

    /**
     * Opens the console on the Database screen and selects the fixture explicitly.
     *
     * The view model auto-selects the first database it finds, which would be enough if the
     * fixture were the only one — but the app's databases directory is process-wide state shared
     * with every other test in the run, so "first" is not a guarantee worth resting on.
     */
    private fun openFixtureDatabase() {
        compose.launchConsole(Routes.Database)
        compose.awaitTag(Database.SELECTOR)
        compose.onNodeWithTag(Database.SELECTOR).performClick()
        compose.awaitTag(Database.database(FIXTURE_DB))
        compose.onNodeWithTag(Database.database(FIXTURE_DB)).performClick()
        compose.awaitTag(Database.table(FIXTURE_TABLE))
    }

    private fun runQuery(sql: String) {
        // BROWSE is the landing tab, so the editor only composes once QUERY is opened.
        compose.onNodeWithTag(Database.tab("QUERY")).performClick()
        compose.awaitTag(Database.QUERY_EDITOR)
        compose.onTextFieldIn(Database.QUERY_EDITOR).performTextInput(sql)
        compose.waitForIdle()
        compose.onNodeWithTag(Database.QUERY_RUN).performClick()
    }

    private companion object {
        const val FIXTURE_DB = "alohomora_device_fixture.db"
        const val FIXTURE_TABLE = "notes"
        const val FIRST_ROW = "first-note"
    }
}
