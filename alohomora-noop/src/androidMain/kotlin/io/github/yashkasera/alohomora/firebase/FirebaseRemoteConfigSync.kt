package io.github.yashkasera.alohomora.firebase

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import io.github.yashkasera.alohomora.Alohomora

@Suppress("unused")
fun Alohomora.syncFirebaseRemoteConfig(
    remoteConfig: FirebaseRemoteConfig,
    type: String = "remote_config",
) = Unit
