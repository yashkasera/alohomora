package io.github.yashkasera.alohomora.showcaseApp.data.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class PostsApi(
    private val client: HttpClient,
) {
    suspend fun fetchPosts(): List<PostDto> {
        return client.get("https://jsonplaceholder.typicode.com/posts").body()
    }
}
