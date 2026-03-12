package io.github.yashkasera.alohomora.data.model

data class AlohomoraCommit(
    val sha: String,
    val author: String,
    val message: String,
    val timestamp: Long,
)
