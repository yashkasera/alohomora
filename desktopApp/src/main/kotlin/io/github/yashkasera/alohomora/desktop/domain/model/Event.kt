package io.github.yashkasera.alohomora.desktop.domain.model

import kotlinx.serialization.json.JsonElement

data class Event(
    val id: Long,
    val name: String,
    val properties: JsonElement?,
    val time: Long,
)
