package io.github.yashkasera.alohomora.di

import org.koin.dsl.module

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.yashkasera.alohomora.data.db.AlohomoraDb
import io.github.yashkasera.alohomora.data.db.AlohomoraDbConstructor
import io.github.yashkasera.alohomora.data.repository.VaultRepositoryImpl
import io.github.yashkasera.alohomora.devtools.DevToolsAppDatabaseProvider
import io.github.yashkasera.alohomora.devtools.DevToolsPreferencesInspector
import io.github.yashkasera.alohomora.devtools.DevToolsTcpServer
import io.github.yashkasera.alohomora.devtools.EmptyAppDatabaseProvider
import io.github.yashkasera.alohomora.devtools.IosPreferencesInspector
import io.github.yashkasera.alohomora.domain.repository.VaultRepository
import io.github.yashkasera.alohomora.utils.share.ShareManager
import platform.Foundation.NSHomeDirectory

actual val platformModule = module {
    single<AlohomoraDb> {
        val dbFilePath = NSHomeDirectory() + "/alohomora.db"
        Room.databaseBuilder<AlohomoraDb>(
            name = dbFilePath,
            factory = AlohomoraDbConstructor::initialize,
        )
            .setDriver(BundledSQLiteDriver())
            .fallbackToDestructiveMigration(true)
            .build()
    }
    single<DevToolsPreferencesInspector> { IosPreferencesInspector() }
    single<DevToolsAppDatabaseProvider> { EmptyAppDatabaseProvider }
    single { DevToolsTcpServer() }
    single { ShareManager() }
    single<VaultRepository> { VaultRepositoryImpl() }
    single<io.github.yashkasera.alohomora.domain.repository.PreferenceRepository> {
        io.github.yashkasera.alohomora.data.repository.PreferenceRepositoryImpl()
    }
}
