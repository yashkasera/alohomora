package io.github.yashkasera.alohomora.showcaseApp

import android.app.Application
import io.github.yashkasera.alohomora.devtools.DevToolsTcpClient
import io.github.yashkasera.alohomora.showcaseApp.di.appModule
import org.koin.core.context.loadKoinModules

class AndroidApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Alohomora initializes Koin via its AndroidX Startup initializer.
        loadKoinModules(appModule)
    }
}
