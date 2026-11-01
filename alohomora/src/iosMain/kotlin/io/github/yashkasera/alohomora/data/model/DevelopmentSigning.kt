package io.github.yashkasera.alohomora.data.model

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSBundle
import platform.Foundation.NSISOLatin1StringEncoding
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSString
import platform.Foundation.stringWithContentsOfFile

/**
 * Whether the *host app* is a development build — the iOS counterpart of Android's
 * `FLAG_DEBUGGABLE`.
 *
 * `Platform.isDebugBinary` is not this: it describes the Kotlin framework, and every SPM consumer
 * links the Release XCFramework, so it is false in their Debug builds too. The signing is the one
 * thing that tracks the app's own configuration:
 * - **Simulator** — always a development environment; App Store builds cannot run there.
 * - **Development-signed device build** — Xcode embeds a profile whose entitlements grant
 *   `get-task-allow`, the entitlement that lets a debugger attach.
 * - **TestFlight / ad-hoc / enterprise** — the embedded profile denies `get-task-allow`.
 * - **App Store** — Apple strips the profile entirely.
 *
 * Read once: the profile cannot change under a running process.
 */
internal val isDevelopmentSignedBuild: Boolean by lazy {
    isDevelopmentBuild(profile = readEmbeddedProfile(), isSimulator = isRunningOnSimulator())
}

/** Split from the I/O so the decision table is testable without a signed bundle. */
internal fun isDevelopmentBuild(profile: String?, isSimulator: Boolean): Boolean =
    isSimulator || (profile != null && ProvisioningProfile.allowsDebugging(profile))

internal object ProvisioningProfile {

    // `embedded.mobileprovision` is a CMS (PKCS#7) envelope around an XML plist. The plist is stored
    // as plain bytes inside it, so scanning for the key/value pair works without a CMS decoder —
    // which iOS does not expose as public API anyway. `\s*` because the plist is pretty-printed with
    // a newline and tabs between the key and its value.
    private val getTaskAllowTrue = Regex("""<key>get-task-allow</key>\s*<true\s*/>""")

    fun allowsDebugging(profile: String): Boolean = getTaskAllowTrue.containsMatchIn(profile)
}

/**
 * ISO Latin-1 rather than UTF-8 on purpose: the CMS wrapper is binary, so a UTF-8 decode of the
 * whole file fails and returns null, while Latin-1 maps every byte and leaves the ASCII plist intact.
 */
@OptIn(ExperimentalForeignApi::class)
private fun readEmbeddedProfile(): String? {
    val path = NSBundle.mainBundle.pathForResource("embedded", "mobileprovision") ?: return null
    return NSString.stringWithContentsOfFile(path, NSISOLatin1StringEncoding, null)
}

// The simulator runtime sets this for every process it launches; a device never does.
private fun isRunningOnSimulator(): Boolean =
    NSProcessInfo.processInfo.environment["SIMULATOR_DEVICE_NAME"] != null
