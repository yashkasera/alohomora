package io.github.yashkasera.alohomora.trace

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.yashkasera.alohomora.DevToolsActivity
import io.github.yashkasera.alohomora.common.TraceEntry

internal class TraceNotificationHelper(
    private val context: Context,
) {
    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ensureChannel()
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showLatest(items: List<TraceEntry>) {
        val summaries = items
            .take(MAX_LINES)
            .map { request ->
                val method = request.method ?: "?"
                val path = request.path?.let { base ->
                    if (request.query.isNullOrEmpty()) base else "$base?${request.query}"
                } ?: (request.url ?: "")
                val status = request.status?.toString() ?: "-"
                "$status $method $path"
            }

        val style = NotificationCompat.InboxStyle().also { inbox ->
            summaries.forEach { inbox.addLine(it) }
            inbox.setSummaryText("${summaries.size} latest traces")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentTitle("Latest network traces")
            .setContentText(summaries.firstOrNull() ?: "No recent traces")
            .setStyle(style)
            .setContentIntent(createContentIntent())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun ensureChannel() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Network traces",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Shows recent network traces"
        }
        manager.createNotificationChannel(channel)
    }

    private fun createContentIntent(): PendingIntent {
        val intent = Intent(context, DevToolsActivity::class.java)
            .putExtra(EXTRA_START_DESTINATION, DESTINATION_TRACE)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        return PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val CHANNEL_ID = "alohomora_traces"
        private const val NOTIFICATION_ID = 41024
        private const val MAX_LINES = 5
        private const val REQUEST_CODE = 41025

        const val EXTRA_START_DESTINATION = "alohomora_start_destination"
        const val DESTINATION_TRACE = "trace"
    }
}
