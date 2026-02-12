package io.github.yashkasera.alohomora.desktop.data.devtools

import io.github.yashkasera.alohomora.desktop.domain.model.ApiLog
import io.github.yashkasera.alohomora.desktop.domain.model.DatabaseInfo
import io.github.yashkasera.alohomora.desktop.domain.model.DatabaseSchema
import io.github.yashkasera.alohomora.desktop.domain.model.DatabaseTable
import io.github.yashkasera.alohomora.desktop.domain.model.DatabaseTableColumn
import io.github.yashkasera.alohomora.desktop.domain.model.DatabaseTableSchema
import io.github.yashkasera.alohomora.desktop.domain.model.Event
import io.github.yashkasera.alohomora.devtools.ApiLogPayload
import io.github.yashkasera.alohomora.devtools.AppDatabaseInfo
import io.github.yashkasera.alohomora.devtools.DatabaseSchemaSnapshot
import io.github.yashkasera.alohomora.devtools.DatabaseTableColumnPayload
import io.github.yashkasera.alohomora.devtools.DatabaseTableSchemaPayload
import io.github.yashkasera.alohomora.devtools.DatabaseTableSnapshot
import io.github.yashkasera.alohomora.devtools.EventPayload

internal fun EventPayload.toDomain(): Event = Event(
    id = id,
    name = name,
    properties = properties,
    time = time,
)

internal fun ApiLogPayload.toDomain(): ApiLog = ApiLog(
    id = id,
    status = status,
    url = url,
    message = message,
    method = method,
    scheme = scheme,
    host = host,
    path = path,
    query = query,
    request = request,
    response = response,
    time = time,
    duration = duration,
    requestHeaders = requestHeaders,
    responseHeaders = responseHeaders,
    curl = curl,
    size = size,
    isViewed = isViewed,
)

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
