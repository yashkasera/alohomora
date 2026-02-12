package io.github.yashkasera.alohomora

import android.app.Application
import android.content.Context
import androidx.startup.Initializer
import io.github.yashkasera.alohomora.presentation.ui.screens.navigation.NavigationPlugin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class AlohomoraInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        println("AlohomoraInitializer create called")
        Alohomora.init {
            androidLogger()
            androidContext(context)
        }
        Alohomora.startDevToolsServer()
        Alohomora.registerPlugin(NavigationPlugin)
        val app = context.applicationContext as Application
        app.registerActivityLifecycleCallbacks(ActivityTracker)
    }

    override fun dependencies(): List<Class<out Initializer<*>?>?> = emptyList()
}
