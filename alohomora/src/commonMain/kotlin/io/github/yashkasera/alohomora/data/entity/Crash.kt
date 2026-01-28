package io.github.yashkasera.alohomora.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Clock

@Entity
data class Crash(
    @PrimaryKey var id: Long = 0,
    val place: String? = null,
    val reason: String? = null,
    val stackTrace: String? = null,
    val time: Long = Clock.System.now().epochSeconds,
    val isViewed: Boolean = false,
)
