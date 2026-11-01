package io.github.yashkasera.alohomora.network

import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.util.AttributeKey

/**
 * No-op mirror of `:alohomora`'s `AlohomoraInspector` Ktor client plugin.
 *
 * Installs no hooks, so an `install(AlohomoraInspector)` call in a release build costs one
 * empty plugin registration and captures nothing. Exists purely so consumer code that
 * installs the inspector compiles against both artifacts.
 */
@Suppress("unused")
val AlohomoraInspector = createClientPlugin("AlohomoraInspector") {
    /* no-op: no onRequest/onResponse hooks are registered */
}

/**
 * No-op mirror of `:alohomora`'s `AlohomoraReplayOfKey`.
 *
 * Nothing reads it in a release build, but a custom Ktor replay handler puts it into the request's
 * attributes and has to compile against both artifacts. Declared in this file so it lands on the same
 * `AlohomoraInspectorKt` facade as the debug declaration. **Keep the name in lockstep with it.**
 */
@Suppress("unused")
val AlohomoraReplayOfKey: AttributeKey<String> = AttributeKey("AlohomoraReplayOf")
