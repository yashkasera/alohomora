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
import io.github.yashkasera.alohomora.common.Span
import io.github.yashkasera.alohomora.common.SpanEventsConverter
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.data.datasource.local.ErrorDao
import io.github.yashkasera.alohomora.data.datasource.local.EventDao
import io.github.yashkasera.alohomora.data.datasource.local.ScreenDao
import io.github.yashkasera.alohomora.data.datasource.local.SpanDao
import io.github.yashkasera.alohomora.data.datasource.local.TrafficDao

// --- Database ---

@Database(
    entities = [Event::class, TrafficEntry::class, Error::class, Screen::class, Span::class],
    // 4: added the Span table for distributed trace capture. Both platforms use
    // fallbackToDestructiveMigration, so this bump wipes every captured traffic entry, event and
    // error on the next launch — but skipping it crashes at startup instead.
    // 3: Error.id gained `autoGenerate`, which changes its column to AUTOINCREMENT. Required, not
    // cosmetic — without it every insert used the default id 0 and `OnConflictStrategy.REPLACE`
    // overwrote the previous row, so the table never held more than one error.
    version = 4,
    exportSchema = false,
)
@ConstructedBy(AlohomoraDbConstructor::class)
@TypeConverters(PropertiesConverter::class, HeadersConverter::class, SpanEventsConverter::class)
internal abstract class AlohomoraDb : RoomDatabase() {
    abstract fun screenDao(): ScreenDao
    abstract fun errorDao(): ErrorDao
    abstract fun trafficDao(): TrafficDao
    abstract fun eventDao(): EventDao
    abstract fun spanDao(): SpanDao
}

@Suppress("KotlinNoActualForExpect")
internal expect object AlohomoraDbConstructor : RoomDatabaseConstructor<AlohomoraDb> {
    override fun initialize(): AlohomoraDb
}
