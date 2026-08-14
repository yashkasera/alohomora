package io.github.yashkasera.alohomora.di

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabaseCorruptException
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.yashkasera.alohomora.data.db.AlohomoraDb
import io.github.yashkasera.alohomora.data.db.isSchemaIdentityMismatch
import io.github.yashkasera.alohomora.data.repository.CacheRepositoryImpl
import io.github.yashkasera.alohomora.data.repository.DatabaseRepositoryImpl
import io.github.yashkasera.alohomora.data.repository.PlatformDatabaseAccessor
import io.github.yashkasera.alohomora.devtools.AndroidAppDatabaseProvider
import io.github.yashkasera.alohomora.devtools.AndroidTrustStore
import io.github.yashkasera.alohomora.devtools.DevToolsAppDatabaseProvider
import io.github.yashkasera.alohomora.devtools.DevToolsCacheInspector
import io.github.yashkasera.alohomora.devtools.DevToolsTcpServer
import io.github.yashkasera.alohomora.devtools.DevToolsTrustStore
import io.github.yashkasera.alohomora.devtools.RepositoryCacheInspector
import io.github.yashkasera.alohomora.domain.repository.CacheRepository
import io.github.yashkasera.alohomora.domain.repository.DatabaseRepository
import io.github.yashkasera.alohomora.presentation.ui.screens.navigation.NavigationHistoryViewModel
import io.github.yashkasera.alohomora.traffic.TrafficNotificationCallback
import io.github.yashkasera.alohomora.traffic.TrafficNotificationHelper
import io.github.yashkasera.alohomora.utils.share.ShareManager
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

internal actual val platformModule = module {
    single<AlohomoraDb> {
        val context: Context = androidContext()
        openDatabase(context)
    }
    viewModel { NavigationHistoryViewModel() }
    single<DevToolsCacheInspector> { RepositoryCacheInspector(get()) }
    single<DevToolsAppDatabaseProvider> { AndroidAppDatabaseProvider(androidContext()) }
    single { DevToolsTcpServer() }
    single<DevToolsTrustStore> { AndroidTrustStore(androidContext()) }
    single { ShareManager(androidContext()) }
    single { TrafficNotificationHelper(androidContext()) }
    single<TrafficNotificationCallback> {
        TrafficNotificationCallback { latest -> get<TrafficNotificationHelper>().showLatest(latest) }
    }
    single<DatabaseRepository> {
        val accessor = PlatformDatabaseAccessor()
        accessor.setContext(androidContext())
        DatabaseRepositoryImpl(accessor)
    }
    single<CacheRepository> {
        CacheRepositoryImpl(androidContext())
    }
}

/**
 * Opens the database, recreating it once if the compiled schema no longer matches the file.
 *
 * The open is forced here rather than left to the first DAO call. Room validates lazily, so a
 * schema mismatch used to surface as an exception thrown from whichever coroutine happened to
 * touch a DAO first — in practice the DevTools reader loop, which closed the socket and reported
 * nothing, so the console sat in a disconnect/reconnect cycle every few seconds with the real
 * cause visible only in logcat. Failing (and healing) at construction keeps the error next to the
 * thing that caused it.
 *
 * Only [isSchemaIdentityMismatch] is recovered from; see that function for why the match is narrow.
 */
private fun openDatabase(context: Context): AlohomoraDb {
    val appContext = context.applicationContext
    val database = getDatabaseBuilder(appContext)
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()

    return try {
        // Touching the open helper runs Room's identity check synchronously. Cheaper than a query
        // and it does not need a coroutine, so it stays off the caller's critical path.
        database.openHelper.writableDatabase
        database
    } catch (exception: Exception) {
        if (!exception.isSchemaIdentityMismatch()) throw exception

        Log.w(
            "AlohomoraDb",
            "Schema does not match AlohomoraDb.version; recreating the capture database. " +
                "Bump the version when entities change: ${exception.message}",
        )
        runCatching { database.close() }
        deleteDatabaseFiles(appContext)

        getDatabaseBuilder(appContext)
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }
}

/**
 * Removes the database and its sidecars.
 *
 * `deleteDatabase` alone is not enough: on some OEM builds it renames sidecars to `.corrupt`
 * instead of removing them, so a stale `-wal` could otherwise carry the old schema's pages into
 * the fresh file. Shared with the corruption path below for exactly that reason.
 */
private fun deleteDatabaseFiles(context: Context) {
    val dbFile = context.getDatabasePath(DATABASE_NAME)
    context.deleteDatabase(dbFile.name)
    dbFile.parentFile?.listFiles { file ->
        file.name.startsWith(dbFile.name) && file.name != dbFile.name
    }?.forEach { it.delete() }
}

private fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<AlohomoraDb> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("alohomora.db")
    ensureHealthyDatabase(appContext, dbFile.absolutePath)

    return Room.databaseBuilder<AlohomoraDb>(
        context = appContext,
        name = dbFile.absolutePath,
    ).fallbackToDestructiveMigration(true)
}

/**
 * Deletes the local database only if SQLite reports it as actually corrupt.
 *
 * Previously this caught bare [Exception] around both the open and the `quick_check`, so a
 * transient file lock, low disk, or a bundled-vs-framework SQLite version mismatch was
 * indistinguishable from corruption and silently destroyed the developer's captured traces.
 * On at least some OEM builds `deleteDatabase` renames sidecars to `.corrupt` rather than
 * removing them, so every false positive also left `-wal`/`-shm` files behind — observed in
 * the wild as `alohomora.db-wal.corrupt.corrupt.corrupt.corrupt.corrupt.corrupt`, several MB
 * of orphaned junk that then showed up in the DevTools database picker.
 */
private fun ensureHealthyDatabase(context: Context, databasePath: String) {
    val dbFile = context.getDatabasePath("alohomora.db")
    if (!dbFile.exists()) return

    val corrupt = try {
        SQLiteDatabase.openDatabase(
            databasePath,
            null,
            SQLiteDatabase.OPEN_READWRITE,
        ).use { db ->
            db.rawQuery("PRAGMA quick_check(1)", null).use { cursor ->
                cursor.moveToFirst() && cursor.getString(0) != "ok"
            }
        }
    } catch (exception: SQLiteDatabaseCorruptException) {
        true
    } catch (exception: Exception) {
        // Anything else is a transient or environmental failure. Room gets to try next; if
        // the file really is unusable it will surface its own error rather than us guessing.
        Log.w("AlohomoraDb", "Health check inconclusive, keeping database: ${exception.message}")
        false
    }

    if (!corrupt) return

    Log.w("AlohomoraDb", "quick_check reported corruption; recreating ${dbFile.name}")
    deleteDatabaseFiles(context)
}

private const val DATABASE_NAME = "alohomora.db"
