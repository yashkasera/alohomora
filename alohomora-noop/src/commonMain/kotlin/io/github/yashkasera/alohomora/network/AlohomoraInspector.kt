package io.github.yashkasera.alohomora.network

import io.ktor.client.plugins.api.createClientPlugin

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
