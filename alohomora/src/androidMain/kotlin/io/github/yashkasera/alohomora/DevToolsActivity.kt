package io.github.yashkasera.alohomora

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import io.github.yashkasera.alohomora.presentation.navigation.DestinationRequest
import io.github.yashkasera.alohomora.presentation.navigation.Routes
import io.github.yashkasera.alohomora.presentation.ui.AlohomoraApp
import io.github.yashkasera.alohomora.traffic.TrafficNotificationHelper

class DevToolsActivity : ComponentActivity() {
    // Compose state, not a plain field. The activity is `singleTask` and the notification adds
    // SINGLE_TOP, so a tap while the console is open arrives through onNewIntent — after setContent
    // has already composed. Only a snapshot write reaches that composition; a plain field was read
    // once and every later tap did nothing.
    private var destination by mutableStateOf(DestinationRequest.Root)

    companion object {
        private const val EXTRA_TRACE_ID = "extra_trace_id"

        fun newIntent(context: Context, traceId: String): Intent {
            return Intent(context, DevToolsActivity::class.java).apply {
                putExtra(EXTRA_TRACE_ID, traceId)
                putExtra(
                    TrafficNotificationHelper.EXTRA_START_DESTINATION,
                    TrafficNotificationHelper.DESTINATION_TRACE,
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // On recreation the NavController restores its own back stack. Replaying the launch intent
        // over it would push the original destination back on top of wherever the user had gone.
        if (savedInstanceState == null) requestDestination(intent)
        enableEdgeToEdge()
        setContent {
            AlohomoraApp(
                destination = destination,
                onThemeChanged = { ThemeChanged(it) },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestDestination(intent)
    }

    private fun requestDestination(intent: Intent?) {
        val extra = intent?.getStringExtra(TrafficNotificationHelper.EXTRA_START_DESTINATION)
        val traceId = intent?.getStringExtra(EXTRA_TRACE_ID)
        val route = when {
            traceId != null -> Routes.TrafficDetails(traceId)
            extra == TrafficNotificationHelper.DESTINATION_TRACE -> Routes.Traffic
            else -> Routes.Overview
        }
        destination = DestinationRequest(route)
    }
}

@Composable
private fun ThemeChanged(isDark: Boolean) {
    val view = LocalView.current
    LaunchedEffect(isDark) {
        val window = (view.context as Activity).window
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
    }
}
