package io.github.yashkasera.alohomora.desktop.util

import java.awt.FileDialog
import java.awt.Frame

fun pickSavePath(defaultName: String, dialogTitle: String, extension: String): String? {
    return try {
        val dialog = FileDialog(null as Frame?, dialogTitle, FileDialog.SAVE)
        dialog.file = defaultName
        dialog.isVisible = true
        val file = dialog.file ?: return null
        val directory = dialog.directory ?: return null
        val basePath = directory + file
        if (extension.isBlank()) {
            basePath
        } else if (basePath.lowercase().endsWith(extension.lowercase())) {
            basePath
        } else {
            basePath + extension
        }
    } catch (e: Exception) {
        null
    }
}
