package io.github.yashkasera.alohomora.androidApp

import android.app.Application
import io.github.yashkasera.alohomora.androidApp.di.appModule
import org.koin.core.context.loadKoinModules

class AndroidApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Alohomora initializes Koin via its AndroidX Startup initializer.
        loadKoinModules(appModule)
    }
}
