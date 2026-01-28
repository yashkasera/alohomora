package io.github.yashkasera.alohomora.di

import org.koin.dsl.module

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.yashkasera.alohomora.data.db.AlohomoraDb
import java.io.File

actual val platformModule = module {
    single<AlohomoraDb> {
        val dbFile = File(System.getProperty("java.io.tmpdir"), "alohomora.db")
        Room.databaseBuilder<AlohomoraDb>(
            name = dbFile.absolutePath,
        )
            .setDriver(BundledSQLiteDriver())
            .build()
    }
}
