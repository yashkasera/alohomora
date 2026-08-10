package io.github.yashkasera.alohomora.devtools

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import io.github.yashkasera.alohomora.presentation.ui.components.ConnectionRequestSheetContent
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import platform.UIKit.UIApplication
import platform.UIKit.UIModalPresentationPageSheet
import platform.UIKit.UISheetPresentationControllerDetent
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.sheetPresentationController
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * Presents the connection request as a bottom sheet over whatever the app is showing.
 *
 * There is no background path here, unlike Android. iOS freezes a backgrounded app, so the
 * DevTools socket stays bound while nothing services it — a connection that gets as far as
 * authenticating implies the app is foregrounded and has a key window to present on.
 */
internal actual object ConnectionPromptHost {

    private var presented: UIViewController? = null

    /**
     * Guards the present/dismiss race.
     *
     * UIKit silently ignores a dismiss issued while the presentation animation is still running,
     * and that is exactly the timing here — the sheet animates in, the user reads four digits and
     * the desktop authenticates moments later. The dismiss would be dropped and the sheet would
     * sit there over a connected session.
     */
    private var isPresenting = false
    private var dismissRequested = false

    actual fun show(otp: String, onRememberChange: (Boolean) -> Unit) {
        onMain {
            // Replace rather than stack: a reconnect during an unfinished pairing would
            // otherwise leave two sheets, and dismissing only the top one.
            dismissPresented(animated = false)

            val controller = ComposeUIViewController {
                AppTheme {
                    ConnectionRequestSheetContent(
                        otp = otp,
                        onRememberChange = onRememberChange,
                        // Fills the detent so the sheet is one continuous surface; anything
                        // shorter leaves the controller's background visible underneath.
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }.apply {
                modalPresentationStyle = UIModalPresentationPageSheet

                // Without a detent this renders as a full-height card: the Compose content hugs
                // the top and the theme's dark background fills the rest, which reads as "the
                // sheet is full screen with a black background". A medium detent makes it an
                // actual bottom sheet sized to roughly half the screen.
                sheetPresentationController?.apply {
                    setDetents(listOf(UISheetPresentationControllerDetent.mediumDetent()))
                    prefersGrabberVisible = true
                    preferredCornerRadius = SHEET_CORNER_RADIUS
                }
            }

            presented = controller
            isPresenting = true
            topMostController()?.presentViewController(
                controller,
                animated = true,
                completion = {
                    // Only the current sheet may clear the flag. A superseded presentation's
                    // completion arriving late would otherwise mark the *new* sheet as settled
                    // and let a dismiss for it be dropped again.
                    if (presented === controller) {
                        isPresenting = false
                        if (dismissRequested) {
                            dismissRequested = false
                            dismissPresented(animated = true)
                        }
                    }
                },
            )
        }
    }

    actual fun dismiss() {
        onMain {
            if (isPresenting) {
                // Deferred to the presentation completion; dismissing now would be a no-op.
                dismissRequested = true
            } else {
                dismissPresented(animated = true)
            }
        }
    }

    private fun dismissPresented(animated: Boolean) {
        val controller = presented ?: return
        presented = null
        dismissRequested = false
        isPresenting = false
        // Dismiss through the presenter. Asking the presented controller to dismiss itself is
        // forwarded by UIKit in the common case, but not when it has been re-parented or is
        // already detached — and then the sheet stays on screen after the desktop connects.
        val presenter = controller.presentingViewController ?: controller
        presenter.dismissViewControllerAnimated(animated, completion = null)
    }

    /**
     * Walks past anything already presented; presenting on a controller that is itself covered
     * silently does nothing.
     */
    private fun topMostController(): UIViewController? {
        val keyWindow = UIApplication.sharedApplication.windows
            .filterIsInstance<UIWindow>()
            .firstOrNull { it.isKeyWindow() }
            ?: UIApplication.sharedApplication.keyWindow

        var controller = keyWindow?.rootViewController
        while (controller?.presentedViewController != null) {
            controller = controller.presentedViewController
        }
        return controller
    }

    /** UIKit presentation is main-thread only; the DevTools reader runs on a background queue. */
    private fun onMain(block: () -> Unit) {
        dispatch_async(dispatch_get_main_queue()) { block() }
    }

    private const val SHEET_CORNER_RADIUS = 20.0
}
