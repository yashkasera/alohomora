package io.github.yashkasera.alohomora.di

import org.koin.dsl.module

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.yashkasera.alohomora.data.db.AlohomoraDb
import platform.Foundation.NSHomeDirectory

actual val platformModule = module {
    single<AlohomoraDb> {
        val dbFilePath = NSHomeDirectory() + "/alohomora.db"
        Room.databaseBuilder<AlohomoraDb>(
            name = dbFilePath,
            factory = { AlohomoraDb::class.instantiateImpl() }
        )
            .setDriver(BundledSQLiteDriver())
            .build()
    }
}
