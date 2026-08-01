package io.github.yashkasera.alohomora.data.model

interface AlohomoraConfig {
    val projectName: String
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
}
