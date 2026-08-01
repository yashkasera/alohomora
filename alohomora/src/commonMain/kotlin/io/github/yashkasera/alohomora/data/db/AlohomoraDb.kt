package io.github.yashkasera.alohomora.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import io.github.yashkasera.alohomora.common.Error
import io.github.yashkasera.alohomora.common.Event
import io.github.yashkasera.alohomora.common.HeadersConverter
import io.github.yashkasera.alohomora.common.PropertiesConverter
import io.github.yashkasera.alohomora.common.Screen
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.data.datasource.local.ErrorDao
import io.github.yashkasera.alohomora.data.datasource.local.EventDao
import io.github.yashkasera.alohomora.data.datasource.local.ScreenDao
import io.github.yashkasera.alohomora.data.datasource.local.TrafficDao

// --- Database ---

@Database(
    entities = [Event::class, TrafficEntry::class, Error::class, Screen::class],
    version = 1,
    exportSchema = false,
)
@ConstructedBy(AlohomoraDbConstructor::class)
@TypeConverters(PropertiesConverter::class, HeadersConverter::class)
internal abstract class AlohomoraDb : RoomDatabase() {
    abstract fun screenDao(): ScreenDao
    abstract fun errorDao(): ErrorDao
    abstract fun trafficDao(): TrafficDao
    abstract fun eventDao(): EventDao
}

@Suppress("KotlinNoActualForExpect")
internal expect object AlohomoraDbConstructor : RoomDatabaseConstructor<AlohomoraDb> {
    override fun initialize(): AlohomoraDb
}
