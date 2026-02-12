package io.github.yashkasera.alohomora.showcaseApp.domain.model

data class Post(
    val id: Long,
    val userId: Long,
    val title: String,
    val body: String,
)
