import platform.UIKit.UIViewController

/**
 * No-op mirror of `:alohomora`'s `MainViewController`.
 *
 * Returns an empty [UIViewController] so a release build links the same entry point without
 * pulling in the console UI. Must match the debug signature exactly.
 */
@Suppress("unused")
fun MainViewController(): UIViewController = UIViewController()
