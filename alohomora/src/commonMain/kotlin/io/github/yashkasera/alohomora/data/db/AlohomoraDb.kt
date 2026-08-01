package io.github.yashkasera.alohomora.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import io.github.yashkasera.alohomora.common.TelemetryEvent
import io.github.yashkasera.alohomora.common.TraceEntry
import io.github.yashkasera.alohomora.common.Incident
import io.github.yashkasera.alohomora.common.HeadersConverter
import io.github.yashkasera.alohomora.common.PropertiesConverter
import io.github.yashkasera.alohomora.common.Screen
import io.github.yashkasera.alohomora.data.datasource.local.TelemetryDao
import io.github.yashkasera.alohomora.data.datasource.local.TraceDao
import io.github.yashkasera.alohomora.data.datasource.local.IncidentDao
import io.github.yashkasera.alohomora.data.datasource.local.ScreenDao

// --- Database ---

@Database(
    entities = [TelemetryEvent::class, TraceEntry::class, Incident::class, Screen::class],
    // 2: TelemetryEvent.isViewed added.
    version = 2,
    exportSchema = false,
)
@ConstructedBy(AlohomoraDbConstructor::class)
@TypeConverters(PropertiesConverter::class, HeadersConverter::class)
internal abstract class AlohomoraDb : RoomDatabase() {
    abstract fun screenDao(): ScreenDao
    abstract fun incidentDao(): IncidentDao
    abstract fun traceDao(): TraceDao
    abstract fun telemetryDao(): TelemetryDao
}

@Suppress("KotlinNoActualForExpect")
internal expect object AlohomoraDbConstructor : RoomDatabaseConstructor<AlohomoraDb> {
    override fun initialize(): AlohomoraDb
}
