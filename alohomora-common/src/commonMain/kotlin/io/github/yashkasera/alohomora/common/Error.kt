package io.github.yashkasera.alohomora.common

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Clock

@Entity
data class Error(
    @PrimaryKey var id: Long = 0,
    val place: String? = null,
    val reason: String? = null,
    val stackTrace: String? = null,
    // Milliseconds. Every explicit write in the project uses toEpochMilliseconds(); this
    // default was epochSeconds, so anything relying on it was ~1000x too low and sank to the
    // bottom of a newest-first list.
    val time: Long = Clock.System.now().toEpochMilliseconds(),
    val isViewed: Boolean = false,
)