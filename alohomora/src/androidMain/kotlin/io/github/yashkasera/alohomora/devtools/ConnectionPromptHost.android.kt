package io.github.yashkasera.alohomora.devtools

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import io.github.yashkasera.alohomora.ActivityTracker
import io.github.yashkasera.alohomora.presentation.ui.components.ConnectionRequestSheetContent
import io.github.yashkasera.alohomora.ui.components.AlohomoraBottomSheetModal
import io.github.yashkasera.alohomora.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("StaticFieldLeak")
internal actual object ConnectionPromptHost {

    /**
     * The overlay currently attached, if any.
     *
     * Held together with the parent it was added to. Removal used to go through
     * `ActivityTracker.currentActivity`, which is null exactly when it matters: the user reads the
     * code, switches to the desktop to type it — backgrounding the app — and auth completes with
     * no foreground Activity. The overlay then survived as a MATCH_PARENT view over the whole
     * window, swallowing every touch once the user came back.
     */
    private var overlay: ComposeView? = null
    private var overlayParent: ViewGroup? = null

    actual fun show(otp: String, onRememberChange: (Boolean) -> Unit) {
        val activity = ActivityTracker.currentActivity
        if (activity == null) {
            // Backgrounded. Android 10+ blocks background Activity starts, and there is no
            // window to attach to, so a notification is the only channel left. It carries no
            // checkbox, so onRememberChange is never called and the pairing stays one-off —
            // the safe default when we cannot ask.
            notify(otp)
            return
        }

        activity.runOnUiThread {
            dismissOverlay()
            val view = ComposeView(activity).apply {
                // The overlay outlives individual recompositions but must die with the window,
                // otherwise a rotation leaks the composition.
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AppTheme {
                        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                        AlohomoraBottomSheetModal(
                            // Swiping away must actually tear the overlay down. With an empty
                            // handler the sheet animated out but the full-screen ComposeView
                            // stayed attached and ate every touch — the app looked frozen. The
                            // code remains on the console's Overview screen, so dismissing here
                            // loses nothing.
                            onDismissRequest = { detachOverlay() },
                            sheetState = sheetState,
                        ) {
                            ConnectionRequestSheetContent(
                                otp = otp,
                                onRememberChange = onRememberChange,
                                // Wrap: ModalBottomSheet already sizes to its content.
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
            val parent = activity.window.decorView as? ViewGroup ?: return@runOnUiThread
            parent.addView(
                view,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                ),
            )
            overlay = view
            overlayParent = parent
        }
    }

    actual fun dismiss() {
        // Posted to the overlay's own view, not to currentActivity: the app is usually
        // backgrounded at this point, and a null Activity used to mean the overlay was never
        // removed at all.
        overlay?.let { view -> view.post { detachOverlay() } } ?: cancelNotification()
        cancelNotification()
    }

    /** Must run on the UI thread. */
    private fun dismissOverlay() = detachOverlay()

    private fun detachOverlay() {
        val view = overlay ?: return
        // Prefer the recorded parent; fall back to the live one in case the hierarchy moved.
        (overlayParent ?: view.parent as? ViewGroup)?.removeView(view)
        // Releases the composition immediately rather than waiting for the lifecycle observer,
        // so a stale sheet cannot flash back on the next attach.
        view.disposeComposition()
        overlay = null
        overlayParent = null
    }

    private fun notify(otp: String) {
        val context = ActivityTracker.applicationContext ?: return
        // POST_NOTIFICATIONS is declared in the manifest but is a runtime grant on 13+. If the
        // host app never asked for it there is genuinely nowhere to show this, so degrade
        // silently rather than crash — the code is still visible inside the console.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Alohomora", NotificationManager.IMPORTANCE_HIGH),
            )
        }

        manager.notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Alohomora code: $otp")
                .setContentText("Enter this on the desktop client to connect.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setAutoCancel(false)
                .build(),
        )
    }

    private fun cancelNotification() {
        ActivityTracker.applicationContext
            ?.getSystemService(NotificationManager::class.java)
            ?.cancel(NOTIFICATION_ID)
    }

    private const val CHANNEL_ID = "alohomora_connection_request"
    private const val NOTIFICATION_ID = 0xA10A
}
