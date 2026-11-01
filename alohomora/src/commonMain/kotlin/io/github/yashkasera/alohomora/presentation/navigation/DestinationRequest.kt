package io.github.yashkasera.alohomora.presentation.navigation

/**
 * One request, from outside the console, to show [route] — a notification tap or a launch intent.
 *
 * Deliberately **not** a data class. The nav host keys its navigation effect on the request, and
 * identity equality is what makes two taps on the same notification two navigations: a `Routes`
 * key would match the previous one after the user had navigated away from it, and the second tap
 * would do nothing.
 *
 * A request for [Routes.Overview] navigates nowhere. That is what an intent with no destination
 * maps to — the launcher icon, shake-to-open, the server notification — and on an already-running
 * console those should bring it forward where the user left it, not reset it to the root.
 */
internal class DestinationRequest(val route: Routes) {
    companion object {
        /** The default for hosts that never redirect the console. */
        val Root = DestinationRequest(Routes.Overview)
    }
}
