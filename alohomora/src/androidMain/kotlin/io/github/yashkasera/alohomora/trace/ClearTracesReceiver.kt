package io.github.yashkasera.alohomora.trace

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ClearTracesReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        CoroutineScope(Dispatchers.IO).launch {

//            PowerplayDebugDatabase.INSTANCE.traceDao.deleteAll()
//            TraceNotification.dismissNotification()
        }
    }

    companion object {
        fun newIntent(context: Context) =
            Intent(context, ClearTracesReceiver::class.java)
    }
}
