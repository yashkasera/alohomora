package io.github.yashkasera.alohomora.desktop.util

import java.awt.FileDialog
import java.awt.Frame

fun pickLoadPath(dialogTitle: String, vararg extensions: String): String? {
    return try {
        val dialog = FileDialog(null as Frame?, dialogTitle, FileDialog.LOAD)
        if (extensions.isNotEmpty()) {
            dialog.setFilenameFilter { _, name ->
                extensions.any { name.endsWith(it, ignoreCase = true) }
            }
        }
        dialog.isVisible = true
        val file = dialog.file ?: return null
        val directory = dialog.directory ?: return null
        directory + file
    } catch (_: Exception) {
        null
    }
}
