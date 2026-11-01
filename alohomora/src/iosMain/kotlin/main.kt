import androidx.compose.ui.window.ComposeUIViewController
import io.github.yashkasera.alohomora.presentation.ui.AlohomoraApp
import platform.UIKit.UIViewController

/**
 * Root view controller for the Alohomora console on iOS, with no close button.
 *
 * Use this only where the host supplies its own way out. **When presenting modally, use the
 * [MainViewController] overload taking `onClose`** — see it for why.
 *
 * Two declarations rather than one with `onClose: (() -> Unit)? = null`, because Kotlin default
 * arguments do not become Swift defaults: a defaulted parameter would have broken every existing
 * `MainKt.MainViewController()` call site.
 */
@Suppress("unused")
fun MainViewController(): UIViewController = consoleViewController(onClose = null)

/**
 * Root view controller for the Alohomora console on iOS, rendering a close button on the root
 * screen that calls [onClose].
 *
 * **Supply this whenever the console is presented modally.** iOS has no system back button, and a
 * Compose view inside a SwiftUI sheet consumes the vertical drag, so the sheet's interactive
 * swipe-to-dismiss never fires — without a handler there is no way out of the console. Pair it with
 * `.presentationDragIndicator(.visible)` on the Swift side for the native grab handle as well.
 *
 * Non-nullable on purpose: a Swift caller that has nothing to pass wants the other overload.
 */
@Suppress("unused")
fun MainViewController(onClose: () -> Unit): UIViewController = consoleViewController(onClose)

// Both overloads call this rather than one calling the other. The earlier pair delegated
// `MainViewController()` to `MainViewController(onClose = null)`, and 30628dda removed it over an
// infinite recursion (EXC_BAD_ACCESS) on that path. A private function with its own name leaves no
// overload for the call to resolve back into.
private fun consoleViewController(onClose: (() -> Unit)?): UIViewController =
    ComposeUIViewController { AlohomoraApp(onClose = onClose) }
