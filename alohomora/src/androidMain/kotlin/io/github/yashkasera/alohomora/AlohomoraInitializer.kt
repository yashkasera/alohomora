package io.github.yashkasera.alohomora

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.startup.Initializer
import io.github.yashkasera.alohomora.data.model.AlohomoraConfig
import io.github.yashkasera.alohomora.presentation.ui.screens.navigation.NavigationPlugin
import java.util.ServiceLoader
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class AlohomoraInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        val config = discoverConfig(context)
        AlohomoraImpl.initInternal(config, hostDebuggable = context.isDebuggable()) {
            androidLogger()
            androidContext(context)
        }
        Alohomora.registerPlugin(NavigationPlugin)
        ActivityTracker.attach(context.applicationContext as Application)
        // After attach: a shake opens the console over the tracked foreground Activity. Gated like
        // the common hooks in initInternal, for a non-debuggable build that ships the full library.
        if (AlohomoraImpl.isDevToolsEnabled()) {
            AndroidShakeToOpen.install(context.applicationContext)
        }
    }

    fun discoverConfig(context: Context): AlohomoraConfig? {
        val loader = ServiceLoader.load(
            AlohomoraConfig::class.java,
            context.classLoader,
        )
        return loader.firstOrNull()
    }

    // The *host app's* flag, not this library's: Alohomora arrives as a release AAR, so its own
    // BuildConfig says nothing about whether the app it is embedded in is a debug build.
    private fun Context.isDebuggable(): Boolean =
        (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    override fun dependencies(): List<Class<out Initializer<*>?>?> = emptyList()
}
