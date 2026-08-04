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
        // Cmd/Ctrl+0 as the tenth slot, added when Traces landed between Traffic and Events and pushed
        // the tail of the sidebar past the digits. Appending Traces at the end instead would have kept
        // this map untouched, but that subordinates the information architecture to a keybinding table.
        Key.Zero -> 9
        else -> -1
    }
}

/**
 * How many sidebar positions are reachable by shortcut. Beyond this, sections are click-only.
 *
 * Shared with the command palette so the shortcut it *prints* cannot drift from the one that actually
 * works — the palette stopped at 8 while [matchesNavigation] already handled 9.
 */
const val NAVIGATION_SHORTCUT_SLOTS: Int = 10

/** Digit shown for the section at [index], or null when it has no shortcut. */
fun navigationShortcutDigit(index: Int): String? = when {
    index !in 0 until NAVIGATION_SHORTCUT_SLOTS -> null
    // The tenth slot is Cmd+0, not Cmd+10.
    index == NAVIGATION_SHORTCUT_SLOTS - 1 -> "0"
    else -> "${index + 1}"
}

@OptIn(ExperimentalComposeUiApi::class)
fun KeyEvent.isClearShortcut(): Boolean =
    isShortcutModifier() && isShiftPressed && key == Key.Backspace

@OptIn(ExperimentalComposeUiApi::class)
fun KeyEvent.isScreenshotShortcut(): Boolean =
    isShortcutModifier() && isShiftPressed && key == Key.S
