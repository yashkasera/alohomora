package io.github.yashkasera.alohomora.desktop.util

import java.awt.FileDialog
import java.awt.Frame

fun pickApkPath(): String? {
    return try {
        val dialog = FileDialog(null as Frame?, "Select APK", FileDialog.LOAD)
        dialog.setFilenameFilter { _, name -> name.endsWith(".apk", ignoreCase = true) }
        dialog.isVisible = true
        val file = dialog.file ?: return null
        val directory = dialog.directory ?: return null
        directory + file
    } catch (e: Exception) {
        null
    }
}
