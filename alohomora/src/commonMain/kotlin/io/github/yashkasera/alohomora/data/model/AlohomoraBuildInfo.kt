package io.github.yashkasera.alohomora.data.model

data class AlohomoraBuildInfo(
    val branch: String,
    val commitSha: String,
    val isDirty: Boolean,
    val buildTimestampUtc: Long,
    val buildVariant: String,
    val versionName: String,
    val versionCode: Int,
)
