package io.github.yashkasera.alohomora.common

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import kotlin.time.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

@Entity
@Serializable
data class TelemetryEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val properties: JsonElement?,
    // Milliseconds, matching every explicit write (recordTelemetry, the trace
    // interceptors). This default was seconds, so anything relying on it sorted ~1000x too
    // low and sank to the bottom of a time-ordered list.
    val time: Long = Clock.System.now().toEpochMilliseconds(),
    /** Set once the user opens the event, so the list can dim it. Mirrors TraceEntry.isViewed. */
    val isViewed: Boolean = false,
)

class PropertiesConverter {
    @TypeConverter
    fun convertTo(data: JsonElement): String = Json.encodeToString(data)

    @TypeConverter
    fun convertFrom(string: String): JsonElement = Json.decodeFromString(string)
}
