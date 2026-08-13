package io.github.yashkasera.alohomora

import androidx.compose.ui.window.ComposeUIViewController
import io.github.yashkasera.alohomora.presentation.ui.AlohomoraApp
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplication
import platform.UIKit.UIModalPresentationPageSheet
import platform.UIKit.UISheetPresentationControllerDetent
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.sheetPresentationController
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.math.sqrt

/**
 * iOS shake detection via Core Motion. Unlike Android, the library both listens and presents here,
 * because `Alohomora.init()` is the earliest hook the library gets on iOS. See [installShakeToOpen].
 *
 * `CMMotionManager` is retained for the process lifetime — a released manager stops delivering
 * updates. Apple recommends a single instance per app; a debug library sharing the accelerometer is
 * acceptable and does not interfere with a host that reads motion of its own.
 */
private val motionManager = CMMotionManager()

@OptIn(ExperimentalForeignApi::class)
internal actual fun installShakeToOpen() {
    if (!motionManager.accelerometerAvailable) return
    if (motionManager.accelerometerActive) return

    motionManager.accelerometerUpdateInterval = SAMPLE_INTERVAL_SECONDS
    motionManager.startAccelerometerUpdatesToQueue(NSOperationQueue()) { data, _ ->
        data ?: return@startAccelerometerUpdatesToQueue
        if (!ShakeToOpenState.enabled) return@startAccelerometerUpdatesToQueue
        // CMAcceleration is already in gravities, so a resting device reads a magnitude near 1.0.
        val magnitude = data.acceleration.useContents { sqrt(x * x + y * y + z * z) }
        if (magnitude <= SHAKE_THRESHOLD_G) return@startAccelerometerUpdatesToQueue

        // data.timestamp is seconds since boot; debounce so one physical shake toggles once.
        if (data.timestamp - lastShakeSeconds < SHAKE_DEBOUNCE_SECONDS) return@startAccelerometerUpdatesToQueue
        lastShakeSeconds = data.timestamp
        DevToolsConsoleHost.toggle()
    }
}

private var lastShakeSeconds = 0.0

private const val SAMPLE_INTERVAL_SECONDS = 0.1 // 10 Hz — enough to feel a shake, cheap on battery
private const val SHAKE_THRESHOLD_G = 2.3
private const val SHAKE_DEBOUNCE_SECONDS = 1.0

/**
 * Presents the console as a page sheet and toggles it on each shake.
 *
 * Shake-to-toggle is the exit, not swipe-to-dismiss: the Compose content consumes the sheet's
 * dismiss drag (the same reason the showcase renders its own close button), so tracking the
 * presented controller here and dismissing it on the next shake is the reliable way back out.
 */
private object DevToolsConsoleHost {

    private var presented: UIViewController? = null

    fun toggle() = onMain {
        if (presented != null) dismiss() else present()
    }

    private fun present() {
        val controller = ComposeUIViewController { AlohomoraApp() }.apply {
            modalPresentationStyle = UIModalPresentationPageSheet
            sheetPresentationController?.apply {
                // Large detent: the console is the whole app, not a half-height prompt.
                setDetents(listOf(UISheetPresentationControllerDetent.largeDetent()))
                prefersGrabberVisible = true
                preferredCornerRadius = SHEET_CORNER_RADIUS
            }
        }
        presented = controller
        topMostController()?.presentViewController(controller, animated = true, completion = null)
    }

    private fun dismiss() {
        val controller = presented ?: return
        presented = null
        // Dismiss through the presenter; a detached controller asked to dismiss itself is a no-op.
        val presenter = controller.presentingViewController ?: controller
        presenter.dismissViewControllerAnimated(true, completion = null)
    }

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

    private fun onMain(block: () -> Unit) {
        dispatch_async(dispatch_get_main_queue()) { block() }
    }

    private const val SHEET_CORNER_RADIUS = 20.0
}
