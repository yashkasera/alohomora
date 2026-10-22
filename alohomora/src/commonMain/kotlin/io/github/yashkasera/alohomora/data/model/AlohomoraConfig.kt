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

    // The one gate on the DevTools server. The consumer's build config decides it — the Gradle
    // plugin stamps it true for enabled variants, iOS reads it from Info.plist — so enabling a
    // non-debuggable variant (e.g. beta) deliberately runs the server there. Defaults off so a
    // config that predates this, or a release with no generated config, stays closed.
    val devToolsEnabled: Boolean get() = false
}
