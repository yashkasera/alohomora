package io.github.yashkasera.alohomora.showcaseApp.data.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header

class PostsApi(
    private val client: HttpClient,
) {
    suspend fun fetchPosts(): List<PostDto> {
        return client.get("https://jsonplaceholder.typicode.com/posts") {
            // Dummy credentials, on purpose. These demonstrate — and act as a live check on —
            // Alohomora's header redaction: they must show as [REDACTED] in the trace detail,
            // in the frame streamed to the desktop client, and in the generated curl command.
            // X-Request-Source is not sensitive and must survive verbatim.
            header("Authorization", "Bearer showcase-demo-token-not-a-real-secret")
            header("X-Api-Key", "showcase-demo-api-key")
            header("X-Request-Source", "alohomora-showcase")
        }.body()
    }
}
