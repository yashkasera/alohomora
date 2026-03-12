package io.github.yashkasera.alohomora.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.yashkasera.alohomora.data.db.AlohomoraDb
import io.github.yashkasera.alohomora.data.repository.PlatformDatabaseAccessor
import io.github.yashkasera.alohomora.data.repository.VaultRepositoryImpl
import io.github.yashkasera.alohomora.domain.repository.VaultRepository
import io.github.yashkasera.alohomora.devtools.AndroidAppDatabaseProvider
import io.github.yashkasera.alohomora.devtools.DevToolsAppDatabaseProvider
import io.github.yashkasera.alohomora.devtools.AndroidPreferencesInspector
import io.github.yashkasera.alohomora.devtools.DevToolsPreferencesInspector
import io.github.yashkasera.alohomora.devtools.DevToolsTcpServer
import io.github.yashkasera.alohomora.presentation.ui.screens.navigation.NavigationHistoryViewModel
import io.github.yashkasera.alohomora.utils.share.ShareManager
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

actual val platformModule = module {
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
    single { ShareManager(androidContext()) }
    single<VaultRepository> {
        val accessor = PlatformDatabaseAccessor()
        accessor.setContext(androidContext())
        VaultRepositoryImpl(accessor)
    }
}

private fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<AlohomoraDb> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("alohomora.db")

    return Room.databaseBuilder<AlohomoraDb>(
        context = appContext,
        name = dbFile.absolutePath,
    ).fallbackToDestructiveMigration(true)
}
