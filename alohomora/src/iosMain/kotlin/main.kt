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
fun MainViewController(): UIViewController = ComposeUIViewController {
    AlohomoraApp()
}
