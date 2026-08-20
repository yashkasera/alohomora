package io.github.yashkasera.alohomora.devtools

import io.github.yashkasera.alohomora.common.BuildMetadataPayload
import io.github.yashkasera.alohomora.common.GitHistoryPayload
import io.github.yashkasera.alohomora.data.model.AlohomoraConfig
import io.github.yashkasera.alohomora.data.model.GitHistoryCommit

internal fun AlohomoraConfig.toBuildMetadataPayload(): BuildMetadataPayload = BuildMetadataPayload(
    appName = appName,
    projectName = projectName,
    packageName = packageName,
    versionName = versionName,
    versionCode = versionCode,
    variantName = variantName,
    flavorName = flavorName,
    buildType = buildType,
    branch = branch,
    commitSha = commitSha,
    isDirty = isDirty,
    buildTimestampUtc = buildTimestampUtc,
    slackWebhookUrl = slackWebhookUrl,
)

internal fun GitHistoryCommit.toGitHistoryPayload(): GitHistoryPayload = GitHistoryPayload(
    sha = sha,
    author = author,
    message = message,
    timestamp = timestamp,
)
