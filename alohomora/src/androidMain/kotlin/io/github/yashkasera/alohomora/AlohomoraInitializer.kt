package io.github.yashkasera.alohomora

import android.app.Application
import android.content.Context
import androidx.startup.Initializer
import io.github.yashkasera.alohomora.common.AlohomoraConfig
import io.github.yashkasera.alohomora.presentation.ui.screens.navigation.NavigationPlugin
import java.util.ServiceLoader
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class AlohomoraInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        println("AlohomoraInitializer create called")
        val config = discoverConfig(context)
        println("Alohomora -> config=$config")
        Alohomora.init(config) {
            androidLogger()
            androidContext(context)
        }
        Alohomora.startDevToolsServer()
        Alohomora.registerPlugin(NavigationPlugin)
        val app = context.applicationContext as Application
        app.registerActivityLifecycleCallbacks(ActivityTracker)
    }

    fun discoverConfig(context: Context): AlohomoraConfig? {
        val loader = ServiceLoader.load(
            AlohomoraConfig::class.java,
            context.classLoader
        )
        return loader.firstOrNull()
    }

    override fun dependencies(): List<Class<out Initializer<*>?>?> = emptyList()
}
