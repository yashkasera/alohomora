package io.github.yashkasera.alohomora.desktop.presentation.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The singleton side sheets the app opens by identity rather than by a selected item. Data-driven
 * sheets (traffic, error, trace, event, editors) keep their own source of truth in a ViewModel or
 * local state and never need an id — they still register into the dismiss stack via
 * [AlohomoraSideSheet], they just aren't owned here.
 */
enum class SideSheetId { MockRules, Journeys, DeepLinkCatalog, DeepLinkBuilder, CommandPalette, Help }

/**
 * One controller for every overlay in a device window. It carries two independent registries:
 *
 * 1. A **dismiss-topmost stack** of anonymous tokens, each with its current `onDismiss`. Every
 *    [AlohomoraSideSheet] registers itself while visible, so the stack reflects real open order and
 *    [dismissTop] (wired to Escape) closes whatever the user opened last — including a detail sheet
 *    sitting on top of its master.
 * 2. **Singleton visibility** keyed by [SideSheetId], so menu items, shortcuts, and the command
 *    palette open a sheet with [open] instead of threading a `show*`/`onOpen*`/`onDismiss*` triple
 *    down through the composition.
 *
 * The two are deliberately separate: the stack only needs identity + a dismiss handler for ordering,
 * while the id set owns the boolean an owner would otherwise hold.
 */
@Stable
class SideSheetHostState {
    private val openSet = mutableStateListOf<SideSheetId>()

    fun open(id: SideSheetId) {
        if (id !in openSet) openSet.add(id)
    }

    fun close(id: SideSheetId) {
        openSet.remove(id)
    }

    fun isOpen(id: SideSheetId): Boolean = id in openSet

    private val stack = mutableStateListOf<Entry>()

    /** True while any sheet is on the stack — used to suppress the modifier-key badges. */
    val isAnyOpen: Boolean get() = stack.isNotEmpty()

    fun register(token: Any, onDismiss: () -> Unit) {
        stack.removeAll { it.token === token }
        stack.add(Entry(token, onDismiss))
    }

    fun unregister(token: Any) {
        stack.removeAll { it.token === token }
    }

    /**
     * Dismiss the most recently opened sheet. Returns false when nothing is open.
     *
     * The entry is popped *before* its handler runs, so two Escape events landing in the same frame
     * (before the sheet's [DisposableEffect] gets a chance to unregister on the next recomposition)
     * dismiss two sheets, not the same one twice. The sheet's own later `unregister` is then a no-op.
     */
    fun dismissTop(): Boolean {
        val top = stack.lastOrNull() ?: return false
        stack.removeAt(stack.lastIndex)
        top.onDismiss()
        return true
    }

    private class Entry(val token: Any, val onDismiss: () -> Unit)
}

/**
 * Defaults to a throwaway host rather than throwing, so a sheet dropped outside the app scaffold — a
 * `@Preview` or an isolated Compose test rendering one panel — still composes. Registration into the
 * fallback is inert but harmless; the real device window always provides its own host.
 */
val LocalSideSheetHost = staticCompositionLocalOf { SideSheetHostState() }

/**
 * Registers an overlay that is not built on [AlohomoraSideSheet] (the command palette and help
 * dialog) into the dismiss stack, so Escape treats it like any other sheet. Call it as a sibling of
 * the overlay's own conditional render.
 */
@Composable
fun RegisterSideSheet(visible: Boolean, onDismiss: () -> Unit) {
    val host = LocalSideSheetHost.current
    val token = remember { Any() }
    val latestDismiss by rememberUpdatedState(onDismiss)
    DisposableEffect(host, visible) {
        if (visible) host.register(token) { latestDismiss() } else host.unregister(token)
        onDispose { host.unregister(token) }
    }
}
