package io.github.yashkasera.alohomora.desktop.domain.model

data class BuildInfo(
    val projectName: String,
    val packageName: String? = null,
    val versionName: String,
    val versionCode: Int,
    val variantName: String,
    val flavorName: String? = null,
    val buildType: String? = null,
    val branch: String,
    val commitSha: String,
    val isDirty: Boolean,
    val buildTimestampUtc: Long,
    val slackWebhookUrl: String? = null,
)
