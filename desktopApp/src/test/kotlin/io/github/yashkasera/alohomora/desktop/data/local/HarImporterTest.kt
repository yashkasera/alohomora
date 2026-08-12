package io.github.yashkasera.alohomora.desktop.data.local

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HarImporterTest {

    @Test
    fun validHarProducesRules() {
        val har = """
        {
          "log": {
            "entries": [
              {
                "request": { "method": "GET", "url": "https://api.example.com/users?page=1" },
                "response": {
                  "status": 200,
                  "content": { "text": "{\"users\":[]}", "mimeType": "application/json" }
                }
              },
              {
                "request": { "method": "POST", "url": "https://api.example.com/orders" },
                "response": {
                  "status": 201,
                  "content": { "text": "{\"id\":1}", "mimeType": "application/json; charset=utf-8" }
                }
              }
            ]
          }
        }
        """.trimIndent()

        val rules = importHar(har)
        assertEquals(2, rules.size)

        assertEquals("/users", rules[0].urlPattern)
        assertEquals("GET", rules[0].method)
        assertEquals(200, rules[0].statusCode)
        assertEquals("{\"users\":[]}", rules[0].responseBody)
        assertEquals("application/json", rules[0].contentType)

        assertEquals("/orders", rules[1].urlPattern)
        assertEquals("POST", rules[1].method)
        assertEquals(201, rules[1].statusCode)
        assertEquals("application/json", rules[1].contentType)
    }

    @Test
    fun nonSuccessStatusCodesAreSkipped() {
        val har = """
        {
          "log": {
            "entries": [
              {
                "request": { "method": "GET", "url": "https://example.com/fail" },
                "response": { "status": 500, "content": { "text": "error" } }
              },
              {
                "request": { "method": "GET", "url": "https://example.com/ok" },
                "response": { "status": 200, "content": { "text": "ok" } }
              }
            ]
          }
        }
        """.trimIndent()

        val rules = importHar(har)
        assertEquals(1, rules.size)
        assertEquals("/ok", rules[0].urlPattern)
    }

    @Test
    fun entriesWithoutBodyAreSkipped() {
        val har = """
        {
          "log": {
            "entries": [
              {
                "request": { "method": "GET", "url": "https://example.com/empty" },
                "response": { "status": 200, "content": {} }
              }
            ]
          }
        }
        """.trimIndent()

        val rules = importHar(har)
        assertTrue(rules.isEmpty())
    }

    @Test
    fun emptyHarProducesEmptyList() {
        val har = """{"log": {"entries": []}}"""
        assertTrue(importHar(har).isEmpty())
    }

    @Test
    fun urlWithoutPathDefaultsToSlash() {
        val har = """
        {
          "log": {
            "entries": [
              {
                "request": { "method": "GET", "url": "https://example.com" },
                "response": { "status": 200, "content": { "text": "root" } }
              }
            ]
          }
        }
        """.trimIndent()

        val rules = importHar(har)
        assertEquals(1, rules.size)
        assertEquals("/", rules[0].urlPattern)
    }

    @Test
    fun allRulesHaveBlankId() {
        val har = """
        {
          "log": {
            "entries": [
              {
                "request": { "method": "GET", "url": "https://example.com/a" },
                "response": { "status": 200, "content": { "text": "x" } }
              }
            ]
          }
        }
        """.trimIndent()

        val rules = importHar(har)
        assertTrue(rules.all { it.id.isBlank() })
    }
}
