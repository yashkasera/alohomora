package io.github.yashkasera.alohomora.api

import io.github.yashkasera.alohomora.data.datasource.local.ApiRequestDao
import io.github.yashkasera.alohomora.data.entity.ApiRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * The collector responsible of collecting data from a [ChuckerInterceptor] and
 * storing it/displaying push notification. You need to instantiate one of those and
 * provide it to
 *
 * @param context An Android Context
 * @param showNotification Control whether a notification is shown while HTTP activity
 * is recorded.
 * @param retentionPeriod Set the retention period for HTTP transaction data captured
 * by this collector. The default is one week.
 */

private object NetworkInjector2 : KoinComponent {
    val dao: ApiRequestDao by inject()
}

class ChuckerCollector() {
    //    private val notificationHelper: NotificationHelper = NotificationHelper(context)
    private val scope = MainScope()

    init {
//        RepositoryProvider.initialize(context)
//        Chucker.showNotifications = showNotification
    }

    /**
     * Call this method when you send an HTTP request.
     * @param transaction The HTTP transaction sent
     */
    internal fun onRequestSent(transaction: ApiRequest) {
        scope.launch {
            withContext(Dispatchers.IO) {
                NetworkInjector2.dao.insert(transaction)
            }

            /*if (showNotification) {
                notificationHelper.show(transaction)
            }
            withContext(Dispatchers.IO) {
                retentionManager.doMaintenance()
            }*/
        }
    }

    /**
     * Call this method when you received the response of an HTTP request.
     * It must be called after [ChuckerCollector.onRequestSent].
     * @param transaction The sent HTTP transaction completed with the response
     */
    internal fun onResponseReceived(transaction: ApiRequest) {
        scope.launch {
            val updated = withContext(Dispatchers.IO) {
                NetworkInjector2.dao.update(transaction)
            }
            /*if (showNotification && updated > 0) {
                notificationHelper.show(transaction)
            }*/
        }
    }
}
