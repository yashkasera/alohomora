package io.github.yashkasera.alohomora.api

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ClearApiRequestsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        CoroutineScope(Dispatchers.IO).launch {

//            PowerplayDebugDatabase.INSTANCE.apiRequestDao.deleteAll()
//            ApiRequestNotification.dismissNotification()
        }
    }

    companion object {
        fun newIntent(context: Context) =
            Intent(context, ClearApiRequestsReceiver::class.java)
    }
}
