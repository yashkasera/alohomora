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
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import io.github.yashkasera.alohomora.presentation.navigation.Routes
import io.github.yashkasera.alohomora.presentation.ui.AlohomoraApp
import io.github.yashkasera.alohomora.traffic.TrafficNotificationHelper

class DevToolsActivity : ComponentActivity() {
    private var startDestination: Routes = Routes.Overview

    companion object {
        private const val EXTRA_TRACE_ID = "extra_trace_id"

        fun newIntent(context: Context, traceId: String): Intent {
            return Intent(context, DevToolsActivity::class.java).apply {
                putExtra(EXTRA_TRACE_ID, traceId)
                putExtra(TrafficNotificationHelper.EXTRA_START_DESTINATION, TrafficNotificationHelper.DESTINATION_TRACE)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateStartDestination(intent)
        enableEdgeToEdge()
        setContent {
            AlohomoraApp(
                startDestination = startDestination,
                onThemeChanged = { ThemeChanged(it) },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        updateStartDestination(intent)
    }

    private fun updateStartDestination(intent: Intent?) {
        val destination = intent?.getStringExtra(TrafficNotificationHelper.EXTRA_START_DESTINATION)
        val traceId = intent?.getStringExtra(EXTRA_TRACE_ID)
        startDestination = when {
            traceId != null -> Routes.TrafficDetails(traceId)
            destination == TrafficNotificationHelper.DESTINATION_TRACE -> Routes.Traffic
            else -> Routes.Overview
        }
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
