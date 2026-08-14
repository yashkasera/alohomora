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

fun pickDirectory(dialogTitle: String, initialDir: String? = null): String? {
    return try {
        val prev = System.getProperty("apple.awt.fileDialogForDirectories")
        System.setProperty("apple.awt.fileDialogForDirectories", "true")
        try {
            val dialog = FileDialog(null as Frame?, dialogTitle, FileDialog.LOAD)
            if (initialDir != null) dialog.directory = initialDir
            dialog.isVisible = true
            val dir = dialog.directory ?: return null
            val file = dialog.file ?: return null
            dir + file
        } finally {
            if (prev != null) {
                System.setProperty("apple.awt.fileDialogForDirectories", prev)
            } else {
                System.clearProperty("apple.awt.fileDialogForDirectories")
            }
        }
    } catch (_: Exception) {
        null
    }
}
