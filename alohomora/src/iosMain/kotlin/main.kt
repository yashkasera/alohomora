import androidx.compose.ui.window.ComposeUIViewController
import io.github.yashkasera.alohomora.presentation.ui.AlohomoraApp
import platform.UIKit.UIViewController

/**
 * Root view controller for the Alohomora console on iOS.
 *
 * Prefer the [MainViewController] overload taking `onClose` when presenting modally — see it for
 * why. This no-argument form exists because Kotlin default arguments do **not** become Swift
 * defaults: expressing it as `onClose: (() -> Unit)? = null` would have broken every existing
 * `MainKt.MainViewController()` call site in Swift.
 */
fun MainViewController(): UIViewController = MainViewController(onClose = null)

/**
 * Root view controller for the Alohomora console on iOS.
 *
 * @param onClose invoked when the user dismisses the console, and the trigger for rendering a
 *   close button at all. **Supply this whenever the console is presented modally.** iOS has no
 *   system back button, and a Compose view inside a SwiftUI sheet consumes the vertical drag
 *   gesture, so the sheet's interactive swipe-to-dismiss never fires — with no handler there is
 *   no way out of the console.
 *
 * Pair it with `.presentationDragIndicator(.visible)` on the Swift side so the user also gets the
 * native grab handle as a second escape route.
 */
fun MainViewController(onClose: (() -> Unit)?): UIViewController = ComposeUIViewController {
    AlohomoraApp(onClose = onClose)
}
