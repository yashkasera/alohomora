package io.github.yashkasera.alohomora.traffic

import io.github.yashkasera.alohomora.Alohomora
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCClass
import platform.Foundation.NSStringFromClass
import platform.Foundation.NSURLProtocol
import platform.Foundation.NSURLSessionConfiguration

/**
 * The Objective-C class object for [AlohomoraURLProtocol].
 *
 * Taken from the companion, which extends `NSURLProtocolMeta` and therefore *is* the ObjC class.
 * Previously this was `NSClassFromString("AlohomoraKitAlohomoraURLProtocol")`, which coupled trace
 * capture to the framework's `baseName`: renaming the framework would have made every install
 * silently return false and capture nothing, with no compile error. It also resolved to null
 * outside the framework (e.g. in unit tests), making the behaviour untestable.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private val alohomoraProtocolClass: ObjCClass get() = AlohomoraURLProtocol

// Swift-facing API on the Alohomora singleton, since companion methods on a class
// whose companion extends an ObjC metaclass are not bridged as Swift type methods.

/**
 * Registers [AlohomoraURLProtocol] with the legacy `NSURLConnection` loading system.
 *
 * **This does not make URLSession traffic traceable.** URLSession ignores globally registered
 * protocol classes, so on its own this captures nothing from a modern app. Kept because it costs
 * nothing and still covers `NSURLConnection`, but every URLSession-based app needs
 * [alohomoraURLSessionConfiguration] or [installURLProtocol] as well.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Suppress("UnusedReceiverParameter", "unused")
fun Alohomora.registerURLProtocol(): Boolean =
    NSURLProtocol.registerClass(alohomoraProtocolClass)

/**
 * Inserts [AlohomoraURLProtocol] at the front of [configuration]'s protocol classes.
 *
 * This is the only supported way to trace URLSession traffic. Call it before constructing the
 * session — `URLSessionConfiguration` is copied by `URLSession(configuration:)`, so mutating the
 * configuration afterwards has no effect.
 *
 * @return true if the class was found and installed.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Suppress("UnusedReceiverParameter")
fun Alohomora.installURLProtocol(configuration: NSURLSessionConfiguration): Boolean {
    val cls = alohomoraProtocolClass
    val existing = configuration.protocolClasses.orEmpty()
    // Compared by ObjC class name, not by identity. Kotlin/Native hands back a *fresh*
    // ObjCClass wrapper on each access, so `it === cls` is never true and an identity check
    // silently re-prepends the class on every call — growing protocolClasses without bound.
    val name = NSStringFromClass(cls)
    val alreadyInstalled = existing.any {
        @Suppress("UNCHECKED_CAST")
        (it as? ObjCClass)?.let { existingClass -> NSStringFromClass(existingClass) == name } == true
    }
    // Prepended, not appended: URLSession asks each class in order whether it can handle a
    // request, and the first to say yes wins. Appending would let the built-in HTTP protocol
    // claim every request first and we would never see one.
    if (!alreadyInstalled) {
        configuration.protocolClasses = listOf(cls) + existing
    }
    return true
}

/**
 * A default URLSession configuration with Alohomora tracing installed.
 *
 * Use the resulting session **instead of `URLSession.shared`**, which cannot be traced because its
 * configuration is immutable:
 * ```swift
 * let session = URLSession(configuration: Alohomora.shared.alohomoraURLSessionConfiguration())
 * ```
 *
 * Declared as an explicit no-argument overload rather than a default parameter: Kotlin default
 * arguments are not bridged as Swift defaults, so `= NSURLSessionConfiguration.default` would
 * force every Swift caller to pass one.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
fun Alohomora.alohomoraURLSessionConfiguration(): NSURLSessionConfiguration =
    alohomoraURLSessionConfiguration(NSURLSessionConfiguration.defaultSessionConfiguration)

/**
 * Installs Alohomora tracing into [base] and returns it.
 *
 * Use this overload when you configure timeouts, default headers or caching policy yourself.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
fun Alohomora.alohomoraURLSessionConfiguration(
    base: NSURLSessionConfiguration,
): NSURLSessionConfiguration = base.also { installURLProtocol(it) }

/** Unregister the global [AlohomoraURLProtocol] interceptor. */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Suppress("UnusedReceiverParameter", "unused")
fun Alohomora.unregisterURLProtocol() {
    NSURLProtocol.unregisterClass(alohomoraProtocolClass)
}
