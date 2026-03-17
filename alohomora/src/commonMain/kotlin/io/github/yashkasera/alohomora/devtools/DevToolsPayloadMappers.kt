package io.github.yashkasera.alohomora.devtools

import io.github.yashkasera.alohomora.common.BuildInfoPayload
import io.github.yashkasera.alohomora.common.ChronicleCommitPayload
import io.github.yashkasera.alohomora.data.model.AlohomoraCommit
import io.github.yashkasera.alohomora.data.model.AlohomoraConfig

internal fun AlohomoraConfig.toBuildInfoPayload(): BuildInfoPayload = BuildInfoPayload(
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
)

internal fun AlohomoraCommit.toChronicleCommitPayload(): ChronicleCommitPayload = ChronicleCommitPayload(
    sha = sha,
    author = author,
    message = message,
    timestamp = timestamp,
)
