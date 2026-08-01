package io.github.yashkasera.alohomora.showcaseApp

import android.app.Application
import io.github.yashkasera.alohomora.showcaseApp.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class AndroidApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // The app owns its own Koin container.
        //
        // This doubles as a regression test for Alohomora's DI isolation: the library's
        // AndroidX Startup initializer has already run by the time onCreate is called, so if
        // Alohomora ever went back to `startKoin` this line would throw
        // KoinAppAlreadyStartedException. Do not replace it with loadKoinModules() — that
        // only worked because the library used to leak its container into GlobalContext.
        startKoin {
            androidContext(this@AndroidApp)
            modules(appModule)
        }
    }
}
