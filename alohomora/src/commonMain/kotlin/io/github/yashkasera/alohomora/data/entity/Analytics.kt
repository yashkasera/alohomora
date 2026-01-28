package io.github.yashkasera.alohomora.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import kotlin.time.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

@Entity
data class Analytics(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val properties: JsonElement?,
    val time: Long = Clock.System.now().epochSeconds,
)

class PropertiesConverter {
    @TypeConverter
    fun convertTo(data: JsonElement): String = Json.encodeToString(data)

    @TypeConverter
    fun convertFrom(string: String): JsonElement = Json.decodeFromString(string)
}
