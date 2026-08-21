package io.github.yashkasera.alohomora.common

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import kotlin.time.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

const val CRASH_EVENT_NAME = "App.Exception"

@Entity
@Serializable
data class Event(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val properties: JsonElement?,
    // Milliseconds, matching every explicit write (recordEvent, the traffic
    // interceptors). This default was seconds, so anything relying on it sorted ~1000x too
    // low and sank to the bottom of a time-ordered list.
    val time: Long = Clock.System.now().toEpochMilliseconds(),
    /** Set once the user opens the event, so the list can dim it. Mirrors TrafficEntry.isViewed. */
    val isViewed: Boolean = false,
)

val Event.isCrashEvent: Boolean get() = name == CRASH_EVENT_NAME

class PropertiesConverter {
    @TypeConverter
    fun convertTo(data: JsonElement): String = Json.encodeToString(data)

    @TypeConverter
    fun convertFrom(string: String): JsonElement = Json.decodeFromString(string)
}