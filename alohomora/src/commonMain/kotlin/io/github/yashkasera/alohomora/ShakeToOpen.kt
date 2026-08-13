package io.github.yashkasera.alohomora

import kotlin.concurrent.Volatile

/**
 * Runtime on/off switch for shake-to-open, flipped by `Alohomora.setShakeToOpenEnabled`.
 *
 * The accelerometer listener is installed once at init and left running; disabling only gates the
 * console launch. Keeping the stream registered is cheaper than tearing it down and re-arming it,
 * and lets a host toggle the gesture freely without re-initialising the library.
 */
internal object ShakeToOpenState {
    @Volatile
    var enabled: Boolean = true
}

/**
 * Installs a shake-to-open gesture that surfaces the Alohomora console on a physical shake.
 *
 * Called once from `Alohomora.initInternal`, under the init lock, mirroring `installCrashHandler`.
 * The mechanism is deliberately platform-specific:
 *
 * - **iOS** starts a Core Motion accelerometer stream here and presents the console itself, because
 *   the library is init'd manually (`Alohomora.init()`) and owns no earlier entry point.
 * - **Android** returns without doing anything: the accelerometer is wired in `AlohomoraInitializer`
 *   instead, which is the one place that already holds the application `Context` and can launch
 *   `DevToolsActivity`. Doing it here would need a `Context` this common path does not have.
 *
 * A shake gesture competes with nothing the host installs (accelerometer streams are not exclusive),
 * so unlike the crash handler there is no chaining contract to honour.
 */
internal expect fun installShakeToOpen()
