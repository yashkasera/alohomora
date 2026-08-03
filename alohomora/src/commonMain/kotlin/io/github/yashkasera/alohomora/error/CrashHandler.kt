package io.github.yashkasera.alohomora.error

/**
 * Installs a process-wide hook that records an uncaught exception before the app dies.
 *
 * Called once from `Alohomora.initInternal`, under the init lock, so both platforms get it from
 * their own entry point (AndroidX Startup on Android, `Alohomora.init()` on iOS) and it cannot
 * install twice.
 *
 * **Every implementation must chain to the handler it replaced.** Alohomora is a debugging aid
 * installed via `debugImplementation`; swallowing the exception would silently disable Crashlytics,
 * Sentry, or whatever else the app installed, and the symptom — crashes stop being reported in a
 * debug build only — is miserable to trace back to here.
 */
internal expect fun installCrashHandler()
