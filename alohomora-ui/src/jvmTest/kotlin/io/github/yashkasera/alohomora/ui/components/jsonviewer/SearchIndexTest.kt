package io.github.yashkasera.alohomora.ui.components.jsonviewer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchIndexTest {

    @Test
    fun `prefix match returns the path`() {
        val index = SearchIndex()
        index.insert("username", "$.username")
        assertEquals(listOf("$.username"), index.search("user"))
    }

    @Test
    fun `substring match returns the path`() {
        val index = SearchIndex()
        index.insert("voluptatem", "$.body")
        assertEquals(listOf("$.body"), index.search("ate"))
    }

    @Test
    fun `exact match returns the path`() {
        val index = SearchIndex()
        index.insert("hello", "$.greeting")
        assertEquals(listOf("$.greeting"), index.search("hello"))
    }

    @Test
    fun `search is case sensitive on pre-lowered tokens`() {
        val index = SearchIndex()
        index.insert("hello", "$.a")
        assertTrue(index.search("HELLO").isEmpty())
    }

    @Test
    fun `no match returns empty list`() {
        val index = SearchIndex()
        index.insert("alpha", "$.a")
        index.insert("beta", "$.b")
        assertTrue(index.search("gamma").isEmpty())
    }

    @Test
    fun `blank query returns empty list`() {
        val index = SearchIndex()
        index.insert("hello", "$.a")
        assertTrue(index.search("").isEmpty())
        assertTrue(index.search("   ").isEmpty())
    }

    @Test
    fun `multiple tokens matching returns all paths`() {
        val index = SearchIndex()
        index.insert("created_at", "$.created_at")
        index.insert("updated_at", "$.updated_at")
        index.insert("name", "$.name")
        val results = index.search("at")
        assertEquals(2, results.size)
        assertTrue("$.created_at" in results)
        assertTrue("$.updated_at" in results)
    }

    @Test
    fun `same path inserted with different tokens matches both`() {
        val index = SearchIndex()
        index.insert("title", "$.posts[0]")
        index.insert("hello world", "$.posts[0]")
        assertEquals(listOf("$.posts[0]"), index.search("title"))
        assertEquals(listOf("$.posts[0]"), index.search("world"))
    }
}
