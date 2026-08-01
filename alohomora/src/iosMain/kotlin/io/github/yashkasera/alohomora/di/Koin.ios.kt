package io.github.yashkasera.alohomora.di

import org.koin.dsl.module

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.yashkasera.alohomora.data.db.AlohomoraDb
import io.github.yashkasera.alohomora.data.db.AlohomoraDbConstructor
import io.github.yashkasera.alohomora.data.repository.VaultRepositoryImpl
import io.github.yashkasera.alohomora.devtools.DevToolsAppDatabaseProvider
import io.github.yashkasera.alohomora.devtools.DevToolsPreferencesInspector
import io.github.yashkasera.alohomora.devtools.IosAppDatabaseProvider
import io.github.yashkasera.alohomora.devtools.DevToolsTcpServer
import io.github.yashkasera.alohomora.devtools.DevToolsTrustStore
import io.github.yashkasera.alohomora.devtools.IosTrustStore
import io.github.yashkasera.alohomora.devtools.IosPreferencesInspector
import io.github.yashkasera.alohomora.domain.repository.VaultRepository
import io.github.yashkasera.alohomora.utils.share.ShareManager
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
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
            error = null
        )
        val dbFilePath = requireNotNull(documentDirectory?.path) + "/$DATABASE_NAME"
        Room.databaseBuilder<AlohomoraDb>(name = dbFilePath)
            .setDriver(BundledSQLiteDriver())
            // Matches Android. Captured traces and telemetry are disposable debug data, so a
            // schema change drops and recreates rather than shipping hand-written migrations.
            // Without this, bumping the version crashes the host app on open.
            .fallbackToDestructiveMigration(true)
            .build()
    }
    single<DevToolsPreferencesInspector> { IosPreferencesInspector() }
    single<DevToolsAppDatabaseProvider> { IosAppDatabaseProvider() }
    single { DevToolsTcpServer() }
    single<DevToolsTrustStore> { IosTrustStore() }
    single { ShareManager() }
    single<VaultRepository> { VaultRepositoryImpl() }
    single<io.github.yashkasera.alohomora.domain.repository.PreferenceRepository> {
        io.github.yashkasera.alohomora.data.repository.PreferenceRepositoryImpl()
    }
}
