package io.github.yashkasera.alohomora.di

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabaseCorruptException
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.yashkasera.alohomora.data.db.AlohomoraDb
import io.github.yashkasera.alohomora.data.repository.PlatformDatabaseAccessor
import io.github.yashkasera.alohomora.data.repository.VaultRepositoryImpl
import io.github.yashkasera.alohomora.devtools.AndroidAppDatabaseProvider
import io.github.yashkasera.alohomora.devtools.DevToolsAppDatabaseProvider
import io.github.yashkasera.alohomora.devtools.AndroidPreferencesInspector
import io.github.yashkasera.alohomora.devtools.DevToolsPreferencesInspector
import io.github.yashkasera.alohomora.devtools.AndroidTrustStore
import io.github.yashkasera.alohomora.devtools.DevToolsTcpServer
import io.github.yashkasera.alohomora.devtools.DevToolsTrustStore
import io.github.yashkasera.alohomora.domain.repository.VaultRepository
import io.github.yashkasera.alohomora.presentation.ui.screens.navigation.NavigationHistoryViewModel
import io.github.yashkasera.alohomora.utils.share.ShareManager
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

internal actual val platformModule = module {
    single<AlohomoraDb> {
        val context: Context = androidContext()
        getDatabaseBuilder(context)
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }
    viewModel { NavigationHistoryViewModel() }
    single<DevToolsPreferencesInspector> { AndroidPreferencesInspector(androidContext()) }
    single<DevToolsAppDatabaseProvider> { AndroidAppDatabaseProvider(androidContext()) }
    single { DevToolsTcpServer() }
    single<DevToolsTrustStore> { AndroidTrustStore(androidContext()) }
    single { ShareManager(androidContext()) }
    single<VaultRepository> {
        val accessor = PlatformDatabaseAccessor()
        accessor.setContext(androidContext())
        VaultRepositoryImpl(accessor)
    }
    single<io.github.yashkasera.alohomora.domain.repository.PreferenceRepository> {
        io.github.yashkasera.alohomora.data.repository.PreferenceRepositoryImpl(androidContext())
    }
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
    context.deleteDatabase(dbFile.name)
    // deleteDatabase does not reliably remove sidecars on every OEM build. Sweep them so a
    // stale WAL cannot resurrect the corrupt pages, and so the leftovers do not accumulate.
    dbFile.parentFile?.listFiles { file ->
        file.name.startsWith(dbFile.name) && file.name != dbFile.name
    }?.forEach { it.delete() }
}
