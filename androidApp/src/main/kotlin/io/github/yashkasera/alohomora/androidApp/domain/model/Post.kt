package io.github.yashkasera.alohomora.androidApp.domain.model

data class Post(
    val id: Long,
    val userId: Long,
    val title: String,
    val body: String,
)
