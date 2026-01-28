package io.github.yashkasera.alohomora.data.model

data class Commit(
    val sha: String,
    val author: String,
    val message: String,
    val timestamp: Long,
)
