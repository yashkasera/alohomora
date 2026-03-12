package io.github.yashkasera.alohomora.data.model

import io.github.yashkasera.alohomora.common.AlohomoraConfig

data class BuildMetadata(
    val branch: String,
    val commitSha: String,
    val isDirty: Boolean,
    val buildTimestampUtc: Long,
    val variantName: String,
    val versionName: String,
    val versionCode: Int,
)

fun AlohomoraConfig?.toBuildMetadata(): BuildMetadata {
    return BuildMetadata(
        branch = this?.branch ?: "unknown",
        commitSha = this?.commitSha ?: "unknown",
        isDirty = this?.isDirty ?: false,
        buildTimestampUtc = this?.buildTimestampUtc ?: 0L,
        variantName = this?.variantName ?: "unknown",
        versionName = this?.versionName ?: "unknown",
        versionCode = this?.versionCode ?: -1,
    )
}
