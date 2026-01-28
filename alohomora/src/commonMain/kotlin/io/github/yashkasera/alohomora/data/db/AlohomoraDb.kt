package io.github.yashkasera.alohomora.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.yashkasera.alohomora.data.datasource.local.AnalyticsDao
import io.github.yashkasera.alohomora.data.datasource.local.ApiRequestDao
import io.github.yashkasera.alohomora.data.datasource.local.CrashDao
import io.github.yashkasera.alohomora.data.datasource.local.ScreenDao
import io.github.yashkasera.alohomora.data.entity.Analytics
import io.github.yashkasera.alohomora.data.entity.ApiRequest
import io.github.yashkasera.alohomora.data.entity.Crash
import io.github.yashkasera.alohomora.data.entity.HeadersConverter
import io.github.yashkasera.alohomora.data.entity.PropertiesConverter
import io.github.yashkasera.alohomora.data.entity.Screen

// --- Database ---

@Database(
    entities = [Analytics::class, ApiRequest::class, Crash::class, Screen::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(PropertiesConverter::class, HeadersConverter::class)
internal abstract class AlohomoraDb : RoomDatabase() {
    abstract fun screenDao(): ScreenDao

    abstract fun crashDao(): CrashDao
    abstract fun networkDao(): ApiRequestDao
    abstract fun eventDao(): AnalyticsDao
}
