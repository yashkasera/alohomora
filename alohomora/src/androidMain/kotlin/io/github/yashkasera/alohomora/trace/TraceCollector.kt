package io.github.yashkasera.alohomora.trace

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.github.yashkasera.alohomora.common.TraceEntry
import io.github.yashkasera.alohomora.data.datasource.local.TraceDao
import io.github.yashkasera.alohomora.trace.TraceInjector.context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * The collector responsible for collecting data from a [TraceInterceptor] and
 * storing it/displaying push notification. You need to instantiate one of those and
 * provide it to
 *
 * @param context An Android Context
 */

private object TraceInjector : KoinComponent {
    val dao: TraceDao by inject()
    val context: Context by inject()
}

class TraceCollector(
    private val showNotification: Boolean = true,
) {
    private val notificationHelper: TraceNotificationHelper =
        TraceNotificationHelper(TraceInjector.context)
    private val scope = MainScope()

    /**
     * Check if notification permission is granted.
     * On Android 13+ (API 33+), this requires explicit user permission.
     */
    fun hasNotificationPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                TraceInjector.context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Permission not required on older Android versions
        }
    }

    /**
     * Call this method when you send an HTTP request.
     * @param transaction The HTTP transaction sent
     */
    internal fun onRequestSent(transaction: TraceEntry) {
        scope.launch {
            withContext(Dispatchers.IO) {
                TraceInjector.dao.insert(transaction)
            }
        }
    }

    /**
     * Call this method when you received the response of an HTTP request.
     * It must be called after [TraceCollector.onRequestSent].
     * @param transaction The sent HTTP transaction completed with the response
     */
    internal fun onResponseReceived(transaction: TraceEntry) {
        scope.launch {
            TraceInjector.dao.update(transaction)
            val latest = withContext(Dispatchers.IO) {
                if (showNotification) {
                    TraceInjector.dao.getLatest(5)
                } else {
                    emptyList()
                }
            }
            if (showNotification) {
                if (ActivityCompat.checkSelfPermission(
                        TraceInjector.context,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    if (showNotification && latest.isNotEmpty()) {
                        if (!hasNotificationPermission()) {
                            return@launch
                        }
                        notificationHelper.showLatest(latest)
                    }
                }
            }
        }
    }
}
