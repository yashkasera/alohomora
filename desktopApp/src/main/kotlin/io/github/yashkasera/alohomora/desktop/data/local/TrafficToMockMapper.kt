package io.github.yashkasera.alohomora.desktop.data.local

import io.github.yashkasera.alohomora.common.MockRule
import io.github.yashkasera.alohomora.common.TrafficEntry

fun TrafficEntry.toMockRule(): MockRule = MockRule(
    id = "",
    enabled = true,
    urlPattern = path ?: url?.substringAfter("://")?.substringAfter('/')?.substringBefore('?')
        ?.let { "/$it" } ?: "/",
    isRegex = false,
    method = method,
    statusCode = status ?: 200,
    responseBody = responseBody ?: "",
    contentType = responseContentType?.substringBefore(';')?.trim() ?: "application/json",
)
