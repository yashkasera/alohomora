package io.github.yashkasera.alohomora.traffic

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.data.datasource.local.TrafficDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal class ClearTrafficReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION) return

        val dao: TrafficDao = Alohomora.koinApplication?.koin?.get() ?: return
        val pending = goAsync()

        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                dao.clearAll()
                NotificationManagerCompat.from(context)
                    .cancel(TrafficNotificationHelper.NOTIFICATION_ID)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION = "io.github.yashkasera.alohomora.CLEAR_TRAFFIC"
    }
}
