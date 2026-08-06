package io.github.yashkasera.alohomora.devtools

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import io.github.yashkasera.alohomora.ActivityTracker
import io.github.yashkasera.alohomora.DevToolsActivity

internal actual object ServerActiveNotificationHost {

    private const val CHANNEL_ID = "alohomora_server_active"
    private const val NOTIFICATION_ID = 0xA10B

    actual fun show(port: Int, hasClient: Boolean) {
        val context = ActivityTracker.applicationContext ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        ensureChannel(context)

        val title = if (hasClient) "Desktop connected" else "DevTools server active"
        val text = if (hasClient) "Streaming on port $port" else "Listening on port $port"
        val chipText = if (hasClient) "Connected" else "Port $port"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(createContentIntent(context))
            .setOngoing(true)
            .setRequestPromotedOngoing(true)
            .setShortCriticalText(chipText)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.notify(NOTIFICATION_ID, notification)
    }

    actual fun dismiss() {
        ActivityTracker.applicationContext
            ?.getSystemService(NotificationManager::class.java)
            ?.cancel(NOTIFICATION_ID)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "DevTools server",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Shown while the Alohomora DevTools server is running"
                setShowBadge(false)
            },
        )
    }

    private fun createContentIntent(context: Context): PendingIntent {
        val intent = Intent(context, DevToolsActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        return PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
