package io.github.yashkasera.alohomora.utils.share

expect class ShareManager {
    fun shareText(text: String)
    fun shareFile(content: String, filename: String, mimeType: String = "text/plain")
}
