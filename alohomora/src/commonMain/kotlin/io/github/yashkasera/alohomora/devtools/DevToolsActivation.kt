package io.github.yashkasera.alohomora.devtools

import io.github.yashkasera.alohomora.data.model.AlohomoraConfig

/**
 * Implemented by configs that can tell "the build said no" from "the build said nothing".
 *
 * The public [AlohomoraConfig.devToolsEnabled] cannot: it is a non-null `Boolean` defaulting to
 * `false`, so an explicit `false` and a config that never mentions the property read the same.
 * Only configs Alohomora builds itself (the iOS bundle config, whose source is a nullable Info.plist
 * key) carry the distinction, so it lives on an internal side interface rather than in the public
 * one.
 */
internal interface ExplicitDevToolsDecision {
    /** `true`/`false` when the build decided, null when it did not. */
    val devToolsDecision: Boolean?
}

/**
 * Decides whether the DevTools server — and with it every way into the console — is active.
 *
 * An explicit build decision wins: the Gradle plugin's generated config (always `true`, since it is
 * only generated for enabled variants, which is how a non-debuggable beta opts in) and the iOS
 * `AlohomoraDevToolsEnabled` Info.plist key (either way). With no decision, the host app's own
 * debuggability decides, which is what makes the documented zero-config setup work: Android with no
 * plugin has no config at all, and iOS consumers link the *Release* XCFramework, so the framework's
 * own build type says nothing about the app.
 *
 * Reading a plain config's `false` as "no decision" is deliberate. It is what every config generated
 * by plugin 1.2.0 and earlier reports, because those predate the property and inherit the interface
 * default, and treating it as an opt-out would leave DevTools off in exactly the debug builds those
 * consumers use. Nothing loses by it: the plugin has never generated `false`, and a variant it does
 * not enable must not contain the full library at all (`verifyNoAlohomoraIn<Variant>` fails the
 * build), so there is no plugin-built variant whose debuggability could wrongly switch it on.
 */
internal object DevToolsActivation {

    fun isEnabled(config: AlohomoraConfig?, hostDebuggable: Boolean): Boolean =
        explicitDecision(config) ?: hostDebuggable

    fun explicitDecision(config: AlohomoraConfig?): Boolean? = when (config) {
        null -> null
        is ExplicitDevToolsDecision -> config.devToolsDecision
        else -> if (config.devToolsEnabled) true else null
    }
}
