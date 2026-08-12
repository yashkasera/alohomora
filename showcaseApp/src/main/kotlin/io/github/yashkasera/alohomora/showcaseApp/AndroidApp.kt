package io.github.yashkasera.alohomora.showcaseApp

import android.app.Application
import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.replay.ktorReplayHandler
import io.github.yashkasera.alohomora.showcaseApp.di.appModule
import io.ktor.client.HttpClient
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
        val koin = startKoin {
            androidContext(this@AndroidApp)
            modules(appModule)
        }.koin

        // Hands Alohomora the app's own client so the console can re-send a captured request through
        // it. Nothing else can: whatever this client's plugins derive per-request — signatures, auth
        // tokens, pinning — is unavailable to a request the library assembles and sends itself, so an
        // edited payload would go out carrying the signature captured from the original body.
        //
        // Resolved eagerly rather than inside the module: registration has to happen whether or not
        // anything has made a request yet, and a lazy `single` would leave replay unavailable until
        // the first one.
        Alohomora.registerReplayHandler(ktorReplayHandler(koin.get<HttpClient>()))

        Alohomora.recordFeatureFlag("dark_mode_v2", "true", source = "Firebase Remote Config", type = "feature_flag")
        Alohomora.recordFeatureFlag("checkout_redesign", "false", source = "Firebase Remote Config", type = "experiment")
        Alohomora.recordFeatureFlag("max_cart_items", "25", source = "LaunchDarkly", type = "remote_config")
        Alohomora.recordFeatureFlag(
            "onboarding_flow",
            "variant_b",
            source = "LaunchDarkly",
            type = "experiment",
            metadata = mapOf("cohort" to "new_users", "rollout_pct" to "50"),
        )
        Alohomora.recordFeatureFlag("enable_search_v3", "true", type = "feature_flag")
    }
}
