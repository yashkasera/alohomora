package io.github.yashkasera.alohomora.desktop.data.local

import io.github.yashkasera.alohomora.common.TrafficEntry
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrafficExporterTest {

    private val sampleEntry = TrafficEntry(
        id = "e1",
        status = 200,
        url = "https://api.example.com/users?page=1&limit=10",
        method = "GET",
        scheme = "https",
        host = "api.example.com",
        path = "/users",
        query = "page=1&limit=10",
        requestBody = null,
        responseBody = """{"users": []}""",
        time = 1724500000000L,
        duration = 150,
        requestHeaders = mapOf("Accept" to listOf("application/json")),
        requestContentType = null,
        responseContentType = "application/json",
        responseHeaders = mapOf("Content-Type" to listOf("application/json; charset=utf-8")),
        requestSize = 0,
        responseSize = 14,
    )

    private val postEntry = TrafficEntry(
        id = "e2",
        status = 201,
        url = "https://api.example.com/users",
        method = "POST",
        scheme = "https",
        host = "api.example.com",
        path = "/users",
        query = null,
        requestBody = """{"name": "Alice"}""",
        responseBody = """{"id": "abc"}""",
        time = 1724500001000L,
        duration = 250,
        requestHeaders = mapOf("Content-Type" to listOf("application/json")),
        requestContentType = "application/json",
        responseContentType = "application/json",
        responseHeaders = mapOf("Content-Type" to listOf("application/json")),
        requestSize = 17,
        responseSize = 13,
    )

    private val entries = listOf(sampleEntry, postEntry)

    // ── JSON ────────────────────────────────────────────────────────────────────

    @Test
    fun jsonRoundTrip() {
        val json = entries.toExportString(TrafficExportFormat.JSON, "1.0.0")
        val decoded = exportJson.decodeFromString(TrafficExportEnvelope.serializer(), json)
        assertEquals(1, decoded.version)
        assertEquals(2, decoded.entryCount)
        assertEquals(2, decoded.entries.size)
        assertEquals("e1", decoded.entries[0].id)
        assertEquals("e2", decoded.entries[1].id)
        assertEquals(200, decoded.entries[0].status)
        assertEquals("POST", decoded.entries[1].method)
    }

    @Test
    fun jsonEmptyList() {
        val json = emptyList<TrafficEntry>().toExportString(TrafficExportFormat.JSON, "1.0.0")
        val decoded = exportJson.decodeFromString(TrafficExportEnvelope.serializer(), json)
        assertEquals(0, decoded.entryCount)
        assertTrue(decoded.entries.isEmpty())
    }

    @Test
    fun jsonPreservesNullFields() {
        val minimal = TrafficEntry(id = "m1")
        val json = listOf(minimal).toExportString(TrafficExportFormat.JSON, "1.0.0")
        val decoded = exportJson.decodeFromString(TrafficExportEnvelope.serializer(), json)
        assertEquals(null, decoded.entries[0].status)
        assertEquals(null, decoded.entries[0].url)
        assertEquals(null, decoded.entries[0].method)
    }

    @Test
    fun jsonExcludesInternalFields() {
        val entry = sampleEntry.copy(
            isViewed = true,
            replayOf = "original-id",
            mockedBy = "rule-1",
            requestBodyTruncated = true,
            responseBodyTruncated = true,
        )
        val json = listOf(entry).toExportString(TrafficExportFormat.JSON, "1.0.0")
        assertFalse(json.contains("isViewed"))
        assertFalse(json.contains("replayOf"))
        assertFalse(json.contains("mockedBy"))
        assertFalse(json.contains("requestBodyTruncated"))
        assertFalse(json.contains("responseBodyTruncated"))
        assertTrue(json.contains("e1"))
        assertTrue(json.contains("api.example.com"))
    }

    // ── HAR ─────────────────────────────────────────────────────────────────────

    private val lenientJson = Json { ignoreUnknownKeys = true }

    @Test
    fun harProducesValidStructure() {
        val har = entries.toExportString(TrafficExportFormat.HAR, "2.0.0")
        val root = lenientJson.decodeFromString(HarExportRoot.serializer(), har)
        assertEquals("1.2", root.log.version)
        assertEquals("Alohomora", root.log.creator.name)
        assertEquals("2.0.0", root.log.creator.version)
        assertEquals(2, root.log.entries.size)
    }

    @Test
    fun harEntryMapsRequestFields() {
        val harEntry = sampleEntry.toHarEntry()
        assertEquals("GET", harEntry.request.method)
        assertEquals("https://api.example.com/users?page=1&limit=10", harEntry.request.url)
        assertEquals(150, harEntry.time)
        assertTrue(harEntry.startedDateTime.contains("2024-08-24"))
    }

    @Test
    fun harEntryMapsQueryString() {
        val harEntry = sampleEntry.toHarEntry()
        assertEquals(2, harEntry.request.queryString.size)
        assertEquals("page", harEntry.request.queryString[0].name)
        assertEquals("1", harEntry.request.queryString[0].v)
        assertEquals("limit", harEntry.request.queryString[1].name)
        assertEquals("10", harEntry.request.queryString[1].v)
    }

    @Test
    fun harEntryMapsHeaders() {
        val harEntry = sampleEntry.toHarEntry()
        assertEquals(1, harEntry.request.headers.size)
        assertEquals("Accept", harEntry.request.headers[0].name)
        assertEquals("application/json", harEntry.request.headers[0].v)
    }

    @Test
    fun harEntryMapsPostData() {
        val harEntry = postEntry.toHarEntry()
        assertEquals("application/json", harEntry.request.postData?.mimeType)
        assertEquals("""{"name": "Alice"}""", harEntry.request.postData?.text)
    }

    @Test
    fun harEntryMapsResponse() {
        val harEntry = sampleEntry.toHarEntry()
        assertEquals(200, harEntry.response.status)
        assertEquals("OK", harEntry.response.statusText)
        assertEquals("""{"users": []}""", harEntry.response.content.text)
        assertEquals("application/json", harEntry.response.content.mimeType)
    }

    @Test
    fun harHandlesNullFields() {
        val minimal = TrafficEntry(id = "m1")
        val harEntry = minimal.toHarEntry()
        assertEquals("GET", harEntry.request.method)
        assertEquals("", harEntry.request.url)
        assertEquals(0, harEntry.response.status)
        assertEquals(0, harEntry.time)
        assertTrue(harEntry.request.queryString.isEmpty())
        assertEquals(null, harEntry.request.postData)
    }

    @Test
    fun harEmptyList() {
        val har = emptyList<TrafficEntry>().toExportString(TrafficExportFormat.HAR, "1.0.0")
        val root = lenientJson.decodeFromString(HarExportRoot.serializer(), har)
        assertTrue(root.log.entries.isEmpty())
    }

    // ── cURL ────────────────────────────────────────────────────────────────────

    @Test
    fun curlContainsShebang() {
        val curl = entries.toExportString(TrafficExportFormat.CURL, "1.0.0")
        assertTrue(curl.startsWith("#!/usr/bin/env bash"))
    }

    @Test
    fun curlContainsEntryCount() {
        val curl = entries.toExportString(TrafficExportFormat.CURL, "1.0.0")
        assertTrue(curl.contains("# Entries: 2"))
    }

    @Test
    fun curlMatchesCurlCommand() {
        val curl = entries.toExportString(TrafficExportFormat.CURL, "1.0.0")
        assertTrue(curl.contains(sampleEntry.curlCommand()))
        assertTrue(curl.contains(postEntry.curlCommand()))
    }

    @Test
    fun curlEmptyList() {
        val curl = emptyList<TrafficEntry>().toExportString(TrafficExportFormat.CURL, "1.0.0")
        assertTrue(curl.contains("# Entries: 0"))
        assertFalse(curl.contains("curl "))
    }
}
