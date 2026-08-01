package io.github.yashkasera.alohomora.traffic

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.data.datasource.local.TrafficDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The collector responsible for collecting data from a [TrafficInterceptor] and
 * storing it/displaying push notification.
 */
class TrafficCollector(
    private val showNotification: Boolean = true,
) {
    // Resolved lazily from Alohomora's isolated Koin container rather than through
    // KoinComponent/GlobalContext, which the library no longer populates. Lazy also means
    // `TrafficInterceptor()` can be constructed before Alohomora.init() without throwing.
    private val dao: TrafficDao? get() = Alohomora.koinApplication?.koin?.get()
    private val context: Context? get() = Alohomora.koinApplication?.koin?.get()

    private val notificationHelper: TrafficNotificationHelper? by lazy {
        context?.let(::TrafficNotificationHelper)
    }

    // Not MainScope(): nothing here touches the UI, and posting DB work through the main
    // thread put a Room call on it for every single response.
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Check if notification permission is granted.
     * On Android 13+ (API 33+), this requires explicit user permission.
     */
    fun hasNotificationPermission(): Boolean {
        val ctx = context ?: return false
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                ctx,
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
    internal fun onRequestSent(transaction: TrafficEntry) {
        scope.launch {
            runCatching { dao?.insert(transaction) }
                .onFailure { println("[Alohomora] Failed to record request: ${it.message}") }
        }
    }

    /**
     * Call this method when you received the response of an HTTP request.
     * It must be called after [TrafficCollector.onRequestSent].
     * @param transaction The sent HTTP transaction completed with the response
     */
    internal fun onResponseReceived(transaction: TrafficEntry) {
        scope.launch {
            runCatching {
                val traceDao = dao ?: return@runCatching
                traceDao.update(transaction)
                if (!showNotification || !hasNotificationPermission()) return@runCatching
                val latest = traceDao.getLatest(5)
                if (latest.isEmpty()) return@runCatching
                notificationHelper?.showLatest(latest)
            }.onFailure { println("[Alohomora] Failed to record response: ${it.message}") }
        }
    }
}
