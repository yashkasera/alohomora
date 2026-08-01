package io.github.yashkasera.alohomora.trace

import io.github.yashkasera.alohomora.Alohomora
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCClass
import platform.Foundation.NSStringFromClass
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSession

/**
 * Regression tests for iOS trace capture producing nothing.
 *
 * The library shipped only `registerURLProtocol()`, which registers with the legacy
 * `NSURLConnection` loading system. **URLSession ignores globally registered protocol classes**,
 * and `URLSession.shared` can never be intercepted because its configuration is immutable — so
 * every URLSession-based app got a permanently empty Traces screen while telemetry worked fine,
 * which made it look like a transport problem rather than a capture problem.
 *
 * These tests assert the only thing that actually makes interception happen: the protocol class
 * being present, and first, in a session configuration.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class AlohomoraURLProtocolInstallTest {

    /**
     * The Objective-C class object, taken from the companion exactly as production does.
     *
     * Deliberately *not* `NSClassFromString("AlohomoraKitAlohomoraURLProtocol")`: that name only
     * resolves inside the built framework, so it is null in a test binary — and it silently
     * becomes null in production too if the framework `baseName` ever changes.
     */
    private val protocolClass: ObjCClass get() = AlohomoraURLProtocol

    /** ObjC class name of our interceptor. */
    private val protocolClassName: String get() = NSStringFromClass(protocolClass)

    /**
     * Names of the classes installed in [this].
     *
     * Compared by name rather than identity because Kotlin/Native hands back a fresh
     * `ObjCClass` wrapper on every access, so `===` between two references to the same ObjC
     * class is false. An identity-based check here silently passed while production
     * re-prepended the class on every install.
     */
    private fun NSURLSessionConfiguration.installedClassNames(): List<String> =
        protocolClasses.orEmpty().mapNotNull { (it as? ObjCClass)?.let(::NSStringFromClass) }

    @Test
    fun `installing adds the protocol class to a configuration`() {
        val config = NSURLSessionConfiguration.defaultSessionConfiguration
        assertTrue(
            protocolClassName !in config.installedClassNames(),
            "a fresh default configuration must not already contain the interceptor",
        )

        assertTrue(Alohomora.installURLProtocol(config))

        assertTrue(
            protocolClassName in config.installedClassNames(),
            "interceptor missing after install",
        )
    }

    @Test
    fun `the protocol class is installed first`() {
        val config = NSURLSessionConfiguration.defaultSessionConfiguration
        Alohomora.installURLProtocol(config)

        // URLSession asks each class in order whether it can handle a request and the first to
        // accept wins. Appended instead of prepended, the built-in HTTP protocol claims every
        // request and the interceptor never runs — capture would silently do nothing.
        assertEquals(
            protocolClassName,
            config.installedClassNames().first(),
            "interceptor must be first or the built-in HTTP protocol wins",
        )
    }

    @Test
    fun `installing twice does not duplicate the class`() {
        val config = NSURLSessionConfiguration.defaultSessionConfiguration
        Alohomora.installURLProtocol(config)
        val afterFirst = config.installedClassNames()
        Alohomora.installURLProtocol(config)
        Alohomora.installURLProtocol(config)

        assertEquals(
            afterFirst,
            config.installedClassNames(),
            "repeat installs must be idempotent, or protocolClasses grows without bound",
        )
    }

    @Test
    fun `the convenience configuration is ready to trace`() {
        val config = Alohomora.alohomoraURLSessionConfiguration()

        assertEquals(protocolClassName, config.installedClassNames().first())
        // And it must survive being handed to a real session. URLSession copies the
        // configuration, so anything mutated after construction would be lost.
        val session = NSURLSession.sessionWithConfiguration(config)
        assertTrue(
            protocolClassName in session.configuration.installedClassNames(),
            "the session's copied configuration lost the interceptor",
        )
    }

    @Test
    fun `a caller-supplied base configuration keeps its own settings`() {
        val base = NSURLSessionConfiguration.defaultSessionConfiguration
        base.timeoutIntervalForRequest = 42.0

        val configured = Alohomora.alohomoraURLSessionConfiguration(base)

        assertEquals(42.0, configured.timeoutIntervalForRequest)
        assertEquals(protocolClassName, configured.installedClassNames().first())
    }
}
