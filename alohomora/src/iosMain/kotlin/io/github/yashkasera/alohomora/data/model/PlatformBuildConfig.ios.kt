package io.github.yashkasera.alohomora.data.model

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSBundle
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile

/**
 * Never returns null: even with no manifest, `Bundle.main` still knows the version and bundle id, so
 * the Config panel shows something rather than the `"unknown"` placeholders it renders today.
 */
@OptIn(ExperimentalForeignApi::class)
internal actual fun discoverPlatformBuildConfig(): AlohomoraConfig? {
    val bundle = NSBundle.mainBundle
    val identity = AppIdentity(
        // CFBundleName is absent when a target sets no product name; the executable name is the
        // closest stand-in and is always present in a built app.
        projectName = bundle.infoString("CFBundleName") ?: bundle.infoString("CFBundleExecutable"),
        packageName = bundle.bundleIdentifier,
        versionName = bundle.infoString("CFBundleShortVersionString"),
        // CFBundleVersion is a *string* and may be dotted ("1.2.3"), which has no Int equivalent.
        // Falling back to null is honest; inventing a number would be a lie the Config panel repeats.
        versionCode = bundle.infoString("CFBundleVersion")?.toIntOrNull(),
    )
    return identity.toAlohomoraConfig(readBundledBuildInfo(bundle))
}

@OptIn(ExperimentalForeignApi::class)
private fun readBundledBuildInfo(bundle: NSBundle): BundledBuildInfo? {
    val path = bundle.pathForResource(
        BundledBuildInfo.RESOURCE_NAME,
        BundledBuildInfo.RESOURCE_EXTENSION,
    ) ?: return null
    val raw = NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null) ?: return null
    return BundledBuildInfo.parse(raw)
}

/**
 * Reads the *effective* Info.plist, which is what the build settings actually resolved to — parsing
 * the source plist would miss every `GENERATE_INFOPLIST_FILE = YES` key.
 */
private fun NSBundle.infoString(key: String): String? =
    (objectForInfoDictionaryKey(key) as? String)?.takeIf { it.isNotBlank() }
