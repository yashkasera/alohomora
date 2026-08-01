package io.github.yashkasera.alohomora.utils.share

internal expect class ShareManager {
    fun shareText(text: String)
    fun shareFile(content: String, filename: String, mimeType: String = "text/plain")
}
