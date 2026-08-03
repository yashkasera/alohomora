package io.github.yashkasera.alohomora.common

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Clock
import kotlinx.serialization.Serializable

@Entity
@Serializable
data class Error(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val place: String? = null,
    val reason: String? = null,
    val stackTrace: String? = null,
    val time: Long = Clock.System.now().toEpochMilliseconds(),
    val isViewed: Boolean = false,
)

/**
 * Exception type shown as the row title, extracted from [Error.reason] (formatted `Type: message`).
 *
 * Lives here rather than beside the recording code because both consoles need it — the mobile
 * screens and the desktop panel — and they must agree.
 *
 * Order matters and used to be the other way round. `substringAfterLast(".")` first meant a message
 * containing a period — "Config missing. Retry." — reduced the reason to whatever followed the
 * *last* period, so the title rendered blank or as a fragment of the message. Isolating the type
 * before splitting on `.` is the only order that survives punctuation in the message.
 */
fun Error.exceptionTypeName(): String =
    reason?.substringBefore(":")?.substringAfterLast(".")?.takeIf { it.isNotBlank() }
        ?: "Unknown Exception"
