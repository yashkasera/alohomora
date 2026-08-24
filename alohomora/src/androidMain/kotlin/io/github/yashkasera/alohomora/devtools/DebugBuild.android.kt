package io.github.yashkasera.alohomora.devtools

import android.content.Context
import android.content.pm.ApplicationInfo
import io.github.yashkasera.alohomora.AlohomoraImpl

/**
 * Whether the host app is a debuggable build.
 *
 * Previously hardcoded `true`, which made the `if (!isDebugBuild) return false` guard in
 * `DevToolsRuntime.start` vacuous: a consumer who wired the real artifact with
 * `implementation` instead of `debugImplementation` shipped a release build that would
 * happily bind a TCP listener exposing captured traffic. iOS always did this properly via
 * `Platform.isDebugBinary`.
 *
 * Fails closed — if the Context cannot be resolved we cannot prove the build is debuggable,
 * so report false rather than start a server.
 */
internal actual val isDebugBuild: Boolean
    get() {
        val context =
            runCatching { AlohomoraImpl.koinApplication?.koin?.get<Context>() }.getOrNull()
                ?: return false
        return context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    }
