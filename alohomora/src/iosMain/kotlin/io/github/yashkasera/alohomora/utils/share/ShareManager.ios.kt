package io.github.yashkasera.alohomora.utils.share

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

internal actual class ShareManager {
    actual fun shareText(text: String) {
        val activityItems = listOf(text)
        val activityViewController = UIActivityViewController(activityItems, null)

        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        rootViewController?.presentViewController(
            activityViewController,
            animated = true,
            completion = null,
        )
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    actual fun shareFile(content: String, filename: String, mimeType: String) {
        // Write to temp file
        val tempDir = NSTemporaryDirectory()
        val filePath = "$tempDir/$filename"

        val data = NSString.create(string = content)
        data.writeToFile(filePath, atomically = true, encoding = NSUTF8StringEncoding, error = null)

        // Create file URL and share
        val fileURL = NSURL.fileURLWithPath(filePath)
        val activityItems = listOf(fileURL)
        val activityViewController = UIActivityViewController(activityItems, null)

        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        rootViewController?.presentViewController(
            activityViewController,
            animated = true,
            completion = null,
        )
    }
}
