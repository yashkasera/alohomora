package io.github.yashkasera.alohomora.devtools

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import io.github.yashkasera.alohomora.common.AppDatabaseInfo
import io.github.yashkasera.alohomora.data.db.AlohomoraDb
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory

/**
 * Tests the iOS Vault implementation against a real SQLite file.
 *
 * These methods all returned `emptyList()` before, so the Vault was silently blank on iOS. This
 * exercises the real driver with known inputs, which pins both the happy path and — more
 * importantly — the two safety rules copied from the Android implementation: the table allowlist
 * and the row-limit clamp.
 */
@OptIn(ExperimentalForeignApi::class)
class IosDatabaseInspectorTest {

    private lateinit var databasePath: String

    /**
     * A real Room instance.
     *
     * The inspector never reads it (app databases are opened by path), but the `expect`
     * constructor requires a non-null one — and building it here doubles as a check that Room
     * itself works on iOS.
     */
    private lateinit var alohomoraDb: AlohomoraDb

    /** Serves a single fixture database, standing in for the sandbox scan. */
    private class FixtureProvider(private val name: String, private val path: String) :
        DevToolsAppDatabaseProvider {
        override fun listDatabases() = listOf(AppDatabaseInfo(name = name, path = path))
        override fun resolvePath(name: String): String? = if (name == this.name) path else null
    }

    private fun inspector() = DevToolsDatabaseInspector(
        database = alohomoraDb,
        appDatabaseProvider = FixtureProvider("fixture.db", databasePath),
    )

    @BeforeTest
    fun setUp() {
        databasePath = NSTemporaryDirectory() + "aloho-vault-test-${randomSuffix()}.db"
        alohomoraDb = Room.databaseBuilder<AlohomoraDb>(
            name = NSTemporaryDirectory() + "aloho-room-${randomSuffix()}.db",
        ).setDriver(BundledSQLiteDriver()).build()
        BundledSQLiteDriver().open(databasePath).use { connection ->
            connection.execSQL(
                "CREATE TABLE posts (" +
                    "id INTEGER PRIMARY KEY NOT NULL, " +
                    "title TEXT NOT NULL, " +
                    "score REAL, " +
                    "payload BLOB, " +
                    "note TEXT DEFAULT 'none')",
            )
            connection.execSQL("CREATE INDEX index_posts_title ON posts(title)")
            connection.execSQL("INSERT INTO posts (id, title, score) VALUES (1, 'first', 1.5)")
            connection.execSQL("INSERT INTO posts (id, title, score) VALUES (2, 'second', NULL)")
            connection.execSQL("INSERT INTO posts (id, title, payload) VALUES (3, 'third', x'0011223344')")
        }
    }

    @AfterTest
    fun tearDown() {
        alohomoraDb.close()
        NSFileManager.defaultManager.removeItemAtPath(databasePath, null)
    }

    @Test
    fun `lists tables and full column schema`() {
        val schema = inspector().loadSchema("fixture.db")

        assertEquals(listOf("posts"), schema.tables)
        val posts = schema.schemas.single()
        assertEquals("id", posts.primaryKey)
        assertEquals(
            listOf("id", "title", "score", "payload", "note"),
            posts.columns.map { it.name },
        )

        val title = posts.columns.first { it.name == "title" }
        assertEquals("TEXT", title.type)
        assertTrue(title.notNull, "title is declared NOT NULL")

        val score = posts.columns.first { it.name == "score" }
        assertTrue(!score.notNull, "score is nullable")

        // dflt_value comes from a separate PRAGMA column and is easy to read off by one.
        assertEquals("'none'", posts.columns.first { it.name == "note" }.defaultValue)
        assertTrue(posts.indexes.contains("index_posts_title"), "indexes were ${posts.indexes}")
    }

    @Test
    fun `reads rows and renders every sqlite column type`() {
        val table = inspector().loadTable("fixture.db", "posts", limit = 100)

        assertEquals(listOf("id", "title", "score", "payload", "note"), table.columns)
        assertEquals(3, table.rows.size)

        val first = table.rows.first { it["id"] == "1" }
        assertEquals("first", first["title"])
        assertEquals("1.5", first["score"])

        // NULL must round-trip as null, not as the string "null".
        assertEquals(null, table.rows.first { it["id"] == "2" }["score"])

        // Blobs are summarised by size rather than streamed, matching Android.
        assertEquals("BLOB(5)", table.rows.first { it["id"] == "3" }["payload"])
    }

    @Test
    fun `rejects a table name that is not in the schema`() {
        // SQLite has no bind parameter for identifiers and backtick quoting is not escaping, so
        // the allowlist is the only defence against a wire-supplied name.
        val sqliteMaster = inspector().loadTable("fixture.db", "sqlite_master", limit = 10)
        assertEquals(emptyList(), sqliteMaster.rows)
        assertEquals(emptyList(), sqliteMaster.columns)

        val injection = inspector().loadTable("fixture.db", "posts` UNION SELECT * FROM posts --", 10)
        assertEquals(emptyList(), injection.rows)
    }

    @Test
    fun `clamps the row limit so a negative value cannot dump the table`() {
        // limit = -1 would become `LIMIT -1`, which SQLite treats as unbounded.
        val clamped = inspector().loadTable("fixture.db", "posts", limit = -1)
        assertTrue(clamped.rows.isNotEmpty(), "a clamped limit must still return rows")
        assertTrue(clamped.rows.size <= 1000, "row count was ${clamped.rows.size}")

        assertEquals(1, inspector().loadTable("fixture.db", "posts", limit = 1).rows.size)
    }

    @Test
    fun `degrades to an empty result for a database that cannot be opened`() {
        val missing = DevToolsDatabaseInspector(
            database = alohomoraDb,
            appDatabaseProvider = FixtureProvider("gone.db", NSTemporaryDirectory() + "gone.db"),
        )
        // Must not throw: this runs on the DevTools reader coroutine, and an unreadable file has
        // to degrade to an empty view rather than tear down the session.
        assertNotNull(missing.loadSchema("gone.db"))
        assertEquals(emptyList(), missing.loadSchema("gone.db").tables)
    }

    @Test
    fun `sandbox scan filters sidecars and hides alohomora's own database`() {
        val provider = IosAppDatabaseProvider()
        // Runs against the real sandbox; assert on the *rules* rather than on contents, which
        // vary by device.
        provider.listDatabases().forEach { info ->
            assertTrue(
                AUXILIARY.none { info.name.contains(it, ignoreCase = true) },
                "sidecar leaked into the database list: ${info.name}",
            )
            assertTrue(info.name != "alohomora.db", "Alohomora's own database must be hidden")
        }
    }

    private companion object {
        val AUXILIARY = listOf("-wal", "-shm", "-journal", ".corrupt")
    }

    private fun randomSuffix(): String = NSFileManager.defaultManager.hashCode().toString() +
        this.hashCode().toString()
}
