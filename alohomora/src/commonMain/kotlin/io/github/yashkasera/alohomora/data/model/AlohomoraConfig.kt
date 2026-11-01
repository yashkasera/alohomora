package io.github.yashkasera.alohomora.data.model

interface AlohomoraConfig {
    val appName: String? get() = null
    val packageName: String?
    val versionName: String
    val versionCode: Int
    val variantName: String
    val flavorName: String?
    val buildType: String?
    val branch: String
    val commitSha: String
    val isDirty: Boolean
    val buildTimestampUtc: Long
    val slackWebhookUrl: String?
    val commits: List<GitHistoryCommit>

    // An explicit opt-in to the DevTools server: the Gradle plugin stamps it true for enabled
    // variants, so enabling a non-debuggable variant (e.g. beta) deliberately runs the server there.
    // `false` — the default, and what every config from plugin 1.2.0 or earlier inherits — means "no
    // opinion", not "off": the host app's debuggability then decides, so a debug build works with no
    // plugin and a release build stays closed. Read it only through DevToolsActivation.
    val devToolsEnabled: Boolean get() = false
}
