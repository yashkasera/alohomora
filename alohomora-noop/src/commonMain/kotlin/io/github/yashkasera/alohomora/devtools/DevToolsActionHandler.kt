package io.github.yashkasera.alohomora.devtools

@Suppress("unused")
fun interface DevToolsActionHandler {
    suspend fun execute(params: Map<String, String>): Map<String, String>
}
