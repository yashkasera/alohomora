package io.github.yashkasera.alohomora.di

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.yashkasera.alohomora.data.db.AlohomoraDb
import io.github.yashkasera.alohomora.data.db.isSchemaIdentityMismatch
import io.github.yashkasera.alohomora.data.repository.DatabaseRepositoryImpl
import io.github.yashkasera.alohomora.devtools.DebugConfigStore
import io.github.yashkasera.alohomora.devtools.DevToolsAppDatabaseProvider
import io.github.yashkasera.alohomora.devtools.DevToolsCacheInspector
import io.github.yashkasera.alohomora.devtools.DevToolsTcpServer
import io.github.yashkasera.alohomora.devtools.DevToolsTrustStore
import io.github.yashkasera.alohomora.devtools.IosAppDatabaseProvider
import io.github.yashkasera.alohomora.devtools.IosDebugConfigStore
import io.github.yashkasera.alohomora.devtools.IosTrustStore
import io.github.yashkasera.alohomora.devtools.RepositoryCacheInspector
import io.github.yashkasera.alohomora.domain.repository.DatabaseRepository
import io.github.yashkasera.alohomora.utils.share.ShareManager
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.runBlocking
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

private const val DATABASE_NAME = "alohomora.db"


@OptIn(ExperimentalForeignApi::class)
internal actual val platformModule = module {
    single<AlohomoraDb> {
        val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
        val dbFilePath = requireNotNull(documentDirectory?.path) + "/$DATABASE_NAME"
        openDatabase(dbFilePath)
    }
    single<DevToolsCacheInspector> { RepositoryCacheInspector(get()) }
    single<DevToolsAppDatabaseProvider> { IosAppDatabaseProvider() }
    single { DevToolsTcpServer() }
    single<DevToolsTrustStore> { IosTrustStore() }
    single<DebugConfigStore> { IosDebugConfigStore() }
    single { ShareManager() }
    single<DatabaseRepository> { DatabaseRepositoryImpl() }
    single<io.github.yashkasera.alohomora.domain.repository.CacheRepository> {
        io.github.yashkasera.alohomora.data.repository.CacheRepositoryImpl()
    }
}

/**
 * Opens the database, recreating it once if the compiled schema no longer matches the file.
 *
 * Mirrors the Android path; see `openDatabase` in `Koin.android.kt` for why the open is forced
 * eagerly rather than left to the first DAO call. The probe differs only because the bundled
 * driver exposes no `openHelper`, so validation is triggered by opening a reader connection.
 */
private fun openDatabase(dbFilePath: String): AlohomoraDb {
    val database = buildDatabase(dbFilePath)
    return try {
        // A zero-row read is enough to make Room open the file and run its identity check. The
        // bundled driver exposes no openHelper, so unlike Android this needs a query.
        //
        // runBlocking is safe here: Koin resolution of AlohomoraDb is deliberately deferred into
        // a coroutine on a background dispatcher (see Alohomora.recordTraffic), never the main
        // thread, precisely because opening this database does synchronous SQLite work.
        runBlocking { database.eventDao().getLatest(0) }
        database
    } catch (exception: Exception) {
        if (!exception.isSchemaIdentityMismatch()) throw exception

        println(
            "[Alohomora] Schema does not match AlohomoraDb.version; recreating the capture " +
                "database. Bump the version when entities change: ${exception.message}",
        )
        runCatching { database.close() }
        deleteDatabaseFiles(dbFilePath)
        buildDatabase(dbFilePath)
    }
}

private fun buildDatabase(dbFilePath: String): AlohomoraDb =
    Room.databaseBuilder<AlohomoraDb>(name = dbFilePath)
        .setDriver(BundledSQLiteDriver())
        // Matches Android. Captured traffic and events are disposable debug data, so a schema
        // change drops and recreates rather than shipping hand-written migrations. Without this,
        // bumping the version crashes the host app on open.
        .fallbackToDestructiveMigration(true)
        .build()

/**
 * Removes the database and its `-wal`/`-shm` sidecars.
 *
 * The sidecars matter: a stale WAL would otherwise carry the previous schema's pages straight back
 * into the freshly created file.
 */
@OptIn(ExperimentalForeignApi::class)
private fun deleteDatabaseFiles(dbFilePath: String) {
    val manager = NSFileManager.defaultManager
    listOf(dbFilePath, "$dbFilePath-wal", "$dbFilePath-shm").forEach { path ->
        if (manager.fileExistsAtPath(path)) {
            manager.removeItemAtPath(path, error = null)
        }
    }
}
