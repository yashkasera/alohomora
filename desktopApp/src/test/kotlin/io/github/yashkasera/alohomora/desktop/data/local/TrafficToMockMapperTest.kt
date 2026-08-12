package io.github.yashkasera.alohomora.desktop.data.local

import io.github.yashkasera.alohomora.common.TrafficEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrafficToMockMapperTest {

    @Test
    fun fullFieldsMapping() {
        val traffic = TrafficEntry(
            id = "t1",
            status = 200,
            url = "https://api.example.com/users?page=1",
            method = "GET",
            path = "/users",
            responseBody = """{"users": []}""",
            responseContentType = "application/json; charset=utf-8",
        )
        val rule = traffic.toMockRule()
        assertEquals("/users", rule.urlPattern)
        assertEquals("GET", rule.method)
        assertEquals(200, rule.statusCode)
        assertEquals("""{"users": []}""", rule.responseBody)
        assertEquals("application/json", rule.contentType)
        assertTrue(rule.id.isBlank())
        assertTrue(rule.enabled)
        assertTrue(!rule.isRegex)
    }

    @Test
    fun nullFieldsFallback() {
        val traffic = TrafficEntry(
            id = "t2",
            status = null,
            url = null,
            method = null,
            path = null,
            responseBody = null,
            responseContentType = null,
        )
        val rule = traffic.toMockRule()
        assertEquals("/", rule.urlPattern)
        assertEquals(null, rule.method)
        assertEquals(200, rule.statusCode)
        assertEquals("", rule.responseBody)
        assertEquals("application/json", rule.contentType)
    }

    @Test
    fun templateLookingStringsPreservedLiterally() {
        val traffic = TrafficEntry(
            id = "t3",
            path = "/test",
            responseBody = """{"id": "{{uuid}}"}""",
        )
        val rule = traffic.toMockRule()
        assertTrue(rule.responseBody.contains("{{uuid}}"))
    }

    @Test
    fun pathExtractedFromUrlWhenPathIsNull() {
        val traffic = TrafficEntry(
            id = "t4",
            url = "https://api.example.com/orders/123?expand=items",
            path = null,
        )
        val rule = traffic.toMockRule()
        assertEquals("/orders/123", rule.urlPattern)
    }
}
