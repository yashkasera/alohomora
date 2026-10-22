package io.github.yashkasera.alohomora.data.model

import kotlin.experimental.ExperimentalNativeApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSBundle
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile

/**
 * Never returns null: even with no manifest, `Bundle.main` still knows the version and bundle id, so
 * the Config panel shows something rather than the `"unknown"` placeholders it renders today.
 */
@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
internal actual fun discoverPlatformBuildConfig(): AlohomoraConfig? {
    val bundle = NSBundle.mainBundle
    val identity = AppIdentity(
        // CFBundleName is absent when a target sets no product name; the executable name is the
        // closest stand-in and is always present in a built app.
        appName = bundle.infoString("CFBundleName") ?: bundle.infoString("CFBundleExecutable"),
        packageName = bundle.bundleIdentifier,
        versionName = bundle.infoString("CFBundleShortVersionString"),
        // CFBundleVersion is a *string* and may be dotted ("1.2.3"), which has no Int equivalent.
        // Falling back to null is honest; inventing a number would be a lie the Config panel repeats.
        versionCode = bundle.infoString("CFBundleVersion")?.toIntOrNull(),
    )
    return identity.toAlohomoraConfig(
        readBundledBuildInfo(bundle),
        // The consumer's per-configuration switch (Drogon sets it YES in Beta). Absent => fall back
        // to the debug binary so a plain debug build keeps DevTools without touching its Info.plist.
        devToolsEnabled = bundle.infoBool("AlohomoraDevToolsEnabled") ?: Platform.isDebugBinary,
    )
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

/**
 * Reads a boolean Info.plist entry. A real `<true/>`/`<false/>` bridges to `NSNumber`; a key
 * injected as a build setting (`INFOPLIST_KEY_…`) can arrive as the string "YES"/"NO", so accept
 * both. Null when the key is absent, which the caller reads as "unspecified".
 */
@OptIn(ExperimentalForeignApi::class)
private fun NSBundle.infoBool(key: String): Boolean? =
    when (val value = objectForInfoDictionaryKey(key)) {
        is NSNumber -> value.boolValue
        is String -> value.equals("YES", ignoreCase = true) || value.equals("true", ignoreCase = true)
        else -> null
    }
