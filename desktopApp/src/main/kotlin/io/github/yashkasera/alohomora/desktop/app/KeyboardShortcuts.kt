package io.github.yashkasera.alohomora.desktop.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key

val isMacOs: Boolean by lazy {
    System.getProperty("os.name").orEmpty().contains("Mac", ignoreCase = true)
}

fun displayModifier(): String = if (isMacOs) "Cmd" else "Ctrl"

fun KeyEvent.isShortcutModifier(): Boolean =
    if (isMacOs) isMetaPressed else isCtrlPressed

@OptIn(ExperimentalComposeUiApi::class)
fun KeyEvent.isModifierKeyOnly(): Boolean {
    val k = key
    return if (isMacOs) {
        k == Key.MetaLeft || k == Key.MetaRight
    } else {
        k == Key.CtrlLeft || k == Key.CtrlRight
    }
}

@OptIn(ExperimentalComposeUiApi::class)
fun KeyEvent.matchesNavigation(): Int {
    if (!isShortcutModifier()) return -1
    return when (key) {
        Key.One -> 0
        Key.Two -> 1
        Key.Three -> 2
        Key.Four -> 3
        Key.Five -> 4
        Key.Six -> 5
        Key.Seven -> 6
        Key.Eight -> 7
        Key.Nine -> 8
        else -> -1
    }
}

const val NAVIGATION_SHORTCUT_SLOTS: Int = 9

fun navigationShortcutDigit(index: Int): String? = when {
    index !in 0 until NAVIGATION_SHORTCUT_SLOTS -> null
    else -> "${index + 1}"
}

@OptIn(ExperimentalComposeUiApi::class)
fun KeyEvent.isClearShortcut(): Boolean =
    isShortcutModifier() && isShiftPressed && key == Key.Backspace

@OptIn(ExperimentalComposeUiApi::class)
fun KeyEvent.isScreenshotShortcut(): Boolean =
    isShortcutModifier() && isShiftPressed && key == Key.S

@OptIn(ExperimentalComposeUiApi::class)
fun KeyEvent.isZoomInShortcut(): Boolean =
    isShortcutModifier() && key == Key.Equals

@OptIn(ExperimentalComposeUiApi::class)
fun KeyEvent.isZoomOutShortcut(): Boolean =
    isShortcutModifier() && key == Key.Minus

@OptIn(ExperimentalComposeUiApi::class)
fun KeyEvent.isResetZoomShortcut(): Boolean =
    isShortcutModifier() && key == Key.Zero
