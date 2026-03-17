package io.github.yashkasera.alohomora.desktop.data.devtools

import io.github.yashkasera.alohomora.common.AppDatabaseInfo
import io.github.yashkasera.alohomora.common.BuildInfoPayload
import io.github.yashkasera.alohomora.common.ChronicleCommitPayload
import io.github.yashkasera.alohomora.common.DatabaseSchemaSnapshot
import io.github.yashkasera.alohomora.common.DatabaseTableColumnPayload
import io.github.yashkasera.alohomora.common.DatabaseTableSchemaPayload
import io.github.yashkasera.alohomora.common.DatabaseTableSnapshot
import io.github.yashkasera.alohomora.desktop.domain.model.BuildInfo
import io.github.yashkasera.alohomora.desktop.domain.model.ChronicleCommit
import io.github.yashkasera.alohomora.desktop.domain.model.DatabaseInfo
import io.github.yashkasera.alohomora.desktop.domain.model.DatabaseSchema
import io.github.yashkasera.alohomora.desktop.domain.model.DatabaseTable
import io.github.yashkasera.alohomora.desktop.domain.model.DatabaseTableColumn
import io.github.yashkasera.alohomora.desktop.domain.model.DatabaseTableSchema

internal fun AppDatabaseInfo.toDomain(): DatabaseInfo = DatabaseInfo(
    name = name,
    path = path,
)

internal fun DatabaseTableColumnPayload.toDomain(): DatabaseTableColumn = DatabaseTableColumn(
    name = name,
    type = type,
    notNull = notNull,
    primaryKey = primaryKey,
    defaultValue = defaultValue,
)

internal fun DatabaseTableSchemaPayload.toDomain(): DatabaseTableSchema = DatabaseTableSchema(
    name = name,
    columns = columns.map { it.toDomain() },
    primaryKey = primaryKey,
    indexes = indexes,
)

internal fun DatabaseSchemaSnapshot.toDomain(): DatabaseSchema = DatabaseSchema(
    databaseName = databaseName,
    tables = tables,
    schemas = schemas.map { it.toDomain() },
)

internal fun DatabaseTableSnapshot.toDomain(): DatabaseTable = DatabaseTable(
    databaseName = databaseName,
    name = name,
    columns = columns,
    rows = rows,
)

internal fun BuildInfoPayload.toDomain(): BuildInfo = BuildInfo(
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

internal fun ChronicleCommitPayload.toDomain(): ChronicleCommit = ChronicleCommit(
    sha = sha,
    author = author,
    message = message,
    timestamp = timestamp,
)
