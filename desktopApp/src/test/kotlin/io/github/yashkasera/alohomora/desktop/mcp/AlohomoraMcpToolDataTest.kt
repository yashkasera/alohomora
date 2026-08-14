package io.github.yashkasera.alohomora.desktop.mcp

import io.github.yashkasera.alohomora.common.Error
import io.github.yashkasera.alohomora.common.Span
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.desktop.FakeDevToolsRepository
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The tool projections are the read surface an agent sees, so they are tested directly over a fake
 * repository — filtering, truncation flagging, the out-of-order span flags, and the round-trip
 * timeout — rather than through the transport, which the server test covers.
 */
class AlohomoraMcpToolDataTest {

    @Test
    fun `list_traffic failedOnly keeps only non-2xx`() {
        val repo = FakeDevToolsRepository().apply {
            traffic.value = listOf(
                entry("1", status = 200, method = "GET", host = "api.example.com"),
                entry("2", status = 500, method = "POST", host = "api.example.com"),
                entry("3", status = 404, method = "GET", host = "cdn.example.com"),
            )
        }

        val all = AlohomoraMcpToolData.listTraffic(repo, null, null, null, failedOnly = false, limit = 100)
        assertEquals(3, all.jsonArray.size)

        val failed = AlohomoraMcpToolData.listTraffic(repo, null, null, null, failedOnly = true, limit = 100)
        val statuses = failed.jsonArray.map { it.jsonObject["status"]!!.jsonPrimitive.int }.toSet()
        assertEquals(setOf(500, 404), statuses)
    }

    @Test
    fun `list_traffic filters by status method host and limit`() {
        val repo = FakeDevToolsRepository().apply {
            traffic.value = listOf(
                entry("1", status = 200, method = "GET", host = "api.example.com"),
                entry("2", status = 200, method = "POST", host = "api.example.com"),
                entry("3", status = 200, method = "GET", host = "cdn.example.com"),
            )
        }

        val byStatus = AlohomoraMcpToolData.listTraffic(repo, status = 200, method = null, host = null, failedOnly = false, limit = 100)
        assertEquals(3, byStatus.jsonArray.size)

        val byMethod = AlohomoraMcpToolData.listTraffic(repo, status = null, method = "post", host = null, failedOnly = false, limit = 100)
        assertEquals(1, byMethod.jsonArray.size)

        val byHost = AlohomoraMcpToolData.listTraffic(repo, status = null, method = null, host = "cdn", failedOnly = false, limit = 100)
        assertEquals(1, byHost.jsonArray.size)

        val limited = AlohomoraMcpToolData.listTraffic(repo, status = null, method = null, host = null, failedOnly = false, limit = 2)
        assertEquals(2, limited.jsonArray.size)
    }

    @Test
    fun `get_traffic surfaces truncation flags and curl`() {
        val repo = FakeDevToolsRepository().apply {
            traffic.value = listOf(
                entry("1", status = 200, method = "GET", host = "api.example.com").apply {
                    url = "https://api.example.com/things"
                    requestBodyTruncated = true
                    responseBodyTruncated = false
                },
            )
        }

        val detail = AlohomoraMcpToolData.getTraffic(repo, "1")!!.jsonObject
        assertTrue(detail["requestBodyTruncated"]!!.jsonPrimitive.boolean)
        assertFalse(detail["responseBodyTruncated"]!!.jsonPrimitive.boolean)
        assertTrue(detail["curl"]!!.jsonPrimitive.content.startsWith("curl "))
        assertNull(AlohomoraMcpToolData.getTraffic(repo, "missing"))
    }

    @Test
    fun `get_trace flags orphan and skew from out-of-order spans`() {
        // Deliberately out of order (child, skewed, orphan, then root) — buildTraceTree assembles it.
        val repo = FakeDevToolsRepository().apply {
            spans.value = listOf(
                span("bbbb", parent = "aaaa", name = "child", start = 2000, end = 3000),
                span("eeee", parent = "aaaa", name = "skewed", start = 4000, end = 3500),
                span("cccc", parent = "dddd", name = "orphan", start = 1500, end = 1600),
                span("aaaa", parent = null, name = "root", start = 1000, end = 5000),
            )
        }

        val trace = AlohomoraMcpToolData.getTrace(repo, "trace1")!!.jsonObject
        assertTrue(trace["isComplete"]!!.jsonPrimitive.boolean, "root present so trace is complete")
        assertEquals(4, trace["spanCount"]!!.jsonPrimitive.int)

        val byName = trace["spans"]!!.jsonArray.associateBy { it.jsonObject["name"]!!.jsonPrimitive.content }
        assertTrue(byName.getValue("orphan").jsonObject["isOrphan"]!!.jsonPrimitive.boolean)
        assertFalse(byName.getValue("child").jsonObject["isOrphan"]!!.jsonPrimitive.boolean)
        assertTrue(byName.getValue("skewed").jsonObject["hasSkew"]!!.jsonPrimitive.boolean)
        assertFalse(byName.getValue("root").jsonObject["hasSkew"]!!.jsonPrimitive.boolean)

        assertNull(AlohomoraMcpToolData.getTrace(repo, "no-such-trace"))
    }

    @Test
    fun `list_errors titles come from exceptionTypeName`() {
        val repo = FakeDevToolsRepository().apply {
            errors.value = listOf(
                Error(id = 1, place = "checkout", reason = "java.lang.IllegalStateException: boom"),
            )
        }
        val row = AlohomoraMcpToolData.listErrors(repo, 100).jsonArray.single().jsonObject
        assertEquals("IllegalStateException", row["title"]!!.jsonPrimitive.content)
    }

    @Test
    fun `get_build_metadata never leaks the slack webhook`() {
        val repo = FakeDevToolsRepository().apply {
            buildInfo.value = io.github.yashkasera.alohomora.desktop.domain.model.BuildInfo(
                projectName = "Demo",
                packageName = "com.demo",
                versionName = "1.2.3",
                versionCode = 4,
                variantName = "debug",
                branch = "main",
                commitSha = "abc123",
                isDirty = false,
                buildTimestampUtc = 0L,
                slackWebhookUrl = "https://hooks.slack.com/services/SECRET",
            )
        }
        val meta = AlohomoraMcpToolData.buildMetadata(repo)!!.jsonObject
        assertEquals("Demo", meta["projectName"]!!.jsonPrimitive.content)
        assertFalse("slackWebhookUrl" in meta.keys, "the webhook must never be serialized")
    }

    @Test
    fun `get_cache_value returns a delivered value`() = runTest {
        // Pre-seeded, so the awaited flow already holds the key and the tool returns immediately.
        val repo = FakeDevToolsRepository().apply { deliverCacheValue("token", "xyz") }
        val value = AlohomoraMcpToolData.getCacheValue(repo, "token")!!.jsonObject
        assertEquals("token", value["key"]!!.jsonPrimitive.content)
        assertEquals("xyz", value["value"]!!.jsonPrimitive.content)
    }

    @Test
    fun `get_cache_value times out cleanly when nothing arrives`() = runTest {
        // The fake never delivers, so withTimeoutOrNull expires — under runTest the virtual clock
        // advances instantly, so this asserts the null path without a real 5s wait.
        val repo = FakeDevToolsRepository()
        assertNull(AlohomoraMcpToolData.getCacheValue(repo, "never"))
    }

    private fun entry(id: String, status: Int, method: String, host: String) = TrafficEntry(
        id = id,
        status = status,
        method = method,
        host = host,
        path = "/things",
        time = id.toLong(),
    )

    private fun span(id: String, parent: String?, name: String, start: Long, end: Long) = Span(
        traceId = "trace1",
        spanId = id,
        parentSpanId = parent,
        name = name,
        startEpochNanos = start,
        endEpochNanos = end,
    )
}
