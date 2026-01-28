package io.github.yashkasera.alohomora.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Clock
import kotlinx.serialization.json.JsonElement

@Entity
data class Screen(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val properties: JsonElement? = null,
    val time: Long = Clock.System.now().epochSeconds,
)
