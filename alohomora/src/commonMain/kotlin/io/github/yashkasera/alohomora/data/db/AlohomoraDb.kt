package io.github.yashkasera.alohomora.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.yashkasera.alohomora.common.Analytics
import io.github.yashkasera.alohomora.common.ApiRequest
import io.github.yashkasera.alohomora.common.Crash
import io.github.yashkasera.alohomora.common.HeadersConverter
import io.github.yashkasera.alohomora.common.PropertiesConverter
import io.github.yashkasera.alohomora.common.Screen
import io.github.yashkasera.alohomora.data.datasource.local.AnalyticsDao
import io.github.yashkasera.alohomora.data.datasource.local.ApiRequestDao
import io.github.yashkasera.alohomora.data.datasource.local.CrashDao
import io.github.yashkasera.alohomora.data.datasource.local.ScreenDao

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
