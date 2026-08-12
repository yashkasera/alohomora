package io.github.yashkasera.alohomora.ui.components.jsonviewer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

class JsonTreeBuilderTest {

    private fun buildFrom(json: String) =
        JsonTreeBuilder.build(Json.parseToJsonElement(json))

    @Test
    fun `search finds key by substring`() {
        val tree = buildFrom("""{"username": "alice"}""")
        val results = tree.searchIndex.search("name")
        assertTrue(results.isNotEmpty())
    }

    @Test
    fun `search finds value by substring`() {
        val tree = buildFrom("""{"body": "voluptatem ipsum"}""")
        val results = tree.searchIndex.search("ate")
        assertTrue(results.isNotEmpty())
    }

    @Test
    fun `search finds nested value`() {
        val tree = buildFrom("""{"user": {"email": "alice@example.com"}}""")
        val results = tree.searchIndex.search("example")
        assertTrue(results.isNotEmpty())
    }

    @Test
    fun `search finds array element value`() {
        val tree = buildFrom("""{"tags": ["important", "urgent"]}""")
        val results = tree.searchIndex.search("port")
        assertEquals(1, results.size)
    }

    @Test
    fun `search finds numeric value`() {
        val tree = buildFrom("""{"count": 42}""")
        val results = tree.searchIndex.search("42")
        assertTrue(results.isNotEmpty())
    }

    @Test
    fun `search returns empty for no match`() {
        val tree = buildFrom("""{"name": "alice"}""")
        assertTrue(tree.searchIndex.search("zzz").isEmpty())
    }

    @Test
    fun `search is case insensitive via lowercased tokens`() {
        val tree = buildFrom("""{"Name": "Alice"}""")
        val results = tree.searchIndex.search("alice")
        assertTrue(results.isNotEmpty())
    }

    @Test
    fun `tree nodes are built for objects and arrays`() {
        val tree = buildFrom("""{"items": [{"id": 1}, {"id": 2}]}""")
        assertTrue(tree.nodes["$"] is JsonObjectNode)
        assertTrue(tree.nodes["$.items"] is JsonArrayNode)
        assertTrue(tree.nodes["$.items[0]"] is JsonObjectNode)
        assertTrue(tree.nodes["$.items[0].id"] is JsonValueNode)
    }
}
