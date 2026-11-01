import platform.UIKit.UIViewController

/**
 * No-op mirror of `:alohomora`'s `MainViewController`.
 *
 * Returns an empty [UIViewController] so a release build links the same entry point without
 * pulling in the console UI. Must match the debug signature exactly.
 */
@Suppress("unused")
fun MainViewController(): UIViewController = UIViewController()

/**
 * No-op mirror of `:alohomora`'s `MainViewController(onClose)`. [onClose] is never called: there is
 * no console, so nothing to close.
 */
@Suppress("unused", "UNUSED_PARAMETER")
fun MainViewController(onClose: () -> Unit): UIViewController = UIViewController()
