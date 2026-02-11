package io.github.yashkasera.alohomora

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import io.github.yashkasera.alohomora.api.ApiLogNotificationHelper
import io.github.yashkasera.alohomora.presentation.navigation.Routes
import io.github.yashkasera.alohomora.presentation.ui.AlohomoraApp

class DevToolsActivity : ComponentActivity() {
    private val startDestinationState = mutableStateOf<Routes>(Routes.Dashboard)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateStartDestination(intent)
        enableEdgeToEdge()
        setContent {
            AlohomoraApp(
                startDestination = startDestinationState.value,
                onThemeChanged = { ThemeChanged(it) },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        updateStartDestination(intent)
    }

    private fun updateStartDestination(intent: Intent?) {
        val destination = intent?.getStringExtra(ApiLogNotificationHelper.EXTRA_START_DESTINATION)
        startDestinationState.value = when (destination) {
            ApiLogNotificationHelper.DESTINATION_API_LOGS -> Routes.ApiLogs
            else -> Routes.Dashboard
        }
    }
}

@Composable
private fun ThemeChanged(isDark: Boolean) {
    val view = LocalView.current
    LaunchedEffect(isDark) {
        val window = (view.context as Activity).window
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = isDark
            isAppearanceLightNavigationBars = isDark
        }
    }
}
